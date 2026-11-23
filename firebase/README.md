# Firebase — Security Rules de Firestore

Reglas de seguridad de Firestore de SGE (issue #28). La app ya valida los permisos antes de
escribir; estas reglas son la última línea de defensa si alguien se salta la app.

| Archivo | Qué es |
|---|---|
| `firestore.rules` | Las reglas |
| `firebase.json` / `.firebaserc` | Configuración de Firebase CLI (proyecto `icm-sge`) |
| `test/firestore.rules.test.js` | Pruebas de las reglas contra el emulador |

## Resumen de permisos

| Colección | Leer | Crear | Editar | Borrar |
|---|---|---|---|---|
| `users/{uid}` | el mismo usuario | el mismo usuario | el mismo usuario | el mismo usuario |
| `farms/{farmId}` | miembros | quien queda como `ownerUid` | dueño (sin cambiar `ownerUid`) | nadie |
| `members/{uid}` | miembros | dueño · el creador de la finca como OWNER · un usuario como WORKER con el código de invitación (sin `grants`) | dueño (no se quita su propio rol; otorga `grants`, #67) · el propio miembro sin tocar `role`, `isActive` ni `grants` | dueño (a otros) · el propio trabajador |
| `categories` | miembros | dueño | dueño | dueño |
| `transactions` | miembros | miembros (`createdBy` = su uid, monto entero > 0) | dueño (sin cambiar `createdBy`/`createdAt`) | dueño |
| `animalRecords`, `contacts`, `reminders`, `inspectionRoutes` (+ `points`) | miembros | miembros | miembros | miembros |
| `paddocks` (#71) | miembros | miembros (`createdBy` = su uid, mínimo 3 puntos) | dueño (sin cambiar `createdBy`/`createdAt`) | dueño |
| `alerts` (#73) | miembros | nadie (las crea sge-notifier) | dueño, solo `forwardedAt` y `forwardedBy` = su uid | nadie |
| `chats/{workerUid}` (+ `messages`) (#74) | dueño y ese trabajador | dueño y ese trabajador (`senderUid`/`lastSenderUid` = su uid, texto de 1 a 1000) | resumen: igual que crear; mensajes: nadie | nadie |

"Miembro" = tiene `farms/{farmId}/members/{uid}` con `isActive` distinto de `false`.

## Publicar las reglas

### Opción A — Consola de Firebase (sin instalar nada)

1. https://console.firebase.google.com → proyecto **icm-sge** → **Firestore Database** → pestaña **Reglas**.
2. Borra todo lo que hay en el editor y pega el contenido de `firestore.rules`.
3. **Publicar**.

### Opción B — Firebase CLI

```bash
cd firebase
npm install
npx firebase login
npm run deploy
```

## Probar las reglas (emulador)

Requiere **Node 18+** y **Java 11+** (el emulador de Firestore es un `.jar`).

```bash
cd firebase
npm install
npm test
```

Corre las pruebas de `test/` contra el emulador local: no toca la base de datos real.

## Probar con dos cuentas en la app

Mientras no exista la unión con código de invitación (#29), el trabajador se crea a mano:

1. Registra la cuenta A con **"Crear finca"** → queda como OWNER.
2. Registra la cuenta B (cualquier opción) y copia su UID desde **Authentication → Users**.
3. En **Firestore**, en `farms/{farmId de A}/members`, crea un documento con ID = UID de B:
   - `role` (string): `WORKER`
   - `isActive` (boolean): `true`
   - `joinedAt` (number): `0`
4. En `users/{UID de B}`, pon `farmId` (string) = el farmId de A.
5. Entra con B: en **Más** no deben aparecer *Categorías* ni *Trabajadores*.
