# Historias de Usuario y Backlog Priorizado
### Sistema de Gestión Económica — Finca Ganadera
*Versión 1 · 9 de julio de 2026*

---

## Convenciones

- **Formato:** Como ‹rol› / Quiero ‹acción› / Para ‹beneficio›
- **Criterios de aceptación:** Gherkin en español (Dado / Cuando / Entonces)
- **Prioridad:** MoSCoW heredada del RF de origen (ver `docs/1-definicion-y-requisitos/definicion_proyecto_requisitos.md`)
- **Actor único:** "administrador" = el dueño/encargado de la finca, único usuario del sistema

---

## Historias de Usuario

### HU-01 — Registrar un ingreso (RF-01 · Must)

**Como** administrador de la finca,
**quiero** registrar un ingreso indicando fecha, categoría, monto, y opcionalmente nota y medio de pago,
**para** llevar un control digital de las entradas de dinero de la finca.

#### Criterios de aceptación

```gherkin
Escenario: Registro exitoso de un ingreso con campos obligatorios
  Dado que el administrador está en la pantalla principal
  Cuando presiona "Registrar ingreso"
  Entonces se muestra un formulario con los campos: fecha (predeterminada hoy),
    categoría de ingreso (lista desplegable), monto, nota (opcional) y
    medio de pago (opcional)

Escenario: Guardar un ingreso válido
  Dado que el administrador completó fecha, categoría y monto
  Cuando confirma el registro
  Entonces el ingreso se persiste localmente
    Y se muestra un mensaje de confirmación
    Y el balance se actualiza reflejando el nuevo ingreso

Escenario: Registro rápido en pocos pasos
  Dado que el administrador quiere registrar un ingreso rápidamente
  Cuando completa el flujo usando los valores predeterminados (fecha = hoy)
  Entonces el registro se completa en un máximo de 4 toques desde la pantalla principal

Escenario: Validación de campos obligatorios
  Dado que el administrador dejó el monto vacío
  Cuando intenta guardar
  Entonces se muestra un mensaje de error indicando que el monto es obligatorio
    Y el formulario no se envía

Escenario: Registro sin conexión a internet
  Dado que el dispositivo no tiene conexión a internet
  Cuando el administrador registra un ingreso
  Entonces el ingreso se guarda localmente sin error
    Y se sincroniza automáticamente cuando se restablezca la conexión
```

**RNF vinculados:** RNF-01 (máx. 3-4 toques), RNF-06 (sin pérdida de datos), RNF-07 (offline-first)

---

### HU-02 — Registrar un egreso (RF-02 · Must)

**Como** administrador de la finca,
**quiero** registrar un egreso indicando fecha, categoría, monto, y opcionalmente nota y medio de pago,
**para** llevar un control digital de las salidas de dinero de la finca.

#### Criterios de aceptación

```gherkin
Escenario: Registro exitoso de un egreso con campos obligatorios
  Dado que el administrador está en la pantalla principal
  Cuando presiona "Registrar egreso"
  Entonces se muestra un formulario con los campos: fecha (predeterminada hoy),
    categoría de egreso (lista desplegable), monto, nota (opcional) y
    medio de pago (opcional)

Escenario: Guardar un egreso válido
  Dado que el administrador completó fecha, categoría y monto
  Cuando confirma el registro
  Entonces el egreso se persiste localmente
    Y se muestra un mensaje de confirmación
    Y el balance se actualiza reflejando el nuevo egreso

Escenario: Registro rápido en pocos pasos
  Dado que el administrador quiere registrar un egreso rápidamente
  Cuando completa el flujo usando los valores predeterminados (fecha = hoy)
  Entonces el registro se completa en un máximo de 4 toques desde la pantalla principal

Escenario: Validación de campos obligatorios
  Dado que el administrador dejó el monto vacío
  Cuando intenta guardar
  Entonces se muestra un mensaje de error indicando que el monto es obligatorio
    Y el formulario no se envía

Escenario: Registro sin conexión a internet
  Dado que el dispositivo no tiene conexión a internet
  Cuando el administrador registra un egreso
  Entonces el egreso se guarda localmente sin error
    Y se sincroniza automáticamente cuando se restablezca la conexión
```

**RNF vinculados:** RNF-01 (máx. 3-4 toques), RNF-06 (sin pérdida de datos), RNF-07 (offline-first)

---

### HU-03 — Consultar balance general con comparación de periodos (RF-03 · Must)

**Como** administrador de la finca,
**quiero** ver el balance general (ingresos menos egresos) con comparación entre periodos y desglose por actividad,
**para** conocer el estado financiero de la finca en cualquier momento y detectar tendencias.

