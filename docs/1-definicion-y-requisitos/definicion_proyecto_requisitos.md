# Documento de Definición del Proyecto y Requisitos
### Sistema de Gestión Económica — Finca Ganadera
*Versión 1 · 9 de julio de 2026 · Vision & Scope + Especificación de Requisitos*

---

## 1. Contexto del Negocio

La finca es una explotación de ganadería de doble propósito enfocada en producción lechera, ubicada en Colombia. Actualmente cuenta con **25 cabezas de ganado**: 12 vacas paridas (en producción), 5 novillas de levante, 5 vacas secas y 1 toro. La operación incluye dos ordeños diarios y actividades comerciales asociadas a la venta de leche, cuajada ocasional, y venta de animales (terneros, novillas, vacas de descarte y toros).

Todo el seguimiento económico se lleva actualmente en dos cuadernos físicos: uno para el registro de los animales y otro —el relevante para este proyecto— para el control de dinero (entradas y salidas), organizado por mes, detalle, "Debe" y "Haber". Los registros históricos son inconsistentes, por lo que **no serán migrados**; el administrador se compromete a usar el nuevo sistema desde su implementación en adelante.

## 2. Definición del Problema

El administrador de la finca —único usuario del sistema— no cuenta con una forma rápida y confiable de conocer su balance financiero en un momento dado. El registro manual en papel dificulta consultar el estado económico de la finca, comparar periodos, o separar el desempeño de las distintas líneas de negocio (leche vs. venta de ganado).

## 3. Objetivos

### 3.1 Objetivo general
Proveer al administrador de la finca una herramienta digital simple y rápida para registrar ingresos y egresos, y consultar su balance financiero en cualquier momento, discriminado por periodo y por actividad económica.

### 3.2 Objetivos específicos
- Permitir el registro de una transacción (ingreso o egreso) en máximo 3-4 pasos.
- Calcular y mostrar el balance general consolidado y por actividad.
- Permitir comparar el desempeño financiero entre periodos.
- Garantizar que ningún dato se pierda, incluso sin conexión a internet.
- Explorar, como valor agregado, el uso de inteligencia artificial para reducir la fricción de captura de datos.

## 4. Alcance

### 4.1 Dentro del alcance
- Registro de ingresos y egresos por categoría
- Balance general con comparación de periodos y desglose por actividad
- Historial de transacciones filtrable
- Edición y eliminación de registros
- Autenticación simple del administrador
- Exportación de reportes (PDF/Excel)
- (Stretch, condicionado a tiempo disponible) Captura de foto de anotaciones manuscritas con extracción automática de datos vía IA

### 4.2 Fuera del alcance (por ahora)
- Gestión detallada del hato (partos, sanidad individual) — se mantiene en cuaderno aparte
- Inventario de insumos (concentrado, medicamentos) — rechazado explícitamente por el cliente
- Presupuestos o proyecciones financieras — no prioritario para el cliente
- Soporte multiusuario o roles adicionales — un único usuario administrador
- Entrenamiento de un modelo propio de reconocimiento de escritura — descartado por relación costo/beneficio (ver decisión D2)
- Migración del histórico de la cartera física — los datos existentes son inconsistentes
- Soporte para iOS — el despliegue inicial es exclusivamente Android

## 5. Restricciones y Supuestos

### 5.1 Restricciones
- **Tiempo:** entrega estimada entre el 27 de julio y el 10 de agosto de 2026 (2.5 a 4.5 semanas)
- **Equipo:** un solo desarrollador
- **Tecnología:** sin restricciones impuestas; se prioriza el uso de IA como valor agregado
- **Dispositivo objetivo:** gama media-baja, con cámara de calidad limitada

### 5.2 Supuestos
- El administrador cuenta con buena conectividad de datos/wifi en la finca, aunque el sistema debe operar también sin conexión.
- El administrador está dispuesto a adoptar una interfaz nueva, siempre que sea muy simple (íconos, botones grandes, texto mínimo).
- Un tercero (familiar o contador) podría eventualmente necesitar consultar la información, por lo que se prioriza la exportación de reportes sobre un modelo de permisos multiusuario.

