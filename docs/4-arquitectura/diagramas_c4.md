# Diagramas C4
### Sistema de Gestión Económica — Finca Ganadera
*Versión 3 · 17 de agosto de 2026 — Multi-usuario, Firebase, GPS, sensores, Nominatim*

---

## Nivel 1 — Contexto del Sistema

Muestra el sistema como caja negra, sus actores y los sistemas externos con los que interactúa.

![C4 Nivel 1 — Contexto](assets/c4-nivel1-contexto.svg)

<details>
<summary>Fuente PlantUML</summary>

```plantuml
@startuml c4-nivel1-contexto
!include <C4/C4_Context>

title Diagrama de Contexto — SGE Finca

Person(owner, "Propietario", "Dueño/encargado de la finca. Acceso completo: finanzas, gestión de trabajadores, configuración.")
Person(worker, "Trabajador", "Empleado de campo. Registra transacciones, recorridos de inspección y animales.")

System(sge, "SGE Finca", "App Android multi-usuario para registrar ingresos/egresos, rastrear inspecciones de campo y gestionar el ganado con fotos geolocalizadas.")

System_Ext(firebase, "Firebase", "Auth (autenticación), Firestore (BD remota con sync offline), FCM (notificaciones push).")
System_Ext(gmaps, "Google Maps", "SDK de mapas para visualizar recorridos de inspección y ubicaciones de animales.")
System_Ext(nominatim, "Nominatim", "API REST de geocoding inverso: convierte coordenadas GPS a nombres de lugar.")

Rel(owner, sge, "Gestiona finca, registra transacciones, consulta balance, exporta reportes", "Android")
Rel(worker, sge, "Registra transacciones, recorridos e inspecciones de animales", "Android")
Rel(sge, firebase, "Autentica usuarios, sincroniza datos, envía notificaciones", "HTTPS / SDK")
Rel(sge, gmaps, "Muestra mapas con rutas y ubicaciones", "SDK")
Rel(sge, nominatim, "Consulta nombre de lugar por coordenadas", "HTTPS / REST")

SHOW_LEGEND()
@enduml
```

</details>

### Notas
- Firebase cumple el requisito del curso de "backend de tercero para autenticación y almacenamiento".
- Nominatim cumple el requisito de "API REST externa" diferente a clima.
- Google Maps cumple el requisito de "GPS + mapas + seguimiento en tiempo real".
- Si se implementa el módulo OCR (Could), se usa ML Kit on-device — no requiere sistema externo adicional.

---

## Nivel 2 — Contenedores

Muestra las unidades desplegables del sistema y cómo se comunican.

![C4 Nivel 2 — Contenedores](assets/c4-nivel2-contenedores.svg)

<details>
<summary>Fuente PlantUML</summary>

```plantuml
@startuml c4-nivel2-contenedores
!include <C4/C4_Container>

title Diagrama de Contenedores — SGE Finca

Person(owner, "Propietario")
Person(worker, "Trabajador")

System_Boundary(sge, "SGE Finca") {
    Container(app, "App Android", "Kotlin, Jetpack Compose", "Interfaz de usuario: registro de transacciones, balance, inspecciones, mapa de animales, contactos, recordatorios.")
    ContainerDb(room, "Caché Local", "Room / SQLite", "Caché offline para consultas rápidas (balances, filtros). Se sincroniza con Firestore.")
}

System_Ext(firebase_auth, "Firebase Auth", "Autenticación de usuarios (email/password).")
System_Ext(firestore, "Cloud Firestore", "BD remota: fuente de verdad. Sync offline nativo.")
System_Ext(fcm, "Firebase Cloud Messaging", "Notificaciones push para recordatorios.")
System_Ext(gmaps, "Google Maps SDK", "Mapas para rutas de inspección y ubicaciones de animales.")
System_Ext(nominatim, "Nominatim API", "Geocoding inverso (coordenadas → nombre de lugar).")

Rel(owner, app, "Usa", "Pantalla táctil")
Rel(worker, app, "Usa", "Pantalla táctil")
Rel(app, room, "Lee/escribe", "Room DAOs")
Rel(app, firebase_auth, "Autentica", "Firebase SDK")
Rel(app, firestore, "Sincroniza datos", "Firebase SDK")
Rel(firestore, room, "Popula caché", "Listeners")
Rel(app, fcm, "Recibe notificaciones", "Firebase SDK")
Rel(app, gmaps, "Renderiza mapas", "SDK")
Rel(app, nominatim, "Geocoding inverso", "Retrofit / HTTPS")

SHOW_LEGEND()
@enduml
```

