# Diagramas de Actividad
### Sistema de Gestión Económica — Finca Ganadera
*Versión 2 · 20 de agosto de 2026*

---

> Los diagramas muestran decisiones y caminos alternativos de los procesos de
> negocio. Usan `flowchart` de Mermaid como notación equivalente a actividades.

## AD-01 — Registrar ingreso o egreso

Cubre UC-01 y UC-02.

Resume las decisiones que se toman al registrar un movimiento financiero. El
usuario selecciona el tipo, completa los datos y corrige los campos inválidos
antes de iniciar el guardado local y la sincronización.

> **Nota:** si no hay conexión, la operación no se pierde; permanece pendiente
> y se reintenta cuando el dispositivo recupera la red.

```mermaid
flowchart TD
    A([Inicio]) --> B{¿Ingreso o egreso?}
    B -->|Ingreso| C[Mostrar categorías de ingreso]
    B -->|Egreso| D[Mostrar categorías de egreso]
    C --> E[Completar fecha, categoría y monto]
    D --> E
    E --> F{¿Datos válidos?}
    F -->|No| G[Mostrar errores]
    G --> E
    F -->|Sí| H[Guardar en Room como pendiente]
    H --> I[Enviar escritura al SDK de Firestore]
    I --> J{¿Confirmación remota?}
    J -->|Sí| K[Marcar como sincronizada]
    J -->|No| L[Mantener escritura pendiente]
    L --> M[Reintentar al recuperar conexión]
    M --> K
    K --> N([Transacción registrada])
```

---

## AD-02 — Registrarse y asociarse a una finca

Cubre UC-13 y parte de UC-14.

Muestra el proceso de creación de una cuenta y la asociación del usuario con
una finca. El flujo cambia según el usuario cree una finca o ingrese un código
para unirse a una existente.

> **Nota:** una invitación solo permite crear la membresía cuando existe, sigue
> vigente y se encuentra activa.

```mermaid
flowchart TD
    A([Inicio]) --> B[Completar nombre, email y contraseña]
    B --> C[Crear cuenta con Firebase Auth]
    C --> D{¿Registro válido?}
    D -->|No| E[Mostrar error]
    E --> B
    D -->|Sí| F{¿Crear finca o unirse?}
    F -->|Crear finca| G[Ingresar datos de la finca]
    G --> H[Crear Farm]
    H --> I[Crear FarmMembership con rol OWNER]
    F -->|Unirse| J[Ingresar código de invitación]
    J --> K[Consultar FarmInvitation]
    K --> L{¿Existe, está vigente y activa?}
    L -->|No| M[Mostrar error]
    M --> J
    L -->|Sí| N[Crear FarmMembership con rol WORKER]
    I --> O[Guardar finca activa en contexto local]
    N --> O
    O --> P([Abrir pantalla principal])
```

---

## AD-03 — Realizar recorrido de inspección

Cubre UC-15, UC-16 y UC-17.

Describe las actividades necesarias para comenzar, mantener y finalizar una
inspección. Durante el recorrido se registran puntos GPS y el rumbo del
dispositivo, y opcionalmente se pueden registrar animales.

> **Nota:** la falta de permiso impide iniciar el recorrido. Una pérdida
> temporal de señal durante un recorrido activo muestra una advertencia y el
> seguimiento continúa cuando la señal regresa.

```mermaid
flowchart TD
    A([Inicio]) --> B[Seleccionar nuevo recorrido]
    B --> C{¿Permiso de ubicación concedido?}
    C -->|No| D[Solicitar permiso]
    D --> E{¿Usuario concede permiso?}
    E -->|No| F[Informar que GPS es obligatorio]
    F --> Z([Fin sin iniciar])
    E -->|Sí| G
    C -->|Sí| G{¿GPS habilitado?}
    G -->|No| H[Solicitar activar GPS]
    H --> G
    G -->|Sí| I[Mostrar posición y botón Iniciar]
    I --> J[Crear recorrido en progreso]
    J --> K[Iniciar GPS y magnetómetro]
    K --> L[Registrar punto GPS]
    L --> M[Obtener rumbo de la brújula]
    M --> N[Actualizar trazado, distancia, tiempo y orientación]
    N --> O{¿GPS disponible?}
    O -->|No| P[Mostrar Sin señal y esperar]
    P --> O
    O -->|Sí| Q{¿Registrar animal?}
    Q -->|Sí| R[Ejecutar AD-04]
    R --> S{¿Finalizar recorrido?}
    Q -->|No| S
    S -->|No| L
    S -->|Sí| T[Detener GPS y magnetómetro]
    T --> U[Calcular distancia y duración]
    U --> V[Guardar recorrido completado]
    V --> W[Mostrar resumen]
    W --> X([Fin])
```

---

## AD-04 — Registrar animal con foto y ubicación opcional

Cubre UC-20 y aporta datos a UC-21.

Representa las alternativas para obtener una fotografía y asociarle una
ubicación. Después de validar el formulario, la imagen se conserva localmente y
se sincroniza junto con los datos del animal.

> **Nota:** las imágenes de galería pueden usar coordenadas EXIF, la ubicación
> actual o ninguna. Nominatim solo se consulta cuando existen coordenadas y hay
> conexión.

