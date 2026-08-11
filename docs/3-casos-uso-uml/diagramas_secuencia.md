# Diagramas de Secuencia
### Sistema de Gestión Económica — Finca Ganadera
*Versión 2 · 10 de julio de 2026*

---

> **Nota:** Versión alineada con la arquitectura definida en el Paso 5 (`docs/4-arquitectura/`): MVVM + Clean Architecture ligera (estilo_arquitectonico.md) y stack Kotlin/Compose/Room (ADR-001, ADR-002). Los participantes son los componentes reales de cada capa. Según la convención de indirección: los flujos CRUD van `ViewModel → Repository` directo; la lógica no trivial (balance, exportación) pasa por un Use Case. Los nombres de métodos están en inglés (convención de código); las descripciones, en español.
>
> **Cambios de la v2:** se eliminó la "Cola de Sincronización" de SD-01 — contradecía ADR-003, que define un modelo **local-only**: la BD local es la fuente de verdad y no hay sincronización por transacción, solo backup periódico de la BD completa a Google Drive.

## SD-01 — Registrar transacción (ingreso o egreso)

Cubre UC-01 y UC-02 (HU-01, HU-02).

```mermaid
sequenceDiagram
    actor Admin as Administrador
    participant Screen as RegisterScreen
    participant VM as RegisterViewModel
    participant CatRepo as CategoryRepository
    participant TxRepo as TransactionRepository
    participant DB as Room / SQLite

    Admin->>Screen: Selecciona "Registrar ingreso/egreso"
    Screen->>VM: loadForm(type)
    VM->>CatRepo: getActiveCategories(type)
    CatRepo->>DB: SELECT categorías activas
    DB-->>CatRepo: categories
    CatRepo-->>VM: categories
    VM-->>Screen: UiState(fecha = hoy, categorías)
    Admin->>Screen: Completa campos y confirma

    Screen->>VM: onSave(formData)
    VM->>VM: validate() — campos obligatorios

    alt Validación falla
        VM-->>Screen: UiState.error("El monto es obligatorio")
        Screen-->>Admin: Muestra error
    else Validación exitosa
        VM->>TxRepo: save(transaction)
        TxRepo->>DB: INSERT
        DB-->>TxRepo: ok
        TxRepo-->>VM: saved
        VM-->>Screen: UiState.success
        Screen-->>Admin: "Transacción registrada"
    end

    Note over DB: La BD local es la fuente de verdad (ADR-003).<br/>El flujo es idéntico con o sin conexión (RNF-07).<br/>El backup a Google Drive lo hace WorkManager<br/>en segundo plano cuando hay conexión.
```

---

## SD-02 — Consultar balance general con comparación de periodos

Cubre UC-03 (HU-03). El cálculo del balance es lógica no trivial → pasa por `CalculateBalanceUseCase` (convención del estilo arquitectónico).

```mermaid
sequenceDiagram
    actor Admin as Administrador
    participant Screen as BalanceScreen
    participant VM as BalanceViewModel
    participant UC as CalculateBalanceUseCase
    participant Repo as TransactionRepository

    Admin->>Screen: Abre pantalla de balance
    Screen->>VM: loadBalance(currentMonth)
    VM->>UC: execute(currentMonth)
    UC->>Repo: getTransactions(currentMonth)
    Repo-->>UC: transactions
    UC->>UC: aggregate(income, expenses, net)
    UC-->>VM: Balance
    VM-->>Screen: UiState(balance)
    Screen-->>Admin: Muestra balance del mes

    Admin->>Screen: Selecciona "Comparar con mes anterior"
    Screen->>VM: compare(currentMonth, previousMonth)
    VM->>UC: execute(currentMonth, previousMonth)
    UC->>Repo: getTransactions(previousMonth)
    Repo-->>UC: previousTransactions
    UC->>UC: aggregate ambos periodos + variación
    UC-->>VM: BalanceComparison
    VM-->>Screen: UiState(comparación)
    Screen-->>Admin: Muestra ambos periodos lado a lado

    Admin->>Screen: Selecciona "Desglose por actividad"
    Screen->>VM: breakdownByActivity(period)
    VM->>UC: executeBreakdown(period)
    UC->>UC: groupByActivity(transactions)
    UC-->>VM: ActivityBreakdown
    VM-->>Screen: UiState(desglose)
    Screen-->>Admin: Muestra subtotales (Lechería, Ganado, General)
```