</details>

### Notas
- Solo hay un artefacto desplegable: la **app Android** (APK). Room es un componente interno de la app.
- Firestore maneja la sincronización online/offline nativamente (ADR-002, ADR-003).
- Room sirve como caché estructurada para consultas SQL complejas (balances con agrupación, filtros combinados).

---

## Nivel 3 — Componentes

Muestra los módulos internos de la app Android y sus responsabilidades.

![C4 Nivel 3 — Componentes](assets/c4-nivel3-componentes.svg)

<details>
<summary>Fuente PlantUML</summary>

```plantuml
@startuml c4-nivel3-componentes
!include <C4/C4_Component>

title Diagrama de Componentes — App Android SGE Finca

Container_Boundary(app, "App Android") {

    Component(nav, "Navigation", "Navigation Compose", "Gestiona la navegación entre pantallas.")

    Component(ui_auth, "Auth", "Compose", "Login, registro, unirse a finca. UC-07, UC-13.")
    Component(ui_reg, "Registro de Transacciones", "Compose", "Formularios de ingreso/egreso. UC-01, UC-02.")
    Component(ui_hist, "Historial", "Compose", "Lista filtrable de transacciones. UC-05, UC-06.")
    Component(ui_bal, "Balance", "Compose", "Balance general, comparación, desglose. UC-03.")
    Component(ui_rep, "Reportes", "Compose", "Gráficos y exportación PDF/Excel. UC-08, UC-12.")
    Component(ui_cfg, "Configuración", "Compose", "Categorías, gestión de trabajadores. UC-04, UC-14.")
    Component(ui_route, "Inspecciones", "Compose + Google Maps", "Recorridos GPS en tiempo real, historial. UC-15, UC-16, UC-17.")
    Component(ui_animal, "Animales", "Compose + Google Maps", "Registro con foto y GPS, mapa de animales. UC-20, UC-21.")
    Component(ui_contact, "Contactos", "Compose", "Directorio de la finca, importación. UC-18.")
    Component(ui_reminder, "Recordatorios", "Compose", "Crear/gestionar recordatorios. UC-19.")
    Component(ui_cam, "Captura OCR", "Compose + CameraX", "Foto de anotación + extracción de texto. UC-09, UC-10. (Could)")

    Component(vm, "ViewModels", "ViewModel + StateFlow", "Estado de UI, lógica de presentación.")
    Component(uc, "Use Cases", "Kotlin", "Lógica de negocio: cálculo de balance, validaciones.")
    Component(repo, "Repositories", "Kotlin", "Abstracción de acceso a datos. Patrón write-through.")
    Component(dao, "Room DAOs", "Room", "Queries SQL para CRUD y agregaciones.")
    Component(db, "Room Database", "SQLite", "Esquema, migraciones, caché offline.")
    Component(gps, "Location Service", "Fused Location Provider", "Rastreo GPS en tiempo real para inspecciones.")
    Component(sensors, "Sensor Manager", "Android Sensors", "Acelerómetro (shake) + Magnetómetro (brújula).")
}

System_Ext(firebase_auth, "Firebase Auth")
System_Ext(firestore, "Cloud Firestore")
System_Ext(fcm, "FCM")
System_Ext(gmaps, "Google Maps SDK")
System_Ext(nominatim, "Nominatim API")

Rel(nav, ui_auth, "navega")
Rel(nav, ui_reg, "navega")
Rel(nav, ui_hist, "navega")
Rel(nav, ui_bal, "navega")
Rel(nav, ui_rep, "navega")
Rel(nav, ui_cfg, "navega")
Rel(nav, ui_route, "navega")
Rel(nav, ui_animal, "navega")
Rel(nav, ui_contact, "navega")
Rel(nav, ui_reminder, "navega")
Rel(nav, ui_cam, "navega")

Rel(ui_reg, vm, "eventos / estado")
Rel(ui_hist, vm, "eventos / estado")
Rel(ui_bal, vm, "eventos / estado")
Rel(ui_rep, vm, "eventos / estado")
Rel(ui_cfg, vm, "eventos / estado")
Rel(ui_auth, vm, "eventos / estado")
Rel(ui_route, vm, "eventos / estado")
Rel(ui_animal, vm, "eventos / estado")
Rel(ui_contact, vm, "eventos / estado")
Rel(ui_reminder, vm, "eventos / estado")
Rel(ui_cam, vm, "eventos / estado")

Rel(vm, uc, "invoca")
Rel(vm, repo, "CRUD directo")
Rel(uc, repo, "consulta / persiste")
Rel(repo, dao, "queries")
Rel(dao, db, "SQLite")
Rel(repo, firestore, "sync remoto")
Rel(repo, firebase_auth, "autenticación")
Rel(repo, nominatim, "geocoding", "Retrofit")
Rel(gps, ui_route, "posición en tiempo real")
Rel(gps, ui_animal, "ubicación para foto")
Rel(sensors, vm, "eventos de sensores")
Rel(vm, fcm, "notificaciones push")

SHOW_LEGEND()
@enduml
```