#### Criterios de aceptación

```gherkin
Escenario: Visualizar balance del periodo actual
  Dado que el administrador abre la pantalla de balance
  Cuando se carga la vista
  Entonces se muestra el total de ingresos, total de egresos y balance neto
    del mes en curso

Escenario: Comparar mes actual con mes anterior
  Dado que el administrador está viendo el balance del mes actual
  Cuando selecciona "comparar con mes anterior"
  Entonces se muestran ambos periodos lado a lado con sus totales
    Y se indica la variación (positiva o negativa) entre ambos

Escenario: Comparar año actual con año anterior
  Dado que el administrador quiere una visión anual
  Cuando selecciona comparación año vs. año
  Entonces se muestran los totales acumulados de ambos años
    Y se indica la variación entre ambos

Escenario: Desglose por actividad
  Dado que el administrador está viendo el balance de un periodo
  Cuando selecciona "desglose por actividad"
  Entonces se muestran los subtotales de ingresos y egresos separados
    por actividad (Lechería, Ganado, General)

Escenario: Balance disponible sin conexión
  Dado que el dispositivo no tiene conexión a internet
  Cuando el administrador consulta el balance
  Entonces se calcula y muestra el balance con los datos almacenados localmente
```

**RNF vinculados:** RNF-02 (consultable en cualquier momento), RNF-07 (offline-first)

---

### HU-04 — Categorías fijas de ingreso/egreso por actividad (RF-04 · Must)

**Como** administrador de la finca,
**quiero** que las transacciones se clasifiquen en categorías fijas agrupadas por actividad, con categorías reservadas que puedo activar cuando las necesite,
**para** organizar mis finanzas por línea de negocio sin crear categorías manualmente.

#### Criterios de aceptación

```gherkin
Escenario: Ver categorías activas al registrar una transacción
  Dado que el administrador está registrando un ingreso o egreso
  Cuando despliega la lista de categorías
  Entonces ve únicamente las categorías activas, agrupadas por actividad
    (Lechería, Ganado, o sin actividad para egresos generales)

Escenario: Categorías de ingreso preconfiguradas
  Dado que el sistema se instaló por primera vez
  Cuando el administrador registra un ingreso
  Entonces las categorías disponibles son: Venta de leche, Venta de cuajada,
    Venta de terneros, Venta de novillas, Venta de vacas (descarte),
    Venta de toros

Escenario: Categorías de egreso preconfiguradas
  Dado que el sistema se instaló por primera vez
  Cuando el administrador registra un egreso
  Entonces las categorías disponibles incluyen: Mano de obra — mayordomo,
    Mano de obra — jornaleros, Seguridad social del trabajador, Dotaciones,
    Medicamentos y vacunas, Sales mineralizadas, Concentrado,
    Asesorías eventuales, Mejoramiento de potreros, Servicios públicos,
    Impuesto predial, Mantenimiento de infraestructura

Escenario: Activar una categoría reservada
  Dado que el administrador necesita la categoría "Inseminación artificial / monta natural"
  Cuando accede a la configuración de categorías y la activa
  Entonces la categoría aparece disponible en el formulario de registro
    Y las demás categorías reservadas permanecen inactivas

Escenario: Las categorías reservadas inactivas no aparecen en el registro
  Dado que existen categorías reservadas inactivas
    (Inseminación artificial, Créditos o préstamos, Cuotas a asociaciones, Seguros)
  Cuando el administrador registra una transacción
  Entonces las categorías reservadas inactivas no aparecen en la lista
```

**Nota:** Las categorías y actividades están cerradas con el cliente (ver sección 8 del documento de requisitos). No se contempla creación libre de categorías por parte del usuario.

---

### HU-05 — Consultar historial de transacciones con filtros (RF-05 · Must)

**Como** administrador de la finca,
**quiero** consultar el historial de transacciones filtrado por fecha, categoría y/o actividad,
**para** encontrar registros específicos rápidamente.

#### Criterios de aceptación

