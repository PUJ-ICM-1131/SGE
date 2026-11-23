# Changelog

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/).

## [Unreleased]
### Added
- Potreros (#71): al finalizar una inspección que vuelve a 20 m o menos del inicio y encierra al menos 100 m², se puede guardar como potrero con nombre. Se dibuja con relleno translúcido y etiqueta con su área (m², y hectáreas desde 1 ha); el propietario lo renombra o elimina al tocarlo. Sincronizado con `farms/{farmId}/paddocks` (BD v9) y con Security Rules y pruebas.
- Animales compartidos por finca (#62): se filtran por `farmId` en vez de por usuario y se sincronizan con `farms/{farmId}/animalRecords` (crear, editar y borrar llegan a todos los miembros). Los animales registrados antes pasan a la finca activa de quien los creó. BD v8 (`animals.updatedAt`).
- Sincronización offline (#32): subida con WorkManager (`SyncWorker`) usando el mismo id en Room y Firestore, reintentos con espera exponencial, estado `FAILED` sin reintentos infinitos cuando las Security Rules rechazan, borrados pendientes (BD v7), listeners Firestore → Room con last-write-wins, y aviso de registros pendientes en el inicio y en el historial.
- Roles y permisos (#28): propietario y trabajador con tabla única de permisos (`Permission`), sesión con el rol en vivo (`SessionRepository` / `SessionViewModel`), verificación de permisos en los repositorios, opciones de propietario ocultas para el trabajador, rutas restringidas protegidas y Security Rules de Firestore con pruebas en `firebase/`.
- Permisos de vista otorgables (#67): el propietario activa "Ver balance y reportes" por trabajador desde Trabajadores (`members/{uid}.grants`, en vivo). `VIEW_REPORTS` pasa a `VIEW_BALANCE`, que por defecto solo tiene el propietario: sin él se ocultan el balance del inicio, el resumen del historial y Reportes (ruta protegida). Las Security Rules impiden que un trabajador se otorgue permisos.
- Modelo de datos (#23): entidades Room de transacciones, contactos y recordatorios (BD v6, migración 5→6 sin pérdida de datos), `syncStatus` en todas las tablas, repositorios con interfaz en dominio y escritura Room → Firestore, estructura de Firestore documentada en `docs/5-modelo-datos/`. Al registrarse con "Crear finca" se crea la finca en Firestore con sus categorías por defecto.
- Definición del proyecto y requisitos funcionales/no funcionales (Pasos 1-2).
- Historias de usuario (HU-01 a HU-12) con criterios de aceptación Gherkin y backlog priorizado MoSCoW (Paso 3).
- Casos de uso (UC-01 a UC-12), diagrama de clases de dominio y diagramas de secuencia en Mermaid/PlantUML (Paso 4).
- Arquitectura: estilo MVVM + Clean Architecture ligera, ADRs (framework, BD, backend/sync), diagramas C4 (Paso 5).
- CI: validación de diagramas Mermaid de `docs/` con mermaid-cli (el job Android se agregará cuando exista código en `src/`).
- Pipeline de render de diagramas: `scripts/render-diagrams.sh` genera SVG desde los bloques PlantUML de `docs/` (Smetana, sin Graphviz) y el CI verifica sintaxis y frescura de los SVG commiteados vía checksum de la fuente.
- Autenticación biométrica (huella dactilar) con `BiometricPrompt`: inicio rápido al abrir la app cuando hay sesión de Firebase, oferta de activación tras el primer login/registro y toggle en Configuración. La huella no reemplaza a Firebase Auth: si la sesión expiró se piden las credenciales completas.

### Changed
- Diagramas C4 v2: migrados de la sintaxis experimental C4 de Mermaid a C4-PlantUML, con SVG embebido visible en GitHub y fuente colapsada.
- Diagrama de casos de uso: ahora visible en GitHub como SVG embebido (antes bloque PlantUML sin render).
- Diagramas de secuencia v2: participantes alineados con la arquitectura del Paso 5 (Screens/ViewModels/UseCases/Repositories) y métodos en inglés; nueva tabla de mapeo participante → capa.
- Diagrama de clases v2: atributo `activityGroup` en `Category`, multiplicidad `PhotoCapture 1 → 0..1 ExtractionResult`, notación de opcionalidad UML `[0..1]`.
- `src/README.md`: refleja el stack decidido (Kotlin/Compose/Room) y la estructura Gradle prevista.

### Fixed
- Controles de los mapas tapados (#60): en el mapa de animales el zoom de Google Maps sube por encima del botón de registrar (que sigue abajo a la derecha), y el botón se oculta mientras está abierta la tarjeta de un animal; en inspección de campo y en medir distancia, los botones, avisos y tarjetas que flotan sobre el mapa ya no tapan el zoom, el botón de mi ubicación ni el logo de Google/Mapbox (el mapa se corre con `contentPadding`).
- Incoherencia con ADR-003: se eliminó la "Cola de Sincronización" de SD-01 y el flujo alternativo 6a de UC-01/UC-02 ya no habla de encolar transacciones — el modelo es local-only con backup a Google Drive.
