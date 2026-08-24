# Diagramas de Secuencia
### Sistema de Gestión Económica — Finca Ganadera
*Versión 4 · 20 de agosto de 2026*

---

> Diagramas alineados con MVVM + Clean Architecture ligera, Room, Firebase y
> los casos de uso vigentes. Cada sección explica el propósito del flujo y las
> responsabilidades principales de sus participantes.

## SD-01 — Registrar transacción y sincronizar

Cubre UC-01 y UC-02 (RF-01, RF-02, RNF-07).

Representa el registro de un ingreso o egreso desde la carga de categorías
hasta su almacenamiento y sincronización. La transacción se guarda primero en
Room para que la operación funcione sin conexión; el repositorio usa el SDK de
Firestore para enviarla cuando la red está disponible.

> **Nota:** el estado `PENDING` indica que el dato ya existe localmente, pero la
> confirmación remota todavía no se ha recibido. Al llegar el snapshot de
> Firestore, el registro cambia a `SYNCED`.

```mermaid
sequenceDiagram
    actor Usuario
    participant UI as RegisterScreen
    participant VM as RegisterViewModel
    participant Repo as TransactionRepository
    participant Data as Room + Firestore SDK

    Usuario->>UI: Selecciona ingreso o egreso
    UI->>VM: loadForm(type)
    VM->>Repo: getActiveCategories(type, farmId)
    Repo->>Data: Consultar categorías en Room
    Data-->>Repo: categories
    Repo-->>VM: categories
    Usuario->>UI: Completa datos y confirma
    UI->>VM: save(formData)
    VM->>VM: validate(formData)
    alt Datos inválidos
        VM-->>UI: UiState.error
        UI-->>Usuario: Muestra campos por corregir
    else Datos válidos
        VM->>Repo: create(transaction, membership)
        Repo->>Data: INSERT Room con estado PENDING
        Repo->>Data: set() mediante Firestore SDK
        Repo-->>VM: Guardado local iniciado
        VM-->>UI: UiState.success(pendingSync)
        alt Confirmación remota
            Data-->>Repo: Snapshot confirmado
            Repo->>Data: UPDATE Room a SYNCED
        else Sin conexión
            Note over Repo,Data: El SDK conserva la escritura pendiente<br/>y reintenta al recuperar conexión
        end
    end
```

---

## SD-02 — Consultar balance y comparar periodos

Cubre UC-03 (RF-03).

Muestra cómo el propietario consulta el balance de la finca, compara dos
periodos o solicita un desglose por actividad. El `CalculateBalanceUseCase`
concentra los cálculos para mantenerlos separados de la interfaz.

> **Nota:** el repositorio consulta la proyección local de Room, lo que permite
> mostrar resultados incluso cuando el dispositivo no tiene conexión.

```mermaid
sequenceDiagram
    actor Propietario
    participant UI as BalanceScreen
    participant VM as BalanceViewModel
    participant UC as CalculateBalanceUseCase
    participant Repo as TransactionRepository

    Propietario->>UI: Abre balance
    UI->>VM: loadBalance(currentPeriod, farmId)
    VM->>UC: calculate(currentPeriod, farmId)
    UC->>Repo: getByPeriod(currentPeriod, farmId)
    Note over Repo: Consulta la proyección local de Room
    Repo-->>UC: currentTransactions
    UC->>UC: Calcular ingresos, egresos y neto
    UC-->>VM: Balance
    VM-->>UI: UiState(balance)
    alt Comparar periodos
        Propietario->>UI: Selecciona periodo anterior
        UI->>VM: compare(currentPeriod, previousPeriod)
        VM->>UC: compare(currentPeriod, previousPeriod, farmId)
        UC->>Repo: getByPeriods(periods, farmId)
        Repo-->>UC: transactionsByPeriod
        UC-->>VM: BalanceComparison
        VM-->>UI: UiState(comparison)
    else Desglosar por actividad
        Propietario->>UI: Selecciona desglose
        UI->>VM: breakdown(period, farmId)
        VM->>UC: calculateByActivity(period, farmId)
        UC->>Repo: getTotalsByActivity(period, farmId)
        Repo-->>UC: activityTotals
        UC-->>VM: ActivityBreakdown
        VM-->>UI: UiState(breakdown)
    end
```

---

## SD-03 — Editar o eliminar una transacción

Cubre UC-05 y UC-06 (RF-05, RF-06).

Describe las dos operaciones disponibles desde el historial: editar una
transacción o eliminarla después de confirmar la acción. En ambos casos, el
repositorio actualiza primero la información local y coordina el cambio remoto.

> **Nota:** la eliminación requiere confirmación para evitar pérdidas por una
> selección accidental. La lista se actualiza después de reconciliar los datos.

