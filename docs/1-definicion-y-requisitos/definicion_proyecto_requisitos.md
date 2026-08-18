# Documento de Definición del Proyecto y Requisitos
### Sistema de Gestión Económica — Finca Ganadera
*Versión 2 · 11 de agosto de 2026 · Pivote multi-usuario, Firebase, GPS, sensores*

---

## 1. Contexto del Negocio

La finca es una explotación de ganadería de doble propósito enfocada en producción lechera, ubicada en Colombia. Actualmente cuenta con **25 cabezas de ganado**: 12 vacas paridas (en producción), 5 novillas de levante, 5 vacas secas y 1 toro. La operación incluye dos ordeños diarios y actividades comerciales asociadas a la venta de leche, cuajada ocasional, y venta de animales (terneros, novillas, vacas de descarte y toros).

Todo el seguimiento económico se lleva actualmente en dos cuadernos físicos: uno para el registro de los animales y otro —el relevante para este proyecto— para el control de dinero (entradas y salidas), organizado por mes, detalle, "Debe" y "Haber". Los registros históricos son inconsistentes, por lo que **no serán migrados**; el equipo de la finca usará el nuevo sistema desde su implementación en adelante.

## 2. Definición del Problema

El propietario de la finca y sus trabajadores de campo no cuentan con una forma rápida y confiable de conocer el balance financiero en un momento dado. El registro manual en papel dificulta consultar el estado económico de la finca, comparar periodos, o separar el desempeño de las distintas líneas de negocio (leche vs. venta de ganado). No hay coordinación digital entre propietario y trabajadores: las transacciones que ocurren en campo se comunican verbalmente y se registran tarde o se pierden. Tampoco existe un registro geolocalizado del ganado ni de las inspecciones de campo.

## 3. Objetivos

### 3.1 Objetivo general
Proveer al propietario de la finca y a sus trabajadores una herramienta digital multi-usuario para registrar ingresos y egresos, consultar el balance financiero, rastrear inspecciones de campo y registrar el ganado con fotos geolocalizadas.

### 3.2 Objetivos específicos
- Permitir el registro de una transacción (ingreso o egreso) en máximo 3-4 pasos.
- Calcular y mostrar el balance general consolidado y por actividad.
- Permitir comparar el desempeño financiero entre periodos.
- Soportar múltiples usuarios con roles diferenciados (propietario y trabajador).
- Garantizar que ningún dato se pierda, incluso sin conexión a internet (offline-first con sincronización vía Firebase).
- Permitir rastrear recorridos de inspección con GPS y registrar animales con foto y ubicación.
- Explorar, como valor agregado, el uso de OCR on-device para reducir la fricción de captura de datos.

## 4. Alcance

### 4.1 Dentro del alcance
- Registro de ingresos y egresos por categoría (multi-usuario)
- Balance general con comparación de periodos y desglose por actividad
- Historial de transacciones filtrable
- Edición y eliminación de registros (propietario)
- Autenticación multi-usuario con Firebase Auth (propietario + trabajadores)
- Exportación de reportes (PDF/Excel)
- Recorridos de inspección con rastreo GPS en tiempo real
- Registro de animales con foto geolocalizada y visualización en mapa
- Directorio de contactos de la finca (importable desde el dispositivo)
- Recordatorios con notificaciones locales y push
- Uso de sensores del dispositivo (acelerómetro y proximidad)
- (Stretch, condicionado a tiempo disponible) Captura de foto de anotaciones manuscritas con extracción de texto vía OCR on-device

### 4.2 Fuera del alcance (por ahora)
- Gestión detallada del hato (partos, sanidad individual) — se mantiene en cuaderno aparte
- Inventario de insumos (concentrado, medicamentos) — rechazado explícitamente por el cliente
- Presupuestos o proyecciones financieras — no prioritario para el cliente
- Entrenamiento de un modelo propio de reconocimiento de escritura — descartado por relación costo/beneficio (ver decisión D2)
- Migración del histórico de la cartera física — los datos existentes son inconsistentes
- Soporte para iOS — el despliegue inicial es exclusivamente Android (si el curso introduce Flutter/KMP, se revisará; ver ADR-001)

## 5. Restricciones y Supuestos