```gherkin
Escenario: Ver historial completo
  Dado que el administrador abre la pantalla de historial
  Cuando se carga la vista
  Entonces se muestra la lista de transacciones del mes en curso,
    ordenadas de la más reciente a la más antigua,
    con fecha, categoría, tipo (ingreso/egreso) y monto visibles

Escenario: Filtrar por rango de fechas
  Dado que el administrador está en el historial
  Cuando selecciona un rango de fechas (fecha inicio y fecha fin)
  Entonces se muestran solo las transacciones dentro de ese rango

Escenario: Filtrar por categoría
  Dado que el administrador está en el historial
  Cuando selecciona una categoría específica (p. ej., "Venta de leche")
  Entonces se muestran solo las transacciones de esa categoría

Escenario: Filtrar por actividad
  Dado que el administrador quiere ver solo movimientos de Lechería
  Cuando selecciona la actividad "Lechería"
  Entonces se muestran todas las transacciones cuya categoría pertenece
    a la actividad Lechería

Escenario: Combinar filtros
  Dado que el administrador quiere ver egresos de Medicamentos en junio 2026
  Cuando selecciona la categoría "Medicamentos y vacunas"
    Y el rango de fechas 01/06/2026 – 30/06/2026
  Entonces se muestran solo las transacciones que cumplen ambos criterios

Escenario: Sin resultados
  Dado que el administrador aplica filtros que no coinciden con ningún registro
  Cuando se ejecuta la consulta
  Entonces se muestra un mensaje indicando que no hay transacciones
    para los filtros seleccionados
```

---

### HU-06 — Editar o eliminar un registro existente (RF-06 · Must)

**Como** administrador de la finca,
**quiero** poder editar o eliminar un registro de ingreso o egreso existente,
**para** corregir errores de captura o eliminar registros duplicados.

#### Criterios de aceptación

```gherkin
Escenario: Editar una transacción
  Dado que el administrador selecciona una transacción del historial
  Cuando elige "Editar"
  Entonces se abre el formulario con los datos precargados
    Y puede modificar cualquier campo (fecha, categoría, monto, nota, medio de pago)

Escenario: Guardar edición válida
  Dado que el administrador modificó los datos de una transacción
  Cuando confirma la edición
  Entonces los cambios se persisten
    Y el balance se recalcula
    Y se muestra un mensaje de confirmación

Escenario: Eliminar una transacción con confirmación
  Dado que el administrador selecciona una transacción del historial
  Cuando elige "Eliminar"
  Entonces se muestra un diálogo de confirmación antes de proceder

Escenario: Confirmar eliminación
  Dado que el administrador confirmó la eliminación
  Cuando acepta el diálogo
  Entonces la transacción se elimina permanentemente
    Y el balance se recalcula
    Y se muestra un mensaje de confirmación

Escenario: Cancelar eliminación
  Dado que se mostró el diálogo de confirmación de eliminación
  Cuando el administrador cancela
  Entonces la transacción permanece sin cambios
```

---

### HU-07 — Autenticación del administrador (RF-07 · Must)

**Como** administrador de la finca,
**quiero** proteger el acceso a la aplicación con usuario y contraseña,
**para** que nadie más pueda ver ni modificar mis registros financieros.

#### Criterios de aceptación

```gherkin
Escenario: Inicio de sesión exitoso
  Dado que el administrador abre la aplicación
  Cuando ingresa su usuario y contraseña correctos
  Entonces accede a la pantalla principal de la aplicación

Escenario: Credenciales incorrectas
  Dado que el administrador intenta iniciar sesión
  Cuando ingresa un usuario o contraseña incorrectos
  Entonces se muestra un mensaje de error genérico
    ("Credenciales incorrectas")
    Y no se permite el acceso

Escenario: Sesión persistente
  Dado que el administrador ya inició sesión previamente
  Cuando abre la aplicación de nuevo
  Entonces no necesita volver a autenticarse
    (la sesión se mantiene activa hasta que cierre sesión explícitamente)

Escenario: Cerrar sesión
  Dado que el administrador está autenticado
  Cuando selecciona "Cerrar sesión"
  Entonces se cierra la sesión
    Y se redirige a la pantalla de inicio de sesión
```

**RNF vinculados:** RNF-04 (datos cifrados, con respaldo)

---

### HU-08 — Exportar reportes en PDF o Excel (RF-08 · Should)

**Como** administrador de la finca,
**quiero** exportar el balance y los reportes de transacciones en formato PDF o Excel,
**para** compartir la información financiera con terceros (familiar, contador) sin darles acceso a la app.

#### Criterios de aceptación

```gherkin
Escenario: Exportar balance en PDF
  Dado que el administrador está viendo el balance de un periodo
  Cuando selecciona "Exportar" y elige formato PDF
  Entonces se genera un archivo PDF con el resumen de ingresos, egresos
    y balance neto del periodo, incluyendo desglose por actividad
    Y se ofrece compartirlo o guardarlo en el dispositivo

Escenario: Exportar balance en Excel
  Dado que el administrador está viendo el balance de un periodo
  Cuando selecciona "Exportar" y elige formato Excel
  Entonces se genera un archivo Excel con los mismos datos del balance
    Y se ofrece compartirlo o guardarlo en el dispositivo

Escenario: Exportar historial filtrado
  Dado que el administrador tiene un historial filtrado por fecha y/o categoría
  Cuando selecciona "Exportar"
  Entonces el archivo generado contiene solo las transacciones
    que coinciden con los filtros aplicados

Escenario: Exportar sin datos
  Dado que no hay transacciones en el periodo o filtro seleccionado
  Cuando intenta exportar
  Entonces se muestra un mensaje indicando que no hay datos para exportar
```

