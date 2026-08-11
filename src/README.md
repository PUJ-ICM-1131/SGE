# src/

Stack decidido en el Paso 5 (ver `docs/4-arquitectura/`): **app Android nativa con Kotlin + Jetpack Compose** (ADR-001), persistencia local con **Room/SQLite** (ADR-002), **sin backend propio** — backup a Google Drive vía WorkManager (ADR-003).

Aquí vivirá el proyecto Gradle. Estructura prevista, alineada con las capas de `estilo_arquitectonico.md`:

```
src/
├── build.gradle.kts            # raíz del proyecto Gradle
├── settings.gradle.kts
└── app/
    ├── build.gradle.kts
    └── src/
        ├── main/
        │   ├── java/.../sge/
        │   │   ├── ui/         # Compose Screens + ViewModels + Navigation
        │   │   ├── domain/     # Modelos de dominio + Use Cases (Kotlin puro)
        │   │   ├── data/       # Room Entities, DAOs, Repositories, Backup Service
        │   │   └── di/         # Módulos Hilt
        │   └── res/
        └── test/               # Tests unitarios (capa domain, sin emulador)
```

El scaffolding se hace al iniciar la **fase de implementación** (tras el Paso 10 del roadmap), no antes. El detalle del esquema de BD sale del Paso 6 (`docs/5-modelo-datos/`).
