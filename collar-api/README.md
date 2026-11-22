# SGE Collar API

Proveedor **externo y simulado** de collares GPS para el ganado de
[SGE](https://github.com/PUJ-ICM-1131/SGE). Permite registrar collares (CRUD) y
entrega la posición en tiempo real de cada vaca, que la app consume por REST.

> **Decisión ética:** se rastrea **solo a los animales**. La ubicación de los trabajadores
> no se registra ni se comparte.

## Cómo funciona la simulación

Cada collar tiene un **anclaje** (el centro del potrero) y un **radio**. La vaca recorre
una curva suave dentro de ese radio, a ratos se detiene a pastar y luego camina
(0–2,5 m/s con el radio de 40 m que trae por defecto).

La posición es una **función determinista** de (id del collar, anclaje, radio, hora):

- no se guarda el recorrido, solo la configuración del collar;
- dos celulares que consultan la misma vaca la ven en el mismo punto.

Los collares se guardan en `data/collars.json`, así que sobreviven a un reinicio.

## Ejecutar

Requiere Node 20 o superior.

```bash
npm install
npm run dev        # recarga al guardar; http://localhost:8080, clave "sge-dev-key"
npm test           # 22 pruebas (node:test)
```

Variables de entorno (ver `.env.example`):

| Variable | Por defecto | Para qué |
|---|---|---|
| `NODE_ENV` | — | Con `production` exige una clave fuerte y escucha solo en loopback |
| `PORT` | `8080` | Puerto HTTP |
| `HOST` | `0.0.0.0` (desarrollo) / `127.0.0.1` (producción) | Interfaz donde escucha |
| `COLLAR_API_KEY` | `sge-dev-key` solo en desarrollo | Clave que exige el header `X-Api-Key` (≥ 32 caracteres en producción) |
| `DATA_FILE` | `data/collars.json` | Dónde se guardan los collares |
| `RATE_LIMIT_PER_MINUTE` | `120` | Peticiones por minuto por IP en `/v1` (429 al pasarse) |
| `TRUST_PROXY` | `loopback` | De qué proxy aceptar `X-Forwarded-For` (Funnel reenvía desde 127.0.0.1) |

En PowerShell: `$env:COLLAR_API_KEY="otra"; npm start`.

Desde el **emulador de Android**, el PC se ve como `http://10.0.2.2:8080/`.
Desde un **celular físico** en la misma Wi-Fi, usa la IP del PC (`ipconfig`).

## API

Todas las rutas `/v1` exigen el header `X-Api-Key`. Los errores siempre son
`{"error": "mensaje", "details"?: ["..."]}`.

| Método | Ruta | Qué hace | Respuestas |
|---|---|---|---|
| GET | `/health` | Estado del servicio (sin clave) | 200 |
| POST | `/v1/collars` | Registra un collar | 201, 400, 409 |
| GET | `/v1/collars?farmId=&status=&animalIds=` | Lista collares (filtros opcionales) | 200 |
| GET | `/v1/collars/{id}` | Detalle de un collar | 200, 404 |
| PATCH | `/v1/collars/{id}` | Actualiza solo los campos enviados | 200, 400, 404, 409 |
| DELETE | `/v1/collars/{id}` | Elimina el collar | 204, 404 |
| GET | `/v1/collars/{id}/position` | Posición actual de un collar | 200, 404, 409 si está inactivo |
| GET | `/v1/positions?farmId=&animalIds=a,b` | Posiciones de los collares **activos** | 200 |

### Collar

```json
{
  "id": "beb02a6d-0b27-423c-a63d-a710e82a6d0b",
  "serial": "SGE-0001",
  "farmId": "finca-1",
  "animalId": "id-del-animal-en-SGE",
  "animalName": "Lola",
  "anchor": { "lat": 4.628, "lng": -74.065 },
  "radiusMeters": 40,
  "status": "ACTIVE",
  "createdAt": "2026-11-22T16:25:16.955Z",
  "updatedAt": "2026-11-22T16:25:16.955Z"
}
```

| Campo | Reglas |
|---|---|
| `serial` | Obligatorio, único, 1–40 letras, números o guiones (se guarda en mayúsculas) |
| `farmId` | Obligatorio |
| `animalId` | Opcional; un animal solo puede tener un collar (409 si se repite) |
| `animalName` | Opcional, máx. 80 caracteres |
| `anchor` | Obligatorio: `{lat, lng}` válidos |
| `radiusMeters` | 10–500, por defecto 40 |
| `status` | `ACTIVE` (por defecto) o `INACTIVE`; los inactivos no reportan posición |

### Posiciones

`GET /v1/positions?farmId=finca-1`

```json
{
  "generatedAt": 1795364716969,
  "positions": [
    {
      "collarId": "beb02a6d-…",
      "serial": "SGE-0001",
      "animalId": "id-del-animal-en-SGE",
      "lat": 4.6280957,
      "lng": -74.0651394,
      "speedMps": 0.11,
      "headingDeg": 91.7,
      "batteryPercent": 80,
      "timestamp": 1795364716969
    }
  ]
}
```

Para probar a mano, abre `requests.http` en Android Studio, IntelliJ o VS Code
(extensión REST Client).

## Estructura

```
src/
  index.js                 arranque
  config.js                variables de entorno validadas
  app.js                   arma Express con sus dependencias (inyectables en pruebas)
  routes/                  rutas delgadas: petición → servicio → respuesta
  domain/                  reglas de negocio, validación y simulación (sin HTTP)
  repository/              en memoria (pruebas) y en archivo JSON (ejecución)
  middleware/              API key, límite de peticiones y manejo de errores
deploy/                    servicio systemd e instalador para el LXC de Proxmox
test/                      pruebas con node:test
```

## Desplegar en Proxmox con Tailscale Funnel

Funnel publica la API en `https://<maquina>.<tailnet>.ts.net` con un certificado HTTPS
automático, sin abrir puertos en el router. Tailscale termina el HTTPS y reenvía a
`http://127.0.0.1:8080`, así que **la API solo escucha en loopback** y no queda
expuesta en la red local.

### 1. Contenedor LXC (en Proxmox)

1. Crea un CT **no privilegiado** con la plantilla **Debian 13**. Con 1 vCPU, 512 MB de RAM
   y 4 GB de disco sobra. Deja activado `nesting=1`, que viene por defecto.
2. Dale acceso a `/dev/net/tun`, que Tailscale necesita. En el host:
   ```bash
   pct set <CTID> --dev0 /dev/net/tun
   ```
   En Proxmox 7 se hace en `/etc/pve/lxc/<CTID>.conf`:
   ```
   lxc.cgroup2.devices.allow: c 10:200 rwm
   lxc.mount.entry: /dev/net/tun dev/net/tun none bind,create=file
   ```
3. Reinicia el CT.

### 2. Tailscale y Funnel habilitados (una sola vez)

En la consola de administración de Tailscale, activa **MagicDNS** y los
**certificados HTTPS**, y permite **Funnel** para el nodo (atributo `funnel` en la
política del tailnet). La primera vez que ejecutes `tailscale funnel`, la CLI te da el
enlace para habilitarlo si falta algo.

Dentro del CT:

```bash
curl -fsSL https://tailscale.com/install.sh | sh
tailscale up --hostname=sge-collar-api
```

### 3. Instalar la API

```bash
apt-get update && apt-get install -y git
git clone <url-de-este-repo> /root/sge-collar-api
cd /root/sge-collar-api
bash deploy/install.sh        # imprime la API key la primera vez
tailscale funnel --bg 8080
tailscale funnel status       # → https://sge-collar-api.<tailnet>.ts.net
```

`install.sh` hace lo siguiente:
- instala Node 24 LTS desde NodeSource si hace falta (el Node 20 de Debian 13 ya llegó a fin de vida);
- copia el código a `/opt/sge-collar-api`;
- genera una clave de 64 caracteres en `/etc/sge-collar-api/env` (solo la lee root);
- deja el servicio systemd `sge-collar-api` corriendo con un usuario sin privilegios.

Los datos quedan en `/var/lib/sge-collar-api/collars.json`.

Con `--bg`, Funnel sigue activo después de reiniciar el CT.

### Operación

```bash
systemctl status sge-collar-api
journalctl -u sge-collar-api -f                 # log de peticiones con la IP real del cliente
git pull && bash deploy/install.sh              # actualizar (conserva la clave y los datos)
curl https://sge-collar-api.<tailnet>.ts.net/health
```

Si el servicio falla con `status=226/NAMESPACE`, el CT no tiene `nesting=1`. Actívalo,
o comenta las líneas `Protect*`/`Private*` del `.service`.

### Seguridad: qué protege y qué no

- HTTPS de extremo a extremo hasta el CT, con certificado de Tailscale.
- Clave obligatoria en `/v1` (comparación en tiempo constante). El servicio no arranca
  en producción con una clave débil.
- Límite de peticiones por IP real del cliente (429 + `Retry-After`), también para los
  intentos con clave errada.
- El servicio corre sin privilegios, con el sistema de archivos en solo lectura excepto
  su carpeta de datos.
- **Límite conocido:** la clave viaja dentro del APK, así que alguien que lo descompile
  puede extraerla. Para un proveedor simulado del curso es aceptable. En un producto real,
  la app se autenticaría con Firebase y un backend propio le hablaría al proveedor.
- Si la clave se filtra, cámbiala en `/etc/sge-collar-api/env`, ejecuta
  `systemctl restart sge-collar-api` y actualiza `local.properties` en SGE.
