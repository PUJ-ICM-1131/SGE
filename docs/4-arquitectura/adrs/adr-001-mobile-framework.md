# ADR-001: Framework móvil — Kotlin + Jetpack Compose

| Campo | Valor |
|---|---|
| **Estado** | Aceptada |
| **Fecha** | 2026-07-09 (actualizada 2026-08-17) |
| **Decisores** | Equipo de desarrollo (4 personas) |
| **Origen** | Restricción D5 (Android nativo, iOS fuera de alcance), requisitos del curso |

## Contexto

El proyecto necesita un framework para construir la aplicación móvil Android. Los requisitos clave que condicionan esta elección son:

- **Android-only** (decisión D5): iOS está explícitamente fuera de alcance.
- **Offline-first** (RNF-07): el registro de transacciones debe funcionar sin conexión.
- **Multi-usuario** (RNF-08): propietario y trabajadores comparten datos via Firebase.
- **GPS + Mapas** (RNF-10): rastreo en tiempo real de inspecciones + geolocalización de fotos de animales.
- **Cámara** (RF-09, RF-20): fotos de anotaciones (OCR) y fotos de animales con GPS.
- **Sensores** (RNF-09): acelerómetro + magnetómetro/brújula.
- **Interfaz simple** (RNF-01): botones grandes, íconos, texto mínimo.
- **Equipo de 4 desarrolladores**, semestre académico 2026-3.

## Opciones evaluadas

### Opción A: Kotlin + Jetpack Compose (elegida)
- UI declarativa moderna, estándar oficial de Android.
- Room (SQLite) para persistencia local — API first-party, madura, con soporte de migraciones.
- Firebase SDK nativo para Auth, Firestore y FCM.
- Google Maps SDK, Fused Location Provider, CameraX — todo first-party.
- Sin capa bridge ni dependencia de plugins de terceros.
- **Contras:** solo Android; si el cliente necesita iOS en el futuro, requiere reescritura.

### Opción B: Flutter (Dart)
- Multiplataforma (Android + iOS).
- Hot reload acelera iteración.
- BD local vía drift/sqflite, pero sin soporte first-party para offline-first.
- Plugins necesarios para cámara, GPS, sensores, permisos.
- **Contras:** Dart como lenguaje adicional; offline-first requiere más configuración manual; plugins de terceros para hardware; app más pesada (~15-20 MB base).

### Opción C: React Native (TypeScript)
- Multiplataforma. Gran ecosistema npm.
- Expo simplifica setup inicial.
- **Contras:** bridge JS con overhead de rendimiento; offline-first significativamente más complejo; menos robusto para apps data-heavy offline; módulos nativos para features avanzados de hardware.

## Decisión

**Kotlin + Jetpack Compose.**

## Justificación

1. **Android-only elimina la ventaja de cross-platform.** Flutter y React Native solo aportan valor si hay necesidad de iOS, que está fuera de alcance (D5).
2. **Offline-first es el reto técnico central.** Room + Firestore SDK son soluciones first-party de Google para Android, maduras y documentadas. No hay equivalente con el mismo nivel de integración en Flutter o React Native.
3. **Acceso a hardware sin plugins.** GPS (Fused Location Provider), cámara (CameraX), sensores (SensorManager), contactos, permisos — todo es API nativa de Android. Sin dependencia de wrappers de terceros.
4. **Firebase SDK nativo** ofrece la mejor integración para Auth, Firestore y FCM en Android.
5. **Google Maps SDK** nativo para visualización de recorridos y ubicaciones de animales.
6. **Jetpack Compose** ofrece UI declarativa moderna con Material 3, ideal para una interfaz simple con botones grandes y texto mínimo (RNF-01).
7. **Alineación con el curso:** Kotlin + Compose es el stack que se enseña en el semestre.

## Stack completo

| Categoría | Tecnología |
|---|---|
| Lenguaje + IDE | Kotlin + Android Studio |
| UI | Jetpack Compose + Material 3 |
| Arquitectura | MVVM + Navigaton3 + ViewModel |
| Mapas y GPS | Google Maps SDK + Fused Location Provider |
| Backend | Firebase Auth + Firestore + FCM |
| HTTP | Retrofit (Nominatim REST API) |
| Imágenes | Coil o Glide |
| Cámara | CameraX |
| Sensores | Acelerómetro + Magnetómetro/Brújula |
| Caché local | Room / SQLite |

## Consecuencias

- El stack de desarrollo es: **Kotlin, Jetpack Compose, Gradle**.
- La persistencia es **Room como caché local + Firestore como fuente remota** (ADR-002).
- El backend es **Firebase** (Auth + Firestore + FCM) (ADR-003).
- Si en el futuro el cliente necesita iOS, se evaluará KMP (Kotlin Multiplatform) como camino de migración incremental.