---

## SD-03 — Editar una transacción

Cubre UC-06 vía UC-05 (HU-06, HU-05). CRUD simple → `ViewModel → Repository` sin Use Case.

```mermaid
sequenceDiagram
    actor Admin as Administrador
    participant Hist as HistoryScreen
    participant Edit as EditTransactionScreen
    participant VM as EditTransactionViewModel
    participant Repo as TransactionRepository

    Admin->>Hist: Selecciona una transacción
    Admin->>Hist: Elige "Editar"
    Hist->>Edit: navigate(transactionId)
    Edit->>VM: load(transactionId)
    VM->>Repo: getById(transactionId)
    Repo-->>VM: transaction
    VM-->>Edit: UiState(datos precargados)
    Edit-->>Admin: Muestra formulario con datos

    Admin->>Edit: Modifica campos y confirma
    Edit->>VM: onSave(transactionId, formData)
    VM->>VM: validate() — campos obligatorios

    alt Validación falla
        VM-->>Edit: UiState.error
        Edit-->>Admin: Muestra error
    else Validación exitosa
        VM->>Repo: update(transaction)
        Repo-->>VM: updated
        VM-->>Edit: UiState.success
        Edit-->>Admin: "Transacción actualizada"
    end

    Note over Repo: El balance no se recalcula manualmente:<br/>BalanceScreen observa la BD vía Flow (ADR-002)<br/>y se actualiza de forma reactiva.
```

---

## SD-04 — Eliminar una transacción

Cubre UC-06 vía UC-05 (HU-06). CRUD simple → `ViewModel → Repository` sin Use Case.

```mermaid
sequenceDiagram
    actor Admin as Administrador
    participant Hist as HistoryScreen
    participant Dialog as ConfirmDialog
    participant VM as HistoryViewModel
    participant Repo as TransactionRepository

    Admin->>Hist: Selecciona una transacción
    Admin->>Hist: Elige "Eliminar"
    Hist->>Dialog: show("¿Eliminar esta transacción?")
    Dialog-->>Admin: Muestra diálogo

    alt Administrador confirma
        Admin->>Dialog: Confirma
        Dialog->>VM: onDeleteConfirmed(transactionId)
        VM->>Repo: delete(transactionId)
        Repo-->>VM: deleted
        VM-->>Hist: UiState actualizado
        Hist-->>Admin: "Transacción eliminada"
    else Administrador cancela
        Admin->>Dialog: Cancela
        Dialog-->>Hist: Sin cambios
    end
```

---

## SD-05 — Captura por foto → extracción IA → corrección → registro

Cubre UC-09 → UC-10 → UC-11 (HU-09, HU-10, HU-11). Solo aplica si se implementa el épico de IA (Could). La cámara se maneja con CameraX (ADR-001); la llamada a la API multimodal, con el componente AI Client del C4 nivel 3.