**Nota:** La exportación resuelve la necesidad de compartir con terceros sin implementar multiusuario (ver decisión D4).

---

### HU-09 — Capturar foto de anotación manuscrita (RF-09 · Could)

**Como** administrador de la finca,
**quiero** tomar una foto de mis anotaciones manuscritas del cuaderno directamente desde la app,
**para** iniciar el proceso de digitalización sin salir de la aplicación.

#### Criterios de aceptación

```gherkin
Escenario: Abrir cámara desde la app
  Dado que el administrador quiere digitalizar una anotación
  Cuando selecciona la opción "Registrar desde foto"
  Entonces se abre la cámara del dispositivo dentro de la app

Escenario: Captura exitosa
  Dado que el administrador apuntó la cámara a una anotación del cuaderno
  Cuando toma la foto
  Entonces se muestra una vista previa de la imagen capturada
    Y se ofrecen las opciones "Usar foto" y "Volver a tomar"

Escenario: Volver a tomar la foto
  Dado que la vista previa de la foto no es clara
  Cuando el administrador selecciona "Volver a tomar"
  Entonces se abre la cámara nuevamente para capturar otra foto
```

**Nota:** Esta historia es parte del épico de captura por IA (HU-09 → HU-10 → HU-11). Solo se implementa si el núcleo financiero (Must) está completo y estable.

---

### HU-10 — Extraer datos de la foto con IA (RF-10 · Could)

**Como** administrador de la finca,
**quiero** que la app extraiga automáticamente fecha, concepto y monto de la foto de mi anotación,
**para** no tener que transcribir manualmente cada dato.

#### Criterios de aceptación

```gherkin
Escenario: Extracción exitosa
  Dado que el administrador capturó una foto legible de una anotación
  Cuando confirma "Usar foto"
  Entonces la imagen se envía a la API de IA multimodal
    Y se muestra un indicador de carga
    Y al completarse, se prellenan los campos del formulario de registro
    (fecha, categoría sugerida, monto) con los datos extraídos

Escenario: Respuesta dentro de tiempo aceptable
  Dado que se envió la foto a la API
  Cuando la API responde
  Entonces la respuesta llega en segundos, no en minutos

Escenario: Error en la extracción (foto ilegible)
  Dado que la foto es de mala calidad o ilegible
  Cuando la API no puede extraer datos confiables
  Entonces se muestra un mensaje indicando que no se pudo leer la anotación
    Y se ofrece volver a tomar la foto o registrar manualmente

Escenario: Sin conexión a internet
  Dado que la extracción por IA requiere conexión
  Cuando el dispositivo no tiene conexión
  Entonces se informa al administrador que esta función requiere internet
    Y se ofrece guardar la foto para procesarla después
    o registrar manualmente
```

**RNF vinculados:** RNF-03 (respuesta en segundos)

**Restricción técnica:** Se usa exclusivamente una API de un modelo multimodal existente. No se entrena ni se ajusta (fine-tune) ningún modelo propio (decisión D2).

---

### HU-11 — Corregir datos extraídos antes de guardar (RF-11 · Could)

**Como** administrador de la finca,
**quiero** revisar y corregir los datos que la IA extrajo de la foto antes de que se guarden,
**para** asegurar que la información registrada sea correcta (human-in-the-loop).

#### Criterios de aceptación

```gherkin
Escenario: Revisión del formulario prellenado
  Dado que la IA extrajo datos de la foto y prellenó el formulario
  Cuando el administrador ve el formulario
  Entonces todos los campos prellenados son editables
    Y se muestra la foto original como referencia junto al formulario

Escenario: Corregir un dato incorrecto
  Dado que la IA sugirió un monto incorrecto
  Cuando el administrador corrige el monto manualmente
  Entonces el valor corregido reemplaza al sugerido

Escenario: Confirmar y guardar
  Dado que el administrador revisó y, si fue necesario, corrigió los datos
  Cuando confirma el registro
  Entonces la transacción se guarda con los datos finales (corregidos o no)
    Y se aplican las mismas validaciones que en un registro manual
    (HU-01 / HU-02)

Escenario: Descartar extracción
  Dado que el administrador no está conforme con la extracción
  Cuando selecciona "Descartar"
  Entonces se limpian los campos prellenados
    Y puede registrar la transacción manualmente desde cero
```