```mermaid
sequenceDiagram
    actor Propietario
    participant UI as HistoryScreen
    participant VM as HistoryViewModel
    participant Repo as TransactionRepository
    participant Data as Room + Firestore SDK

    Propietario->>UI: Selecciona una transacción
    UI->>VM: loadTransaction(id, farmId)
    VM->>Repo: getById(id, farmId)
    Repo->>Data: SELECT en Room
    Data-->>Repo: transaction
    Repo-->>VM: transaction
    alt Editar
        Propietario->>UI: Modifica y confirma
        UI->>VM: update(id, formData)
        VM->>VM: validate(formData)
        VM->>Repo: update(transaction)
        Repo->>Data: UPDATE Room a PENDING y Firestore
    else Eliminar
        Propietario->>UI: Selecciona eliminar
        UI-->>Propietario: Solicita confirmación
        alt Confirma
            Propietario->>UI: Confirmar
            UI->>VM: delete(id)
            VM->>Repo: delete(id, farmId)
            Repo->>Data: DELETE lógico/local y remoto
        else Cancela
            UI-->>Propietario: Mantiene la transacción
        end
    end
    opt Operación enviada
        Data-->>Repo: Snapshot remoto actualizado
        Repo->>Data: Reconciliar proyección local
        Repo-->>VM: Resultado
        VM-->>UI: Lista actualizada
    end
```

---

## SD-04 — Registrarse y crear o unirse a una finca

Cubre UC-13 y parte de UC-14 (RF-13, RF-14).

Representa el registro de una cuenta y las dos formas de obtener acceso a una
finca: crear una nueva como propietario o unirse como trabajador mediante un
código de invitación.

> **Nota:** `FarmMembership` relaciona al usuario con una finca y almacena el
> rol que tiene dentro de ella. La invitación se valida por existencia,
> vigencia y estado antes de crear la membresía.

```mermaid
sequenceDiagram
    actor Usuario
    participant UI as RegisterScreen
    participant VM as RegisterViewModel
    participant Auth as Firebase Auth
    participant Repo as MembershipRepository

    Usuario->>UI: Completa nombre, email y contraseña
    UI->>VM: register(credentials)
    VM->>Auth: createUserWithEmailAndPassword()
    alt Credenciales inválidas o email existente
        Auth-->>VM: Error de registro
        VM-->>UI: Muestra error
    else Cuenta creada
        Auth-->>VM: uid
        VM-->>UI: Solicita crear o unirse a finca
        alt Crear finca
            Usuario->>UI: Ingresa datos de la finca
            UI->>VM: createFarm(farmData)
            VM->>Repo: createFarmAndMembership(uid, OWNER)
            Note over Repo: Transacción Firestore:<br/>Farm + FarmMembership
        else Unirse con código
            Usuario->>UI: Ingresa código
            UI->>VM: joinFarm(code)
            VM->>Repo: validateInvitation(code, now)
            alt Código inexistente, vencido o revocado
                Repo-->>VM: InvitationError
                VM-->>UI: Muestra error
            else Código válido
                Repo-->>VM: farmId
                VM->>Repo: createMembership(uid, farmId, WORKER)
            end
        end
        Repo-->>VM: Membership activa
        VM->>Repo: cacheActiveFarmContext()
        VM-->>UI: Navegar a inicio
    end
```

---

## SD-05 — Recorrido de inspección con GPS y brújula

Cubre UC-15, UC-16 y UC-17 (RF-15, RF-16, RF-17).

Muestra el ciclo completo de una inspección: validación del GPS, creación del
recorrido, captura periódica de puntos, visualización de la orientación y cierre
con el cálculo de distancia y duración.

> **Nota:** el GPS proporciona la posición y el magnetómetro proporciona el
> rumbo de la brújula. Si la señal GPS se pierde, la aplicación informa al
> usuario y continúa cuando vuelve a estar disponible.

```mermaid
sequenceDiagram
    actor Usuario
    participant UI as InspectionScreen
    participant VM as InspectionViewModel
    participant Sensors as GPS + Magnetómetro
    participant Repo as InspectionRepository

    Usuario->>UI: Selecciona nuevo recorrido
    UI->>VM: prepareInspection()
    VM->>Sensors: Verificar permiso y GPS
    alt Permiso denegado o GPS apagado
        Sensors-->>VM: Ubicación no disponible
        VM-->>UI: Solicita permiso o activar GPS
    else Ubicación disponible
        Sensors-->>VM: Posición inicial
        VM-->>UI: Muestra mapa y botón Iniciar
        Usuario->>UI: Inicia recorrido
        UI->>VM: startInspection()
        VM->>Repo: createRoute(startTime, userId, farmId)
        Repo-->>VM: routeId
        VM->>Sensors: startLocationAndHeadingUpdates()
        loop Mientras el recorrido esté activo
            Sensors-->>VM: LocationPoint
            VM->>Repo: appendPoint(routeId, point)
            Sensors-->>VM: headingDegrees
            VM-->>UI: Actualizar trazado, distancia y brújula
            opt Se pierde la señal GPS
                Sensors-->>VM: locationUnavailable
                VM-->>UI: Mostrar Sin señal GPS
            end
        end
        Usuario->>UI: Finaliza recorrido
        UI->>VM: finishInspection()
        VM->>Sensors: stopUpdates()
        VM->>Repo: completeRoute(routeId)
        Repo-->>VM: distancia, duración y animales
        VM-->>UI: Mostrar resumen
    end
```

