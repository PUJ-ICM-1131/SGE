# ADR-002: Base de datos local — Room (SQLite)

| Campo | Valor |
|---|---|
| **Estado** | Aceptada |
| **Fecha** | 2026-07-09 |
| **Decisores** | Administrador del proyecto |
| **Origen** | RNF-06 (sin pérdida de datos), RNF-07 (offline-first) |

## Contexto

La aplicación necesita una base de datos local que sea la fuente de verdad de todas las transacciones. Los requisitos clave son:

- **Offline-first** (RNF-07): toda operación CRUD debe funcionar sin conexión.
- **Sin pérdida de datos** (RNF-06): los registros deben persistir aunque falle la app.
- **Consultas con filtros** (RF-05): por fecha, categoría y actividad.
- **Cálculo de balances** (RF-03): agregaciones por periodo y actividad.
- **Modelo relacional simple** (6 entidades, ver diagrama de clases del Paso 4).

Dado que el framework elegido es Kotlin (ADR-001), las opciones se limitan al ecosistema Android/JVM.

## Opciones evaluadas

### Opción A: Room (SQLite) — elegida
- ORM oficial de Android (parte de Jetpack).
- SQLite como motor: maduro, probado, sin servidor.
- Soporte de migraciones, validación de esquema en compilación, integración con coroutines y Flow.
- Consultas SQL reales: ideales para filtros complejos y agregaciones de balance.

### Opción B: SQLDelight
- Genera código Kotlin type-safe a partir de SQL.
- Multiplataforma (KMP-ready).
- **Contras:** curva de aprendizaje extra, menor documentación que Room, no aporta ventaja si no hay KMP.

### Opción C: Realm (MongoDB)
- Base de datos orientada a objetos con sync integrado.
- **Contras:** modelo de licenciamiento complejo; sync nativo va a MongoDB Atlas (servicio de pago para sync real); overhead innecesario para un solo usuario sin sync remoto.

## Decisión

**Room (SQLite).**

## Justificación

1. **First-party:** Room es la solución oficial de Google para persistencia en Android. Máxima compatibilidad con el stack elegido (ADR-001).
2. **SQL real:** las consultas de balance con agrupación por periodo y actividad (RF-03) y los filtros combinados del historial (RF-05) son naturales en SQL.
3. **Validación en compilación:** Room verifica las queries SQL contra el esquema en tiempo de compilación — reduce errores en runtime.
4. **Integración con Kotlin coroutines y Flow:** permite operaciones asíncronas y observación reactiva de datos (el balance se actualiza automáticamente al registrar una transacción).
5. **Migraciones:** soporte nativo para evolucionar el esquema sin perder datos del usuario.
6. **Sin overhead:** no introduce dependencias externas ni servicios de terceros.

## Consecuencias

- Las entidades del dominio (Transaction, Category, User, etc.) se mapean a tablas Room con `@Entity`.
- Los DAOs definen las queries de consulta, filtro y agregación.
- El modelo de datos detallado se define en el Paso 6.
- La estrategia de backup se define en ADR-003.
