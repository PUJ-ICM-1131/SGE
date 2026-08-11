# ADR-001: Framework móvil — Kotlin + Jetpack Compose

| Campo | Valor |
|---|---|
| **Estado** | Aceptada |
| **Fecha** | 2026-07-09 |
| **Decisores** | Administrador del proyecto (cliente/desarrollador) |
| **Origen** | Restricción D5 (Android nativo, iOS fuera de alcance) |

## Contexto

El proyecto necesita un framework para construir la aplicación móvil Android. Los requisitos clave que condicionan esta elección son:

- **Android-only** (decisión D5): iOS está explícitamente fuera de alcance.
- **Offline-first** (RNF-07): el registro de transacciones debe funcionar sin conexión.
- **Acceso a cámara** (RF-09, Could): si se implementa el épico de IA, se necesita acceso nativo a la cámara.
- **Interfaz simple** (RNF-01): botones grandes, íconos, texto mínimo.
- **Un solo desarrollador, plazo corto** (~3-4 semanas de desarrollo).

## Opciones evaluadas

### Opción A: Kotlin + Jetpack Compose (elegida)
- UI declarativa moderna, estándar oficial de Android.
- Room (SQLite) para persistencia local — API first-party, madura, con soporte de migraciones.
- WorkManager para tareas en segundo plano (backup periódico).
- CameraX para acceso a cámara.
- Sin capa bridge ni dependencia de plugins de terceros.
- **Contras:** solo Android; si el cliente necesita iOS en el futuro, requiere reescritura.

### Opción B: Flutter (Dart)
- Multiplataforma (Android + iOS).
- Hot reload acelera iteración.
- BD local vía drift/sqflite, pero sin soporte first-party para offline-first.
- Plugins necesarios para cámara, permisos, file system.
- **Contras:** Dart como lenguaje adicional; offline-first requiere más configuración manual; app más pesada (~15-20 MB base).

### Opción C: React Native (TypeScript)
- Multiplataforma. Gran ecosistema npm.
- Expo simplifica setup inicial.
- **Contras:** bridge JS con overhead de rendimiento; offline-first significativamente más complejo (WatermelonDB o similar); menos robusto para apps data-heavy offline; módulos nativos para features avanzados.

## Decisión

**Kotlin + Jetpack Compose.**

## Justificación

1. **Android-only elimina la ventaja de cross-platform.** Flutter y React Native solo aportan valor si hay necesidad de iOS, que está fuera de alcance (D5).
2. **Offline-first es el reto técnico central.** Room + WorkManager son soluciones first-party de Google para Android, maduras y documentadas. No hay equivalente con el mismo nivel de integración en Flutter o React Native.
3. **Sin capa bridge = menos superficie de bugs.** Un solo desarrollador con plazo corto no puede permitirse depurar problemas de interop entre Dart/JS y el runtime nativo.
4. **CameraX** resuelve el acceso a cámara para el épico de IA sin plugins de terceros.
5. **Jetpack Compose** ofrece UI declarativa moderna con Material 3, ideal para una interfaz simple con botones grandes y texto mínimo (RNF-01).

## Consecuencias

- El stack de desarrollo es: **Kotlin, Jetpack Compose, Gradle**.
- La persistencia local será **Room** (decisión detallada en ADR-002).
- Si en el futuro el cliente necesita iOS, se evaluará KMP (Kotlin Multiplatform) como camino de migración incremental — no requiere reescribir desde cero.
