# Lean Canvas
### Finca — Sistema de Gestión Económica para Fincas Ganaderas
*Versión 2 · 11 de agosto de 2026 — GPS: inspecciones + animales geolocalizados; OCR on-device + Nominatim*

---

## Canvas resumen

| Problema | Solución | Propuesta de Valor Única | Ventaja Especial | Segmento de Clientes |
|---|---|---|---|---|
| 1. Control financiero manual en cuadernos de papel: no hay totales, ni historial consultable, ni reportes | 1. Registro digital de ingresos/egresos con categorías predefinidas para ganadería lechera | *"Control financiero y operativo de tu finca desde el celular — diseñado para el campo, no para la oficina"* | Categorías financieras validadas con usuarios reales de ganadería lechera colombiana | **Primario:** Propietarios de fincas ganaderas lecheras pequeñas/medianas (Colombia, 10–100 cabezas) |
| 2. Cero coordinación digital entre propietario y trabajadores de campo | 2. Multi-usuario con roles (propietario/trabajador) y sincronización en tiempo real | | OCR on-device gratuito para anotaciones manuscritas — no obliga al usuario a cambiar sus hábitos ni genera costos por uso | **Secundario:** Trabajadores de campo que registran operaciones diarias |
| 3. Sin registro geolocalizado del ganado ni de inspecciones de campo | 3. Recorridos de inspección con GPS en tiempo real + registro de animales con foto y ubicación en mapa | | Offline-first: funciona en zonas rurales con conectividad intermitente | |
| 4. Información de contactos clave dispersa y sin relación con la operación | 4. Directorio de contactos integrado + notificaciones para tareas recurrentes | | | |
| **Alternativas existentes:** cuaderno de papel; Excel genérico en el celular; apps de finanzas personales (no entienden ganadería); software ganadero empresarial (costoso y complejo) | 5. Captura de foto de anotación manuscrita → extracción de texto con OCR on-device (ML Kit) | | | **Early adopters:** Administradores que ya usan smartphone pero siguen con cuaderno por falta de una app adecuada |

| Métricas Clave | Canales |
|---|---|
| Transacciones registradas por semana vs. cuaderno | Google Play Store |
| Tiempo promedio entre transacción y registro | Cooperativas lecheras y asociaciones ganaderas |
| Animales registrados con foto y ubicación GPS | Extensionistas agrícolas (asistencia técnica rural) |
| Tasa de adopción de trabajadores por finca | Voz a voz en comunidades rurales |

| Estructura de Costos | Fuentes de Ingreso |
|---|---|
| Desarrollo (equipo de 4 durante un semestre académico) | *(No es el foco del proyecto académico)* |
| Firebase — free tier cubre el volumen inicial | Modelo freemium: funcionalidad base gratuita |
| OCR on-device (ML Kit) — gratuito, sin costo por uso | Funciones avanzadas (reportes PDF) como compra in-app |
| Google Maps SDK — free tier generoso para el volumen esperado | |
| Nominatim API — gratuita (geocoding inverso) | |

---

## Detalle por sección

### 1. Problema (foco principal)

El proyecto nace de la observación directa de una finca ganadera lechera de 25 cabezas en Colombia. Los hallazgos clave son:

**P1 — Control financiero manual:**
El propietario registra todos los movimientos económicos (venta de leche, compra de insumos, pagos de jornales, gastos veterinarios) en un cuaderno de papel. Esto implica:
- No hay totales automáticos: calcular el balance del mes requiere sumar a mano decenas de entradas.
- No hay historial consultable: encontrar un gasto de hace tres meses significa hojear el cuaderno página por página.
- No hay reportes ni tendencias: el propietario no sabe si este mes gastó más o menos que el anterior, ni cuál actividad (lechería vs. ganado) es más rentable.
- El cuaderno se puede dañar, perder o mojar en campo.

**P2 — Falta de coordinación digital:**
En fincas con más de un trabajador, las transacciones que ocurren en campo (compra de sal, pago al jornalero, venta de un ternero) se comunican verbalmente al propietario. Esto causa registros tardíos, duplicados u olvidados.

**P3 — Sin registro geolocalizado del ganado ni de inspecciones:**
El propietario no tiene forma de registrar dónde están los animales, en qué potrero se vieron por última vez, ni documentar los recorridos de inspección diarios. No hay trazabilidad de las visitas a campo.

**P4 — Información de contactos dispersa:**
Los números del veterinario, del proveedor de concentrado, del comprador de leche están en la agenda personal del propietario (o de memoria). No están asociados a las transacciones de la finca, y un trabajador nuevo no tiene acceso a ellos.

### 2. Segmento de clientes

| Segmento | Descripción | Necesidad principal |
|---|---|---|
| **Propietario/Administrador** | Dueño o encargado de una finca ganadera lechera pequeña/mediana. Maneja las finanzas, toma decisiones operativas. Usa smartphone pero no es experto en tecnología. | Saber en cualquier momento cuánto entra y cuánto sale, sin sumar a mano |
| **Trabajador de campo** | Empleado que realiza tareas diarias (ordeño, inspección, compras). Tiene un celular Android básico. | Registrar transacciones, documentar animales y recorridos desde el campo |

