# Paso 4 — Casos de uso y diagramas UML

Diagrama de casos de uso, diagramas de clases y de secuencia derivados de las
historias de usuario del Paso 3.

## Convención de notación del diagrama de casos de uso

Las asociaciones actor–caso de uso se dibujan como **asociaciones navegables
(con flecha)**, no como líneas simples. Ambas formas son UML válido; aquí la
flecha se usa deliberadamente para indicar **quién inicia la interacción**:

| Notación | Significado | Ejemplo en el diagrama |
|---|---|---|
| `Actor --> UC` | El actor **inicia** el caso de uso (actor primario) | `Administrador --> UC-01` — el administrador decide registrar un ingreso |
| `UC --> Actor` | El sistema **invoca** al actor durante el caso de uso (actor secundario) | `UC-10 --> API IA Multimodal` — el sistema llama a la API; la API nunca inicia nada |
| `UC ..> UC : <<include>>` | Inclusión obligatoria (siempre direccional en UML) | `UC-09 ..> UC-10` — capturar foto siempre desencadena la extracción |
| `UC ..> UC : <<extend>>` | Extensión opcional (siempre direccional en UML) | `UC-09 ..> UC-01` — la captura por foto es una vía alternativa de registro |

Con esta convención se distingue de un vistazo que el Administrador es el actor
primario de todos los casos de uso, mientras que la API IA es un actor
secundario que solo participa cuando el sistema la invoca (UC-10).

## Regenerar el diagrama

El diagrama de casos de uso es PlantUML embebido en `casos_de_uso.md`, renderizado
a SVG en `assets/`. Tras editar el bloque, ejecuta `scripts/render-diagrams.sh`
y commitea los cambios (el CI verifica que el SVG esté al día).
