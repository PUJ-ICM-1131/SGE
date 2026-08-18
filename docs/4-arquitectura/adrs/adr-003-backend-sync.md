# ADR-003: Backend y sincronización — Firebase como backend remoto

| Campo | Valor |
|---|---|
| **Estado** | Aceptada (reemplaza versión anterior: local-only + backup a Google Drive) |
| **Fecha** | 2026-08-11 |
| **Decisores** | Equipo de desarrollo (4 personas) |
| **Origen** | RNF-07 (offline-first), RNF-08 (multi-usuario), requisitos del curso (backend de tercero, notificaciones push, autenticación) |

## Contexto

El proyecto pivotó de single-user/local-only a multi-usuario con backend. La estrategia anterior (Room como fuente de verdad + backup periódico a Google Drive vía WorkManager) ya no es viable porque:

1. **Multi-usuario** (RNF-08): propietario y trabajadores necesitan compartir datos en tiempo real. Un backup periódico de SQLite no permite esto.
2. **Requisito del curso**: se exige un backend de tercero con autenticación, almacenamiento y notificaciones push.
3. **Fotos compartidas**: las fotos de animales (UC-20) y recibos deben ser accesibles por todos los usuarios de la finca, no solo por quien las tomó.

La ADR-002 ya decidió usar Room como caché local + Firestore como fuente remota. Esta ADR define la estrategia completa de backend y sincronización.

## Decisión

**Firebase como ecosistema completo de backend:**

| Servicio | Función |
|---|---|
| **Firebase Auth** | Autenticación (email/password), gestión de sesión, UIDs |
| **Cloud Firestore** | Base de datos remota (documentos/colecciones), sincronización offline nativa |
| **Firebase Storage** | Almacenamiento de fotos (animales, recibos, capturas OCR) |
| **Firebase Cloud Messaging (FCM)** | Notificaciones push (recordatorios, alertas) |

## Justificación

1. **Firestore tiene offline-first nativo:** el SDK mantiene una caché local, encola escrituras sin conexión y sincroniza automáticamente al recuperar red. Satisface RNF-07 sin código custom de sync.
2. **Ecosistema integrado:** Auth, Firestore, Storage y FCM comparten configuración, consola y SDK. No hay que integrar servicios dispares.
3. **Free tier (Spark) suficiente:** para el volumen esperado (1 finca, ~4 usuarios, ~100 transacciones/mes), el plan gratuito de Firebase cubre holgadamente:
   - Firestore: 50K lecturas/día, 20K escrituras/día, 1 GiB almacenamiento
   - Storage: 5 GB, 1 GB/día de descarga
   - Auth: ilimitado para email/password
   - FCM: sin límite de mensajes
4. **Room complementa como caché SQL:** las consultas complejas (balances con agrupación, filtros combinados) se ejecutan contra Room (SQL expresivo). Room se alimenta de los listeners de Firestore (ver ADR-002).
5. **Google Drive ya no es necesario:** la sincronización nativa de Firestore reemplaza por completo el backup periódico a Google Drive. Los datos están en la nube sin necesidad de exportar/importar archivos SQLite.

## Arquitectura de sincronización

```
┌────────────┐     ┌───────────┐     ┌──────────────┐     ┌─────────────┐
│  UI Layer  │────▶│   Room    │────▶│  Firestore   │     │  Firebase   │
│ (Compose)  │     │  (caché)  │◀────│  (remoto)    │     │  Storage    │
└────────────┘     └───────────┘     └──────────────┘     │  (fotos)    │
                                            │              └─────────────┘
                                     ┌──────┴──────┐
                                     │ Firebase    │
                                     │ Auth        │
                                     └─────────────┘
```

### Flujo de escritura

1. El usuario crea/edita un registro en la UI.
2. El repositorio escribe en Room (inmediato, sin bloqueo → la UI refleja el cambio al instante).
3. El repositorio escribe en Firestore (asíncrono). Si no hay red, el SDK de Firestore encola la operación y la envía al recuperar conexión.
4. Si hay foto adjunta, se sube a Firebase Storage y la URI resultante se guarda en el documento de Firestore y en Room.

### Flujo de lectura

1. Al iniciar la app, se registran listeners (`addSnapshotListener`) en las colecciones relevantes de Firestore.
2. Los cambios remotos llegan vía listener → se escriben en Room.
3. La UI observa Room (via `Flow<List<T>>` de los DAOs) → se actualiza reactivamente.

### Resolución de conflictos

- **Estrategia: last-write-wins** (por defecto en Firestore).
- Los conflictos son improbables por el modelo de uso: los trabajadores **crean** transacciones nuevas; solo el propietario **edita/elimina**. No hay edición concurrente del mismo documento.
- Si dos usuarios crean transacciones simultáneamente sin conexión, ambas se sincronizan sin conflicto (son documentos distintos con IDs únicos).

## Modelo de datos en Firestore

```
farms/{farmId}
├── name, latitude, longitude, inviteCode, createdAt
├── users/{userId}
│   └── name, email, role, isActive, createdAt
├── transactions/{transactionId}
│   └── type, date, amount, note, categoryId, contactId, ...
├── categories/{categoryId}
│   └── name, type, activityGroup, isReserved, isActive
├── contacts/{contactId}
│   └── name, phone, email, contactType
├── inspectionRoutes/{routeId}
│   ├── startTime, endTime, distanceKm, status, userId
│   └── points/{pointId}
│       └── latitude, longitude, timestamp, order
├── animalRecords/{recordId}
│   └── name, photoUri, latitude, longitude, locationName, ...
└── reminders/{reminderId}
    └── title, description, scheduledAt, repeat, active, userId
```

- **Farm es la raíz:** todo dato pertenece a una finca. Las reglas de seguridad de Firestore restringen acceso a usuarios miembros de la finca.
- **Subcolecciones vs. campos:** `points` es subcolección de `inspectionRoutes` por volumen (puede haber cientos de puntos por ruta). El resto son colecciones de nivel 2 bajo `farms`.

## Reglas de seguridad (esquema)

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /farms/{farmId}/{document=**} {
      allow read, write: if request.auth != null
        && exists(/databases/$(database)/documents/farms/$(farmId)/users/$(request.auth.uid));
    }
  }
}
```

Solo los usuarios registrados en la finca pueden leer/escribir sus datos. Las restricciones por rol (propietario vs. trabajador) se validan adicionalmente en la capa de aplicación.

## Consecuencias

- **ADR-003 anterior (backup a Google Drive) queda obsoleta.** La sincronización nativa de Firestore elimina la necesidad de exportar/importar archivos SQLite.
- **WorkManager ya no se usa para backup.** Se puede usar para otras tareas (ej. recordatorios locales, limpieza de caché).
- **Dependencia de Firebase:** si Google cambia los términos del free tier o depreca Firebase, habría que migrar. Se mitiga porque Firestore exporta datos fácilmente y Room mantiene una copia local completa.
- **La exportación de reportes (RF-08)** sigue generando archivos locales (PDF/Excel) compartidos vía Android share sheet — no depende de Firestore.
- **El módulo OCR (Could)** usa ML Kit on-device, no requiere backend — es independiente de esta decisión.
