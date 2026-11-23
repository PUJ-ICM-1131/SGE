# SGE Notifier: avisos push de transacciones

Cuando alguien registra un ingreso o egreso, los **demás miembros de la finca** reciben una
notificación en el celular: *"Nuevo egreso: -$30.000 · Juan registró -$30.000 en Alimentación"*.
Al tocarla se abre el detalle de la transacción.

```
App (trabajador) ──guarda──▶ Firestore farms/{finca}/transactions/{id}
                                     │  listener en tiempo real
                                     ▼
                     sge-notifier (LXC de Proxmox, firebase-admin)
                     · busca los miembros de la finca (menos el autor)
                     · lee sus tokens en users/{uid}.fcmTokens
                                     │  FCM (HTTP v1)
                                     ▼
                     Celular del propietario → notificación "Movimientos"
```

**Por qué hace falta un servidor:** enviar por FCM requiere la cuenta de servicio de Firebase
(acceso de administrador), y esa clave nunca puede ir dentro del APK. Cloud Functions haría
lo mismo, pero exige el plan Blaze; aquí corre gratis en el mismo LXC de `collar-api`.

**No usa Funnel ni abre puertos:** todas sus conexiones son salientes (hacia Firestore y FCM).
Solo expone `/health` en `127.0.0.1:8081` para verificarlo dentro del contenedor.

## Comportamiento

- **Solo transacciones nuevas.** Al arrancar no relee el historial: las que tienen más de
  `MAX_AGE_MINUTES` (30) no se avisan.
- **Nunca avisa dos veces.** Antes de enviar crea `notificationLog/{finca}_{transacción}`; si ya
  existe (reinicio del servicio, evento repetido), no vuelve a avisar.
- **No avisa al autor.** Con `NOTIFY_AUTHOR=true` sí lo hace, útil para la demo con un solo celular.
- **Limpia tokens muertos.** Si FCM dice que un token ya no está registrado (app desinstalada,
  sesión cerrada), lo quita de `users/{uid}.fcmTokens`.
- **Se recupera solo.** Si se cae el listener (red, índice que falta), reintenta cada 30 s.
- El mensaje es de **solo datos**: la app arma la notificación con su propio canal y su ícono.

## Alertas de vacas fuera de los potreros (#73)

El mismo servicio vigila los potreros que se guardan desde la app (#71):

```
collar-api (127.0.0.1:8080) ──posiciones cada 15 s──▶ sge-notifier
farms/{finca}/paddocks      ──listener──────────────▶   · ¿la vaca está dentro de algún potrero?
                                                        · fuera en 2 lecturas seguidas
                                                        ▼
                     farms/{finca}/alerts/{id} (OPEN) + push al propietario
                     propietario toca "Reenviar" → forwardedAt → push a los trabajadores activos
                     la vaca vuelve a un potrero → la alerta pasa a RESOLVED (sin push)
```

- **Una sola alerta abierta por vaca**: mientras siga fuera no se repite el aviso.
- **Solo el servidor crea y resuelve alertas** (firebase-admin no pasa por las Security Rules);
  el propietario solo puede marcar el reenvío.
- **Necesita la clave de collar-api**: `install.sh` la copia de `/etc/sge-collar-api/env`. Sin
  `COLLAR_API_KEY` las alertas quedan apagadas y los avisos de transacciones siguen funcionando.
- **No necesita índices nuevos**: escucha `paddocks` y `alerts` sin filtros.
- Para la demo, dibuja el potrero cerca del borde del círculo (anclaje + radio) de un collar: la
  vaca sale y entra sola.

Variables (en `/etc/sge-notifier/env`): `COLLAR_API_URL` (por defecto `http://127.0.0.1:8080`),
`COLLAR_API_KEY` y `GEOFENCE_INTERVAL_SECONDS` (por defecto 15, mínimo 5).

## Configuración en Firebase (una sola vez)

### 1. Cuenta de servicio
Firebase Console → ⚙ **Configuración del proyecto** → **Cuentas de servicio** →
**Generar nueva clave privada**. Se descarga un `.json`.

> Ese archivo da acceso de administrador a todo Firebase. **Nunca lo subas a GitHub**
> (el `.gitignore` ya lo bloquea) ni lo compartas por chat. Si se filtra, revócalo en
> Google Cloud Console → IAM → Cuentas de servicio → Claves.

### 2. Índice de Firestore
El servicio consulta `transactions` de **todas** las fincas filtrando por `createdAt`.
Esa consulta necesita un índice de grupo de colecciones:

Firestore → **Índices** → **Campo único** → **Agregar exención**:

- Colección: `transactions`
- Campo: `createdAt`
- Alcance: **Grupo de colecciones**, orden **Ascendente**

Si se te olvida, el log del servicio muestra `FAILED_PRECONDITION` con un enlace que lo crea
directamente. El servicio reintenta solo cuando el índice esté listo (1–2 min).

## Desplegar en el LXC (el mismo de `collar-api`)

Desde tu PC, copia la clave al contenedor (por Tailscale):

```bash
scp icm-sge-firebase-adminsdk-xxxx.json root@sge-collar-api:/etc/sge-notifier/service-account.json
```

Si el directorio no existe aún, créalo antes con `ssh root@sge-collar-api mkdir -p /etc/sge-notifier`.

Dentro del contenedor:

```bash
cd ~/SGE && git pull
cd notifier
bash deploy/install.sh
```

`install.sh` hace lo siguiente:
- verifica la clave y la deja `600 root`;
- copia el código a `/opt/sge-notifier`;
- crea `/etc/sge-notifier/env`;
- deja corriendo el servicio `sge-notifier` con un usuario sin privilegios.

La clave le llega al servicio con `LoadCredential` de systemd, y solo él puede leerla.

### Operación

```bash
systemctl status sge-notifier
journalctl -u sge-notifier -f           # "f1/t1: enviado a 1/1 dispositivos"
curl -s http://127.0.0.1:8081/health    # listening, sent, failed, skipped
nano /etc/sge-notifier/env && systemctl restart sge-notifier   # p. ej. NOTIFY_AUTHOR=true
cd ~/SGE && git pull && cd notifier && bash deploy/install.sh  # actualizar
```

## Desarrollo local

```bash
npm install
npm test                                 # sin credenciales (Firebase simulado)
GOOGLE_APPLICATION_CREDENTIALS=./clave.json NOTIFY_AUTHOR=true npm start
```

## Estructura

```
src/
  index.js                 arranque, Firebase Admin y /health
  config.js                variables de entorno validadas
  transactionNotifier.js   listener de Firestore, deduplicación, envío y limpieza de tokens
  transactionMessage.js    lógica pura: a quién avisar, texto del mensaje, formato COP
  push.js                  envío FCM compartido, limpieza de tokens y "avisar una sola vez"
  geofence.js              lógica pura de alertas: punto en polígono, lecturas seguidas, textos
  paddockWatcher.js        potreros + posiciones de collar-api → alertas y aviso al propietario
  alertForwarder.js        reenvío de alertas a los trabajadores
  collarClient.js          cliente de /v1/positions de collar-api
test/                      pruebas con node:test
deploy/                    servicio systemd e instalador
```