```mermaid
sequenceDiagram
    actor Admin as Administrador
    participant Screen as CaptureScreen
    participant Cam as CameraX
    participant VM as CaptureViewModel
    participant AI as AiClient
    participant API as API IA Multimodal
    participant Repo as TransactionRepository

    Admin->>Screen: Selecciona "Registrar desde foto"
    Screen->>Cam: openCamera()
    Admin->>Cam: Toma foto de anotación
    Cam-->>Screen: preview(image)
    Screen-->>Admin: Muestra foto con "Usar" / "Volver a tomar"

    alt Volver a tomar
        Admin->>Screen: "Volver a tomar"
        Screen->>Cam: openCamera()
    else Usar foto
        Admin->>Screen: "Usar foto"

        alt Sin conexión
            Screen-->>Admin: "Esta función requiere internet"
            Note over Screen: Ofrece guardar la foto<br/>o registrar manualmente
        else Con conexión
            Screen->>VM: extractData(image)
            VM->>AI: extract(image)
            AI->>API: POST imagen (HTTPS)
            Note over Screen: Indicador de carga (RNF-03)
            API-->>AI: JSON(fecha, concepto, monto)
            AI-->>VM: ExtractionResult

            alt Extracción fallida
                VM-->>Screen: UiState.error("No se pudo leer la anotación")
                Screen-->>Admin: Ofrece retomar foto o registro manual
            else Extracción exitosa
                VM-->>Screen: UiState(formulario prellenado + foto de referencia)
                Screen-->>Admin: Muestra formulario + foto

                Admin->>Screen: Revisa, corrige si es necesario, confirma
                Screen->>VM: onSave(formData)
                VM->>VM: validate() — mismas reglas que UC-01/UC-02
                VM->>Repo: save(transaction)
                Repo-->>VM: saved
                VM-->>Screen: UiState.success
                Screen-->>Admin: "Transacción registrada"
            end
        end
    end
```

---

## SD-06 — Exportar reporte

Cubre UC-08 (HU-08). La generación del archivo es lógica no trivial → pasa por `ExportReportUseCase`.

```mermaid
sequenceDiagram
    actor Admin as Administrador
    participant Screen as BalanceScreen / HistoryScreen
    participant VM as ReportViewModel
    participant UC as ExportReportUseCase
    participant Repo as TransactionRepository
    participant OS as Android Share Sheet

    Admin->>Screen: Selecciona "Exportar"
    Screen-->>Admin: Muestra opciones de formato (PDF / Excel)
    Admin->>Screen: Elige formato

    Screen->>VM: onExport(format, activeFilters)
    VM->>UC: execute(activeFilters, format)
    UC->>Repo: getTransactions(activeFilters)
    Repo-->>UC: transactions

    alt Sin datos
        UC-->>VM: EmptyDataError
        VM-->>Screen: UiState.error
        Screen-->>Admin: "No hay datos para exportar"
    else Con datos
        UC->>UC: generateFile(transactions, format)
        UC-->>VM: file(bytes, fileName)
        VM->>OS: share(file) — Intent
        OS-->>Admin: Opciones del sistema (compartir, guardar, etc.)
    end
```

---

## Resumen de cobertura

| Diagrama | Casos de uso | Historias de usuario | Prioridad |
|---|---|---|---|
| SD-01 | UC-01, UC-02 | HU-01, HU-02 | Must |
| SD-02 | UC-03 | HU-03 | Must |
| SD-03 | UC-05, UC-06 | HU-05, HU-06 | Must |
| SD-04 | UC-05, UC-06 | HU-05, HU-06 | Must |
| SD-05 | UC-09, UC-10, UC-11 | HU-09, HU-10, HU-11 | Could |
| SD-06 | UC-08 | HU-08 | Should |

**Nota:** UC-04 (Gestionar categorías) y UC-07 (Autenticarse) no tienen diagrama de secuencia dedicado porque sus flujos son lineales y quedan suficientemente cubiertos en las especificaciones de casos de uso. UC-12 (Reportes visuales) es análogo a SD-02 (consulta de datos + renderizado).

## Mapeo participantes → arquitectura (Paso 5)

| Participante | Capa | Referencia |
|---|---|---|
| `*Screen`, `ConfirmDialog` | UI (Compose) | estilo_arquitectonico.md — Capa UI |
| `*ViewModel` | UI (estado vía StateFlow) | estilo_arquitectonico.md — Capa UI |
| `CalculateBalanceUseCase`, `ExportReportUseCase` | Domain | Convención: lógica no trivial → Use Case |
| `TransactionRepository`, `CategoryRepository` | Data | estilo_arquitectonico.md — Capa Data |
| `Room / SQLite` | Data | ADR-002 |
| `AiClient` | Data | C4 nivel 3 — componente AI Client (Could) |
| `CameraX`, `Android Share Sheet` | Plataforma Android | ADR-001 |
