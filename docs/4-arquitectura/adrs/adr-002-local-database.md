# ADR-002: Persistencia — Room (caché local) + Firestore (fuente remota)

| Campo | Valor |
|---|---|
| **Estado** | Aceptada (reemplaza versión anterior: Room-only) |
| **Fecha** | 2026-08-11 |
| **Decisores** | Equipo de desarrollo (4 personas) |
| **Origen** | RNF-06 (sin pérdida de datos), RNF-07 (offline-first), RNF-08 (multi-usuario), requisitos del curso (backend de tercero) |

## Contexto

El proyecto pivotó de single-user/local-only a multi-usuario con backend. Los requisitos clave ahora son:

- **Multi-usuario** (RNF-08): propietario y trabajadores comparten datos de la misma finca en tiempo real.
- **Backend de tercero** (requisito del curso): Firebase (Auth + Firestore + Storage + FCM).
- **Offline-first** (RNF-07): toda operación CRUD debe funcionar sin conexión.
- **Sin pérdida de datos** (RNF-06): los registros deben persistir aunque falle la app o no haya red.
- **Consultas con filtros** (RF-05): por fecha, categoría y actividad.
- **Cálculo de balances** (RF-03): agregaciones por periodo y actividad.
- **Fotos de animales** (RF-20): imágenes almacenadas en Firebase Storage.

## Decisión

**Room (SQLite) como caché local + Cloud Firestore como fuente de verdad remota + Firebase Storage para fotos.**

## Justificación

1. **Firestore SDK tiene offline-first nativo:** Firestore mantiene una caché local automática. Cuando no hay red, las escrituras se encolan y se sincronizan al recuperar conexión. Esto satisface RNF-07 sin implementar sync manual.
2. **Room sigue siendo valioso como caché estructurada:** para consultas complejas (balances con agrupación, filtros combinados), SQL es más expresivo que las queries de Firestore. Room sirve como caché local optimizada para lectura.
3. **Firebase Storage para fotos:** las fotos de animales (UC-20) y capturas OCR (UC-09) se almacenan en Firebase Storage. Las URIs se guardan en Firestore/Room.
4. **Firebase Auth maneja la autenticación:** reemplaza la autenticación local anterior.
5. **FCM para notificaciones push:** integración natural con el ecosistema Firebase.

## Arquitectura de datos

```
┌──────────────┐     ┌───────────────┐     ┌──────────────────┐
│   UI Layer   │────▶│  Room (caché) │     │    Firestore     │
│  (Compose)   │     │   SQLite      │◀───▶│  (fuente remota) │
└──────────────┘     └───────────────┘     └──────────────────┘
                                                     │
                                           ┌─────────┴────────┐
                                           │ Firebase Storage  │
                                           │   (fotos)         │
                                           └──────────────────┘
```

- **Escritura:** UI → Room (inmediato, sin bloqueo) → Firestore (asíncrono, con retry automático del SDK).
- **Lectura:** UI ← Room (rápido, SQL) ← Firestore (sync en background vía listeners).
- **Conflictos:** poco probables porque cada usuario crea transacciones nuevas (no editan las mismas). El propietario es el único que edita/elimina. Firestore resuelve conflictos por last-write-wins.

## Consecuencias

- Las entidades del dominio se mapean tanto a tablas Room (`@Entity`) como a documentos Firestore.
- Los repositorios implementan el patrón "write-through": escriben en Room y Firestore; leen de Room (populada por listeners de Firestore).
- El modelo de datos detallado se define en el Paso 6.
- ADR-003 (anterior: backup a Google Drive) queda reemplazada por esta ADR: la sincronización es nativa de Firestore, no un backup periódico.