### 5.1 Restricciones
- **Tiempo:** semestre académico 2026-3. Entrega 1: 22 de septiembre; Entrega 2: 20 de octubre; Entrega final: 24 de noviembre de 2026.
- **Equipo:** 4 desarrolladores (proyecto académico, metodología Scrum)
- **Plataforma:** Android nativo (Kotlin + Jetpack Compose) — requisito del curso
- **Backend:** Firebase (Auth + Firestore + Storage + FCM) — requisito del curso
- **Hardware obligatorio:** cámara, galería, almacenamiento, contactos, 2 sensores ≠ luz ambiental, GPS + mapas
- **API REST externa:** al menos una API REST diferente a clima (Nominatim para geocoding inverso)
- **Dispositivo objetivo:** gama media-baja, con cámara de calidad limitada
- **Presupuesto para APIs:** $0 — no hay presupuesto para tokens de IA; se usa OCR on-device (ML Kit) en lugar de API multimodal

### 5.2 Supuestos
- El propietario y los trabajadores cuentan con smartphones Android (gama media-baja como mínimo).
- La finca tiene conectividad intermitente; el sistema debe operar offline y sincronizar cuando haya señal.
- Los usuarios están dispuestos a adoptar una interfaz nueva, siempre que sea muy simple (íconos, botones grandes, texto mínimo).
- El propietario es quien invita a los trabajadores a la finca mediante un código de invitación.

## 6. Requisitos Funcionales

Priorizados con la técnica MoSCoW (Must / Should / Could).

### 6.1 Requisitos originales (validados con el cliente)

| ID | Descripción | Prioridad |
|---|---|---|
| RF-01 | Registrar un ingreso (fecha, categoría, monto, nota y medio de pago opcionales) | Must |
| RF-02 | Registrar un egreso (fecha, categoría, monto, nota y medio de pago opcionales) | Must |
| RF-03 | Calcular y mostrar el balance general en cualquier momento, con comparación entre periodos y desglose por actividad | Must |
| RF-04 | Gestionar categorías fijas de ingreso/egreso agrupadas por actividad, incluyendo categorías reservadas activables | Must |
| RF-05 | Consultar historial de transacciones filtrado por fecha, categoría y/o actividad | Must |
| RF-06 | Editar o eliminar un registro existente | Must |
| RF-07 | Autenticación de acceso multi-usuario con Firebase Auth | Must |
| RF-08 | Exportar balance y reportes en PDF o Excel | Should |
| RF-09 | Capturar, desde el celular, una foto de una anotación manuscrita | Could |
| RF-10 | Procesar la foto con OCR on-device (ML Kit) para extraer texto y prellenar el formulario | Could |
| RF-11 | Permitir corregir manualmente el texto reconocido antes de guardar (human-in-the-loop) | Could |
| RF-12 | Reportes visuales (gráficos de ingresos/egresos por periodo) | Could |

### 6.2 Requisitos nuevos (requisitos del curso)

| ID | Descripción | Prioridad |
|---|---|---|
| RF-13 | Registrarse como nuevo usuario y crear una finca o unirse a una existente con código de invitación | Must |
| RF-14 | Gestionar trabajadores: generar/revocar códigos de invitación, activar/desactivar trabajadores | Must |
| RF-15 | Iniciar un recorrido de inspección con rastreo GPS en tiempo real | Must |
| RF-16 | Visualizar el recorrido de inspección actual en el mapa en tiempo real | Must |
| RF-17 | Consultar el historial de recorridos de inspección con visualización en mapa | Should |
| RF-18 | Gestionar un directorio de contactos de la finca, con importación desde contactos del dispositivo | Should |
| RF-19 | Configurar recordatorios con notificaciones locales y push (vacunación, pagos, ordeño) | Should |
| RF-20 | Registrar un animal con foto, nombre, notas y ubicación GPS; visualizar animales en el mapa | Must |
| RF-21 | Consultar el mapa de animales registrados con sus fotos y ubicaciones | Must |

## 7. Requisitos No Funcionales