---

## SD-06 — Registrar animal con foto y ubicación opcional

Cubre UC-20 y alimenta UC-21 (RF-20, RF-21).

El flujo se divide en dos partes para distinguir la interacción con el
dispositivo de la persistencia. Primero se obtiene la fotografía y se elige la
ubicación; después se guarda el registro y se sincronizan la imagen y sus datos.

### SD-06A — Capturar imagen y elegir ubicación

Describe la captura mediante CameraX o la selección desde la galería. En una
foto existente se puede usar la ubicación EXIF, la posición actual o continuar
sin coordenadas.

> **Nota:** la ubicación es opcional para que la falta de GPS o de metadatos no
> impida registrar al animal.

```mermaid
sequenceDiagram
    actor Usuario
    participant UI as AnimalRegisterScreen
    participant Media as CameraX / Galería / EXIF
    participant GPS as Location Service
    participant VM as AnimalViewModel

    Usuario->>UI: Selecciona registrar animal
    alt Tomar fotografía
        UI->>Media: Abrir cámara
        Media-->>UI: imageUri
        UI->>GPS: getCurrentLocation()
        GPS-->>UI: Coordenadas o no disponible
    else Elegir de galería
        UI->>Media: Seleccionar imagen
        Media-->>UI: imageUri y coordenadas EXIF opcionales
        UI-->>Usuario: Elegir EXIF, ubicación actual o ninguna
        opt Elige ubicación actual
            UI->>GPS: getCurrentLocation()
            GPS-->>UI: Coordenadas o no disponible
        end
    end
    UI->>VM: prepareRecord(imageUri, selectedLocation)
    VM-->>UI: Formulario con ubicación opcional
    Usuario->>UI: Completa nombre, notas y confirma
    UI->>VM: save(animalData)
```

### SD-06B — Geocodificar, guardar y sincronizar

Describe cómo Nominatim convierte las coordenadas en un nombre de lugar y cómo
el repositorio conserva la foto localmente antes de subirla a Firebase Storage.
La URL obtenida se almacena junto con los datos del animal en Firestore.

> **Nota:** cuando no hay conexión, el registro conserva el estado `PENDING` y
> la carga de la fotografía y la geocodificación pueden ejecutarse más tarde.

```mermaid
sequenceDiagram
    participant VM as AnimalViewModel
    participant Repo as AnimalRepository
    participant Geo as Nominatim API
    participant Room as Room / SQLite
    participant Cloud as Storage + Firestore

    VM->>Repo: create(animalRecord)
    opt El registro tiene coordenadas
        alt Hay conexión
            Repo->>Geo: reverseGeocode(latitude, longitude)
            Geo-->>Repo: locationName
        else Sin conexión
            Note over Repo: Marcar geocodificación pendiente
        end
    end
    Repo->>Room: INSERT URI local y estado PENDING
    Repo-->>VM: Guardado local exitoso
    alt Hay conexión
        Repo->>Cloud: Subir foto a Firebase Storage
        Cloud-->>Repo: remotePhotoUrl
        Repo->>Cloud: Guardar datos y URL en Firestore
        Cloud-->>Repo: Confirmación
        Repo->>Room: UPDATE URL y estado SYNCED
    else Sin conexión
        Note over Repo,Room: Foto, geocodificación y documento<br/>quedan pendientes de sincronización
    end
```

---

## SD-07 — Captura de anotación y OCR on-device

Cubre UC-09, UC-10 y UC-11 (RF-09, RF-10, RF-11).

Representa el registro de una transacción a partir de una fotografía. ML Kit
reconoce el texto en el dispositivo y el parser intenta convertirlo en fecha,
concepto, monto y categoría para prellenar el formulario.

> **Nota:** el usuario siempre revisa y confirma la información. Si el OCR no
> reconoce el texto, puede tomar otra foto o continuar con el registro manual.