</details>

### Mapeo componentes → requisitos

| Componente | Casos de Uso | Requisitos |
|---|---|---|
| Auth + VM | UC-07, UC-13, UC-14 | RF-07, RF-13, RF-14, RNF-04, RNF-08 |
| Registro de Transacciones + VM | UC-01, UC-02 | RF-01, RF-02, RNF-01 |
| Balance + VM | UC-03 | RF-03, RNF-02 |
| Historial + VM | UC-05, UC-06 | RF-05, RF-06 |
| Configuración | UC-04, UC-14 | RF-04, RF-14 |
| Reportes + VM | UC-08, UC-12 | RF-08, RF-12 |
| Inspecciones + Google Maps | UC-15, UC-16, UC-17 | RF-15, RF-16, RF-17, RNF-10 |
| Animales + Google Maps | UC-20, UC-21 | RF-20, RF-21, RNF-10 |
| Contactos | UC-18 | RF-18 |
| Recordatorios + FCM | UC-19 | RF-19 |
| Captura OCR + CameraX | UC-09, UC-10, UC-11 | RF-09, RF-10, RF-11, RNF-03 |
| Room Database + DAOs | — (transversal) | RNF-06, RNF-07 |
| Repositories + Firestore | — (transversal) | RNF-07, RNF-08 |
| Location Service | — (transversal) | RNF-10 |
| Sensor Manager | — (transversal) | RNF-09 |

### Mapeo a requisitos del curso

| Requisito del curso | Componente que lo cumple |
|---|---|
| App móvil multi-usuario Android | Auth (Firebase Auth) + roles Propietario/Trabajador |
| Cámara | CameraX (Captura OCR + foto de animal) |
| Galería y almacenamiento | Selector de fotos + Firestore/Room |
| Contactos | Pantalla de Contactos (importación desde dispositivo) |
| 2 sensores ≠ luz ambiental | Acelerómetro (shake-to-register) + Magnetómetro (brújula en inspecciones) |
| GPS + mapas + seguimiento en tiempo real | Location Service + Google Maps SDK (Inspecciones + Animales) |
| Manejo de rutas | Inspecciones: inicio, tracking, historial de recorridos |
| Notificaciones | FCM (push) + notificaciones locales (Recordatorios) |
| Backend de tercero | Firebase (Auth + Firestore + FCM) |
| API REST externa ≠ clima | Nominatim (geocoding inverso) vía Retrofit |

---

> **Regenerar los SVG:** tras editar cualquier bloque PlantUML de este documento, ejecuta `scripts/render-diagrams.sh` y commitea los cambios de `assets/`. El CI falla si el SVG commiteado no corresponde a la fuente.
