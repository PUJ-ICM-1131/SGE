# Changelog

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/).

## [Unreleased]
### Added
- Definición del proyecto y requisitos funcionales/no funcionales (Pasos 1-2).
- Historias de usuario (HU-01 a HU-12) con criterios de aceptación Gherkin y backlog priorizado MoSCoW (Paso 3).
- Casos de uso (UC-01 a UC-12), diagrama de clases de dominio y diagramas de secuencia en Mermaid/PlantUML (Paso 4).
- Arquitectura: estilo MVVM + Clean Architecture ligera, ADRs (framework, BD, backend/sync), diagramas C4 (Paso 5).
- CI: validación de diagramas Mermaid de `docs/` con mermaid-cli (el job Android se agregará cuando exista código en `src/`).
- Pipeline de render de diagramas: `scripts/render-diagrams.sh` genera SVG desde los bloques PlantUML de `docs/` (Smetana, sin Graphviz) y el CI verifica sintaxis y frescura de los SVG commiteados vía checksum de la fuente.

### Changed
- Diagramas C4 v2: migrados de la sintaxis experimental C4 de Mermaid a C4-PlantUML, con SVG embebido visible en GitHub y fuente colapsada.
- Diagrama de casos de uso: ahora visible en GitHub como SVG embebido (antes bloque PlantUML sin render).
- Diagramas de secuencia v2: participantes alineados con la arquitectura del Paso 5 (Screens/ViewModels/UseCases/Repositories) y métodos en inglés; nueva tabla de mapeo participante → capa.
- Diagrama de clases v2: atributo `activityGroup` en `Category`, multiplicidad `PhotoCapture 1 → 0..1 ExtractionResult`, notación de opcionalidad UML `[0..1]`.
- `src/README.md`: refleja el stack decidido (Kotlin/Compose/Room) y la estructura Gradle prevista.

### Fixed
- Incoherencia con ADR-003: se eliminó la "Cola de Sincronización" de SD-01 y el flujo alternativo 6a de UC-01/UC-02 ya no habla de encolar transacciones — el modelo es local-only con backup a Google Drive.