```mermaid
flowchart TD
    A([Inicio]) --> B{¿Cámara o galería?}
    B -->|Cámara| C[Tomar fotografía]
    C --> D[Intentar obtener ubicación actual]
    B -->|Galería| E[Seleccionar imagen]
    E --> F{¿La imagen tiene GPS EXIF?}
    F -->|Sí| G{¿Qué ubicación usar?}
    F -->|No| H{¿Usar ubicación actual?}
    G -->|EXIF| I[Usar ubicación de la foto]
    G -->|Actual| D
    G -->|Ninguna| J[Continuar sin ubicación]
    H -->|Sí| D
    H -->|No| J
    D --> K{¿GPS disponible?}
    K -->|Sí| L[Guardar coordenadas actuales]
    K -->|No| J
    I --> M{¿Hay conexión?}
    L --> M
    M -->|Sí| N[Resolver nombre con Nominatim]
    M -->|No| O[Marcar geocodificación pendiente]
    J --> P[Mostrar formulario sin ubicación]
    N --> Q[Mostrar formulario con ubicación]
    O --> Q
    P --> R[Completar nombre y notas]
    Q --> R
    R --> S{¿Datos válidos?}
    S -->|No| T[Mostrar errores]
    T --> R
    S -->|Sí| U[Guardar URI local y estado pendiente]
    U --> V{¿Hay conexión?}
    V -->|Sí| W[Subir foto a Firebase Storage]
    W --> X[Guardar URL y datos en Firestore]
    V -->|No| Y[Dejar carga y sincronización pendientes]
    X --> Z([Animal registrado])
    Y --> Z
```

---

## AD-05 — Registrar transacción mediante OCR

Cubre UC-09, UC-10 y UC-11.

Muestra el proceso de reconocimiento de una anotación fotografiada. El sistema
intenta extraer campos estructurados, pero mantiene caminos alternativos para
repetir la captura o completar manualmente el formulario.

> **Nota:** el OCR ayuda a diligenciar los datos; la confirmación final siempre
> corresponde al usuario.

```mermaid
flowchart TD
    A([Inicio]) --> B{¿Cámara o galería?}
    B -->|Cámara| C[Tomar foto de la anotación]
    B -->|Galería| D[Seleccionar imagen]
    C --> E[Mostrar vista previa]
    D --> E
    E --> F{¿Usar imagen?}
    F -->|No| B
    F -->|Sí| G[Procesar con ML Kit]
    G --> H{¿Se reconoció texto?}
    H -->|No| I{¿Reintentar o registrar manualmente?}
    I -->|Reintentar| B
    I -->|Manual| J[Abrir formulario vacío]
    H -->|Sí| K[Extraer fecha, concepto y monto]
    K --> L{¿Se obtuvieron campos estructurados?}
    L -->|No| M[Mostrar texto crudo]
    L -->|Sí| N[Prellenar formulario]
    M --> O[Completar campos manualmente]
    N --> O[Revisar y corregir]
    J --> O
    O --> P{¿Datos válidos?}
    P -->|No| Q[Mostrar errores]
    Q --> O
    P -->|Sí| R[Guardar y sincronizar transacción]
    R --> S([Fin])
```

---

## AD-06 — Crear, editar, completar o eliminar recordatorio

Cubre el flujo principal de UC-19.

Reúne las operaciones sobre recordatorios: crear, editar, completar y eliminar.
La programación local se crea o cancela de acuerdo con el estado guardado por
el usuario.

> **Nota:** completar y eliminar cancelan la notificación futura antes de
> sincronizar el nuevo estado con Firestore.

```mermaid
flowchart TD
    A([Inicio]) --> B[Abrir recordatorios]
    B --> C{¿Nueva operación?}
    C -->|Crear| D[Completar título, fecha y repetición]
    C -->|Existente| E[Seleccionar recordatorio]
    E --> F{¿Editar, completar o eliminar?}
    F -->|Editar| D
    F -->|Completar| G[Cancelar notificación programada]
    G --> H[Guardar estado COMPLETED y completedAt]
    F -->|Eliminar| I[Cancelar notificación programada]
    I --> J[Guardar estado DELETED]
    D --> K{¿Datos válidos?}
    K -->|No| L[Mostrar errores]
    L --> D
    K -->|Sí| M[Guardar estado SCHEDULED]
    M --> N[Programar notificación local]
    H --> O[Sincronizar con Firestore]
    J --> O
    N --> O
    O --> P([Fin])
```

---

## AD-07 — Enviar notificación push al propietario

Cubre el flujo alternativo de UC-19 cuando un trabajador registra una
transacción.

Muestra las actividades del envío automático de una notificación al
propietario. Cloud Functions identifica los destinatarios de la finca y FCM
entrega el mensaje a sus dispositivos.

> **Nota:** si no existen tokens válidos, el flujo termina sin envío. Los tokens
> rechazados pueden depurarse para evitar intentos posteriores innecesarios.

```mermaid
flowchart TD
    A([Trabajador registra transacción]) --> B[Guardar documento en Firestore]
    B --> C[Activar Cloud Function]
    C --> D[Consultar membresías OWNER activas de la finca]
    D --> E{¿Hay destinatarios con token válido?}
    E -->|No| F[Finalizar sin envío]
    E -->|Sí| G[Enviar mensaje mediante FCM]
    G --> H{¿Entrega aceptada?}
    H -->|No| I[Registrar error y depurar token inválido]
    H -->|Sí| J[FirebaseMessagingService recibe mensaje]
    J --> K[Mostrar notificación al propietario]
    F --> L([Fin])
    I --> L
    K --> L
```