---

### HU-12 — Reportes visuales con gráficos (RF-12 · Could)

**Como** administrador de la finca,
**quiero** ver gráficos de ingresos y egresos por periodo,
**para** visualizar tendencias financieras de forma intuitiva.

#### Criterios de aceptación

```gherkin
Escenario: Gráfico de ingresos vs. egresos mensual
  Dado que el administrador abre la sección de reportes visuales
  Cuando selecciona la vista mensual
  Entonces se muestra un gráfico de barras comparando ingresos y egresos
    de cada mes

Escenario: Gráfico por actividad
  Dado que el administrador quiere ver la distribución por actividad
  Cuando selecciona "por actividad"
  Entonces se muestra un gráfico (circular o de barras) con la proporción
    de cada actividad sobre el total

Escenario: Periodo sin datos
  Dado que el periodo seleccionado no tiene transacciones
  Cuando se intenta renderizar el gráfico
  Entonces se muestra un mensaje indicando que no hay datos suficientes
    para generar el gráfico
```

---

## Backlog Priorizado

Las historias se organizan por prioridad MoSCoW. Dentro de cada nivel, el orden refleja dependencias lógicas y valor incremental.

### Must — Imprescindibles para el MVP

| Orden | Historia | RF | Justificación del orden |
|:---:|---|---|---|
| 1 | HU-07 — Autenticación | RF-07 | Prerrequisito: proteger el acceso antes de tener datos |
| 2 | HU-04 — Categorías fijas | RF-04 | Prerrequisito: las categorías deben existir antes de registrar transacciones |
| 3 | HU-01 — Registrar ingreso | RF-01 | Flujo central de la app |
| 4 | HU-02 — Registrar egreso | RF-02 | Flujo central de la app (simétrico a HU-01) |
| 5 | HU-05 — Historial con filtros | RF-05 | Necesario para consultar, editar o eliminar registros |
| 6 | HU-06 — Editar / eliminar registro | RF-06 | Depende de HU-05 para localizar el registro |
| 7 | HU-03 — Balance general | RF-03 | Requiere datos registrados (HU-01/02) para mostrar resultados útiles |

### Should — Importantes, se incluyen si el MVP está estable

| Orden | Historia | RF |
|:---:|---|---|
| 8 | HU-08 — Exportar reportes (PDF/Excel) | RF-08 |

### Could — Deseables, solo si sobra tiempo después del núcleo

| Orden | Historia | RF | Nota |
|:---:|---|---|---|
| 9 | HU-12 — Reportes visuales (gráficos) | RF-12 | Independiente del épico de IA |
| 10 | HU-09 — Capturar foto | RF-09 | Inicio del épico de IA — secuencial: 09 → 10 → 11 |
| 11 | HU-10 — Extraer datos con IA | RF-10 | Requiere HU-09 |
| 12 | HU-11 — Corrección manual post-IA | RF-11 | Requiere HU-10 |

---

## Sprints Tentativos (referencia)

> **Nota:** Esta distribución es orientativa. El cronograma formal se define en el Paso 8, una vez decidida la arquitectura (Paso 5) y el plan Scrum (Paso 7). Se asumen sprints de 1 semana dado el plazo corto (≈ 3–4 semanas útiles de desarrollo).

| Sprint | Historias | Enfoque |
|---|---|---|
| Sprint 1 | HU-07, HU-04, HU-01, HU-02 | Autenticación + categorías + registro de transacciones (núcleo) |
| Sprint 2 | HU-05, HU-06, HU-03 | Historial, edición/eliminación, balance — el MVP queda funcional |
| Sprint 3 | HU-08, HU-12 | Exportación de reportes + gráficos (consolidación) |
| Sprint 4 | HU-09, HU-10, HU-11 | Épico de captura por IA (solo si sprints 1-2 están estables) |

---

## Trazabilidad RF → HU

| RF | HU | Prioridad |
|---|---|---|
| RF-01 | HU-01 | Must |
| RF-02 | HU-02 | Must |
| RF-03 | HU-03 | Must |
| RF-04 | HU-04 | Must |
| RF-05 | HU-05 | Must |
| RF-06 | HU-06 | Must |
| RF-07 | HU-07 | Must |
| RF-08 | HU-08 | Should |
| RF-09 | HU-09 | Could |
| RF-10 | HU-10 | Could |
| RF-11 | HU-11 | Could |
| RF-12 | HU-12 | Could |