## 6. Requisitos Funcionales

Priorizados con la técnica MoSCoW (Must / Should / Could).

| ID | Descripción | Prioridad |
|---|---|---|
| RF-01 | Registrar un ingreso (fecha, categoría, monto, nota y medio de pago opcionales) | Must |
| RF-02 | Registrar un egreso (fecha, categoría, monto, nota y medio de pago opcionales) | Must |
| RF-03 | Calcular y mostrar el balance general en cualquier momento, con comparación entre periodos (mes vs. mes anterior, año vs. año) y desglose por actividad | Must |
| RF-04 | Gestionar categorías fijas de ingreso/egreso agrupadas por actividad, incluyendo categorías reservadas inactivas listas para activarse | Must |
| RF-05 | Consultar historial de transacciones filtrado por fecha, categoría y/o actividad | Must |
| RF-06 | Editar o eliminar un registro existente | Must |
| RF-07 | Autenticación de acceso (usuario/contraseña) del administrador | Must |
| RF-08 | Exportar balance y reportes en PDF o Excel | Should |
| RF-09 | Capturar, desde el celular, una foto de una anotación manuscrita | Could |
| RF-10 | Enviar la foto a una API de IA multimodal existente (sin entrenamiento propio) para extraer fecha/concepto/monto y prellenar el formulario | Could |
| RF-11 | Permitir corregir manualmente el texto reconocido antes de guardar (human-in-the-loop) | Could |
| RF-12 | Reportes visuales (gráficos de ingresos/egresos por periodo) | Could |

## 7. Requisitos No Funcionales

| ID | Categoría | Descripción |
|---|---|---|
| RNF-01 | Usabilidad | Registrar una transacción manual en máx. 3-4 toques; interfaz con botones grandes e íconos, texto mínimo (usuario no técnico) |
| RNF-02 | Disponibilidad | El balance debe poder consultarse en cualquier momento |
| RNF-03 | Rendimiento | Si se implementa la extracción por foto, debe responder en segundos, no minutos |
| RNF-04 | Seguridad | Datos financieros cifrados, con respaldo (backup) periódico |
| RNF-05 | Plataforma | Cliente móvil nativo Android; iOS fuera de alcance por ahora (decisión de framework en fase de arquitectura) |
| RNF-06 | Confiabilidad | Ningún registro se pierde aunque falle la app |
| RNF-07 | Offline-first | Debe permitir registrar datos sin conexión y sincronizar cuando haya señal |

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
| D1 | Se corrige el tamaño del hato de 16 a 25 cabezas | La finca de referencia inicial no era la correcta; el cliente aclaró los datos reales en la entrevista |
| D2 | Se descarta el entrenamiento de un modelo propio de reconocimiento de escritura | La relación costo/beneficio no se justifica: no hay migración de histórico, el plazo es corto, y el ahorro de tiempo frente a la captura manual no está validado |
| D3 | No se migra el histórico de la cartera física | Los registros existentes son inconsistentes; el administrador usará el sistema desde su implementación en adelante |
| D4 | Un solo usuario autenticado, con exportación de reportes en vez de multiusuario | Resuelve la necesidad de compartir información con terceros sin la complejidad de un modelo de permisos |
| D5 | Cliente móvil nativo Android; iOS fuera de alcance por ahora | No hay necesidad actual de iOS; se define framework (nativo vs. multiplataforma) en la fase de arquitectura |

## 10. Próximos Pasos

Con este documento cerrado, el siguiente paso del proceso es la construcción de las historias de usuario y el backlog priorizado a partir de los requisitos funcionales aquí definidos, seguido de los diagramas de casos de uso y clases, la arquitectura de software, el modelo de datos, el plan Scrum, el cronograma y el plan de pruebas.