### 3. Propuesta de valor única

> **"Control financiero y operativo de tu finca desde el celular — diseñado para el campo, no para la oficina."**

- Simple: botones grandes, flujos de máximo 3 pasos, iconografía intuitiva (RNF-01).
- Completa: cubre desde el registro de un gasto hasta el reporte mensual, el rastreo de inspecciones y el registro geolocalizado de animales.
- Para el campo: funciona offline, interfaz pensada para manos sucias y sol directo.

### 4. Solución

| # | Funcionalidad | Problema que resuelve | Requisito del curso |
|---|---|---|---|
| S1 | Registro de ingresos/egresos con categorías ganaderas predefinidas | P1 — Control financiero manual | Almacenamiento local + backend |
| S2 | Balance automático, historial filtrable y reportes exportables (PDF/Excel) | P1 — Sin totales ni reportes | Backend (Firebase) |
| S3 | Multi-usuario con roles (propietario/trabajador) y sincronización | P2 — Sin coordinación digital | Multi-usuario + backend (Firebase Auth + Firestore) |
| S4 | Recorridos de inspección con GPS en tiempo real + registro de animales con foto geolocalizada en mapa | P3 — Sin registro geolocalizado | GPS + mapas + seguimiento en tiempo real + cámara |
| S5 | Captura de foto de anotación manuscrita → extracción de texto con OCR on-device (ML Kit) | P1 — Cuaderno de papel | Cámara/galería |
| S6 | Directorio de contactos de la finca importado desde el dispositivo | P4 — Contactos dispersos | Contactos del dispositivo |
| S7 | Recordatorios con notificaciones push (vacunación, pagos, ordeño) | P2 — Comunicación verbal | Notificaciones |
| S8 | Registro rápido por agitación del dispositivo (shake) + auto-pausa en bolsillo | — Conveniencia en campo | 2 sensores (acelerómetro + proximidad) |

### 5. Métricas clave

- **Adopción:** usuarios activos por finca por semana.
- **Eficiencia:** transacciones registradas digitalmente vs. estimación en cuaderno.
- **Cobertura:** animales registrados con foto y ubicación GPS.
- **Calidad OCR:** % de campos correctamente extraídos de fotos (sin corrección manual).

### 6. Ventaja especial

1. **Dominio específico:** las categorías financieras (Venta de leche, Compra de concentrado, Inseminación, etc.) fueron validadas con un usuario real de ganadería lechera colombiana — no son genéricas.
2. **Puente analógico-digital:** la captura por foto con OCR permite que el usuario siga escribiendo en su cuaderno si quiere, y la app extrae los datos automáticamente. Sin costo de API.
3. **Offline-first:** la app funciona sin conexión (Room/SQLite como caché local); se sincroniza con Firebase cuando hay internet. Crítico en zonas rurales colombianas.

### 7. Canales

- **Google Play Store** — distribución directa.
- **Cooperativas lecheras** — canal natural para llegar a pequeños productores.
- **Asistentes técnicos agrícolas** — ya visitan las fincas y pueden recomendar la app.
- **Voz a voz** — en comunidades rurales pequeñas, la recomendación personal es el canal más efectivo.

---

## Mapeo a requisitos técnicos del curso

| Requisito del curso | Cómo se cumple en Finca |
|---|---|
| App móvil multi-usuario (Android) | Roles Propietario/Trabajador con Firebase Auth + Firestore |
| Cámara y galería | Foto de anotación manuscrita (OCR); foto de animales con GPS; adjuntar fotos a transacciones |
| Almacenamiento | Room/SQLite como caché local (offline-first) + Firestore como fuente remota |
| Contactos | Directorio de contactos de la finca, importación desde contactos del dispositivo (UC-18) |
| 2 sensores ≠ luz ambiental | **Acelerómetro:** shake-to-register (gesto de agitación para abrir registro rápido). **Proximidad:** auto-pausa de pantalla cuando el teléfono está en bolsillo durante tracking GPS |
| GPS + mapas + seguimiento en tiempo real | Recorridos de inspección con rastreo GPS en tiempo real; registro de animales con foto y ubicación; visualización en Google Maps (UC-15, UC-16, UC-20, UC-21) |
| Manejo de rutas | Registro, seguimiento en tiempo real y consulta de historial de recorridos de inspección |
| Notificaciones | Recordatorios locales (vacunación, pagos) + push notifications vía Firebase Cloud Messaging (UC-19) |
| Backend de tercero | Firebase Auth (autenticación) + Firestore (almacenamiento) + Storage (fotos) + FCM (notificaciones) |
| API REST externa ≠ clima | **Nominatim** (geocoding inverso): convierte coordenadas GPS a nombres de lugar legibles al registrar fotos de animales o inspecciones |