```mermaid
sequenceDiagram
    actor Usuario
    participant UI as OcrCaptureScreen
    participant VM as OcrViewModel
    participant OCR as ML Kit + Parser
    participant Repo as TransactionRepository

    Usuario->>UI: Selecciona registrar desde foto
    UI-->>Usuario: Abre cámara o galería
    Usuario->>UI: Confirma imagen
    UI->>VM: recognize(image)
    VM->>OCR: processAndExtract(image)
    alt No se reconoce texto
        OCR-->>VM: ExtractionError
        VM-->>UI: Ofrece nueva foto o registro manual
    else Texto reconocido
        OCR-->>VM: fecha, concepto, monto y categoría
        VM-->>UI: Formulario prellenado
        Usuario->>UI: Revisa, corrige y confirma
        UI->>VM: save(correctedData)
        VM->>VM: validate(correctedData)
        alt Datos inválidos
            VM-->>UI: Muestra errores
        else Datos válidos
            VM->>Repo: create(transaction)
            Repo-->>VM: Guardado y sincronización iniciada
            VM-->>UI: Transacción registrada
        end
    end
```

---

## SD-08 — Crear recordatorio y programar notificación local

Cubre el flujo principal de UC-19 (RF-19).

Muestra la creación o edición de un recordatorio y su programación en Android.
También incluye la cancelación de la alarma cuando el recordatorio se completa
o se elimina.

> **Nota:** el recordatorio se sincroniza como dato de la finca, pero la alarma
> que aparece en una fecha y hora determinadas se programa localmente en el
> dispositivo.

```mermaid
sequenceDiagram
    actor Usuario
    participant UI as ReminderScreen
    participant VM as ReminderViewModel
    participant Repo as ReminderRepository
    participant Android as Scheduler + Notifications

    Usuario->>UI: Crea o edita recordatorio
    UI->>VM: save(formData)
    VM->>VM: validate(formData)
    alt Datos inválidos
        VM-->>UI: Muestra errores
    else Datos válidos
        VM->>Repo: upsert(reminder, status = SCHEDULED)
        Note over Repo: Guarda en Room y sincroniza con Firestore
        Repo-->>VM: reminderId
        VM->>Android: schedule(reminderId, scheduledAt, repeat)
        Android-->>VM: Programación confirmada
        VM-->>UI: Recordatorio guardado
        Note over Android: Al llegar la fecha y hora
        Android-->>Usuario: Mostrar notificación local
    end
    opt Completar o eliminar
        Usuario->>UI: Completa o elimina recordatorio
        UI->>VM: close(reminderId, action)
        VM->>Android: cancel(reminderId)
        VM->>Repo: updateStatus(COMPLETED o DELETED)
        VM-->>UI: Lista actualizada
    end
```

---

## SD-09 — Notificar al propietario por una transacción del trabajador

Cubre el flujo push alternativo de UC-19.

Describe la notificación enviada al propietario cuando un trabajador registra
una transacción. La creación del documento activa una Cloud Function, que busca
los propietarios autorizados y envía el mensaje por Firebase Cloud Messaging.

> **Nota:** `FirebaseMessagingService` recibe el mensaje en el dispositivo del
> propietario y genera la notificación visible de Android.

```mermaid
sequenceDiagram
    participant Worker as App del trabajador
    participant FS as Cloud Firestore
    participant Fn as Cloud Functions
    participant FCM as Firebase Cloud Messaging
    participant Owner as FirebaseMessagingService del propietario

    Worker->>FS: Crear transacción con farmId y createdBy
    FS-->>Fn: Evento onDocumentCreated
    Fn->>FS: Consultar membresías OWNER activas
    FS-->>Fn: Destinatarios y tokens autorizados
    Fn->>FCM: Enviar mensaje a tokens autorizados
    FCM-->>Owner: Entregar mensaje push
    Owner->>Owner: Validar payload y crear notificación
```

---

## SD-10 — Exportar reporte

Cubre UC-08 (RF-08).

Representa la generación de un reporte a partir de los filtros activos del
balance o del historial. El caso de uso consulta las transacciones, genera el
archivo solicitado y entrega una URI que Android puede compartir.

> **Nota:** si la consulta no devuelve datos, el sistema informa la situación y
> no crea un archivo vacío.

```mermaid
sequenceDiagram
    actor Propietario
    participant UI as BalanceScreen / HistoryScreen
    participant VM as ReportViewModel
    participant UC as ExportReportUseCase
    participant Repo as TransactionRepository

    Propietario->>UI: Selecciona exportar
    UI-->>Propietario: Muestra PDF o Excel
    Propietario->>UI: Elige formato
    UI->>VM: export(format, activeFilters, farmId)
    VM->>UC: execute(format, activeFilters, farmId)
    UC->>Repo: getByFilters(activeFilters, farmId)
    Note over Repo: Consulta la proyección local de Room
    Repo-->>UC: transactions
    alt No hay datos
        UC-->>VM: EmptyDataError
        VM-->>UI: No hay datos para exportar
    else Hay datos
        UC->>UC: Generar archivo y URI compartible
        UC-->>VM: fileUri
        VM-->>UI: Abrir Android Share Sheet
        UI-->>Propietario: Opciones para guardar o compartir
    end
```