| ID | Categoría | Descripción |
|---|---|---|
| RNF-01 | Usabilidad | Registrar una transacción manual en máx. 3-4 toques; interfaz con botones grandes e íconos, texto mínimo (usuario no técnico) |
| RNF-02 | Disponibilidad | El balance debe poder consultarse en cualquier momento |
| RNF-03 | Rendimiento | Si se implementa la extracción por OCR, debe responder en segundos (on-device, sin latencia de red) |
| RNF-04 | Seguridad | Datos financieros protegidos por Firebase Auth; comunicación cifrada (HTTPS) |
| RNF-05 | Plataforma | Cliente móvil nativo Android (Kotlin + Jetpack Compose); iOS fuera de alcance (ver ADR-001) |
| RNF-06 | Confiabilidad | Ningún registro se pierde aunque falle la app (Room como caché local) |
| RNF-07 | Offline-first | Debe permitir registrar datos sin conexión y sincronizar con Firestore cuando haya señal |
| RNF-08 | Multi-usuario | Roles diferenciados: propietario (acceso completo) y trabajador (acceso operativo) |
| RNF-09 | Hardware | Usar acelerómetro (shake-to-register) y sensor de proximidad (auto-dim durante GPS) |
| RNF-10 | GPS | Rastreo de posición en tiempo real durante inspecciones; geolocalización de fotos de animales |

## 8. Categorías Económicas (detalle de RF-04)

### 8.1 Ingresos

| Categoría | Actividad |
|---|---|
| Venta de leche | Lechería |
| Venta de cuajada (ocasional) | Lechería |
| Venta de terneros | Ganado |
| Venta de novillas | Ganado |
| Venta de vacas (descarte) | Ganado |
| Venta de toros | Ganado |

### 8.2 Egresos
- Mano de obra — mayordomo (salario fijo mensual)
- Mano de obra — jornaleros (pago por día, ocasional)
- Seguridad social del trabajador
- Dotaciones
- Medicamentos y vacunas
- Sales mineralizadas
- Concentrado
- Asesorías eventuales
- Mejoramiento de potreros (fertilizantes y semillas)
- Servicios públicos (agua/concesión, luz)
- Impuesto predial
- Mantenimiento de infraestructura (cercas, corrales, sala de ordeño, maquinaria)

### 8.3 Categorías reservadas (inactivas, disponibles para activar sin fricción)
- Inseminación artificial / monta natural
- Créditos o préstamos
- Cuotas a asociaciones ganaderas
- Seguros

## 9. Decisiones Clave del Proyecto

| ID | Decisión | Justificación |
|---|---|---|
| D1 | Se corrige el tamaño del hato de 16 a 25 cabezas | La finca de referencia inicial no era la correcta; el cliente aclaró los datos reales |
| D2 | Se descarta el entrenamiento de un modelo propio de reconocimiento de escritura | La relación costo/beneficio no se justifica |
| D3 | No se migra el histórico de la cartera física | Los registros existentes son inconsistentes |
| ~~D4~~ | ~~Un solo usuario autenticado~~ → **Multi-usuario con Firebase Auth** | Pivote: el curso requiere multi-usuario. Roles: propietario (acceso completo) + trabajador (acceso operativo). El propietario invita trabajadores con código |
| D5 | Cliente móvil nativo Android; iOS fuera de alcance por ahora | Se define framework en ADR-001. Si el curso introduce Flutter/KMP, se revisará |
| D6 | OCR on-device (ML Kit) en lugar de API IA multimodal | Sin presupuesto para tokens de IA. ML Kit es gratuito, funciona offline, y cubre el caso de uso |
| D7 | Firebase como backend (Auth + Firestore + Storage + FCM) en lugar de Google Drive | Requisito del curso. Reemplaza la estrategia local-only + backup a Google Drive. Firestore maneja sincronización online/offline nativamente |
| D8 | Nominatim como API REST externa (geocoding inverso) | Gratuito, convierte coordenadas GPS a nombres de lugar legibles. Cumple requisito del curso (API REST ≠ clima) sin costo |
| D9 | GPS para inspecciones de campo y registro de animales, no para rutas de entrega | El cliente es productor lechero, no distribuidor. Las rutas de entrega no aportan valor. El GPS se usa para recorridos de inspección y geolocalización de fotos de animales |

## 10. Próximos Pasos

Con este documento actualizado, los siguientes pasos son:
1. Actualizar historias de usuario y backlog con los nuevos requisitos (RF-13 a RF-21)
2. Revisar y actualizar ADRs (002 y 003) para reflejar Firebase
3. Actualizar diagramas de casos de uso y clases
4. Continuar con el modelo de datos (Paso 6)
5. Planificar sprints con el equipo de 4 desarrolladores según el cronograma del semestre
