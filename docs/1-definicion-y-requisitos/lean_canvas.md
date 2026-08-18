# Lean Canvas
### Finca — Sistema de Gestión Económica para Fincas Ganaderas
*Versión 1 · 3 de agosto de 2026*

---

## Canvas resumen

| Problema | Solución | Propuesta de Valor Única | Ventaja Especial | Segmento de Clientes |
|---|---|---|---|---|
| 1. Control financiero manual en cuadernos de papel: no hay totales, ni historial consultable, ni reportes | 1. Registro digital de ingresos/egresos con categorías predefinidas para ganadería lechera | *"Control financiero y operativo de tu finca desde el celular — diseñado para el campo, no para la oficina"* | Categorías financieras validadas con usuarios reales de ganadería lechera colombiana | **Primario:** Propietarios de fincas ganaderas lecheras pequeñas/medianas (Colombia, 10–100 cabezas) |
| 2. Cero coordinación digital entre propietario y trabajadores de campo | 2. Multi-usuario con roles (propietario/trabajador) y sincronización en tiempo real | | Reconocimiento de escritura manuscrita con IA — no obliga al usuario a cambiar sus hábitos | **Secundario:** Trabajadores de campo que registran operaciones diarias |
| 3. Rutas de entrega de leche sin documentar ni optimizar | 3. Rastreo GPS de rutas de entrega con mapa en tiempo real | | Offline-first: funciona en zonas rurales con conectividad intermitente | |
| 4. Información de contactos clave dispersa y sin relación con la operación | 4. Directorio de contactos integrado + captura inteligente de anotaciones con IA | | | |
| **Alternativas existentes:** cuaderno de papel; Excel genérico en el celular; apps de finanzas personales (no entienden ganadería); software ganadero empresarial (costoso y complejo) | 5. Notificaciones y recordatorios para tareas recurrentes (vacunación, pagos, ordeño) | | | **Early adopters:** Administradores que ya usan smartphone pero siguen con cuaderno por falta de una app adecuada |

| Métricas Clave | Canales |
|---|---|
| Transacciones registradas por semana vs. cuaderno | Google Play Store |
| Tiempo promedio entre transacción y registro | Cooperativas lecheras y asociaciones ganaderas |
| Rutas de entrega rastreadas y completadas | Extensionistas agrícolas (asistencia técnica rural) |
| Tasa de adopción de trabajadores por finca | Voz a voz en comunidades rurales |

| Estructura de Costos | Fuentes de Ingreso |
|---|---|
| Desarrollo (equipo de 4 durante un semestre académico) | *(No es el foco del proyecto académico)* |
| Firebase — free tier cubre el volumen inicial | Modelo freemium: funcionalidad base gratuita |
| API de IA multimodal — costo por llamada (solo para captura de foto) | Funciones avanzadas (IA, reportes PDF) como compra in-app |
| Google Maps SDK — free tier generoso para el volumen esperado | |

---

## Detalle por sección

### 1. Problema (foco principal)

El proyecto nace de la observación directa de una finca ganadera lechera de 25 cabezas en Colombia. Los hallazgos clave son:

**P1 — Control financiero manual:**
El administrador registra todos los movimientos económicos (venta de leche, compra de insumos, pagos de jornales, gastos veterinarios) en un cuaderno de papel. Esto implica:
- No hay totales automáticos: calcular el balance del mes requiere sumar a mano decenas de entradas.
- No hay historial consultable: encontrar un gasto de hace tres meses significa hojear el cuaderno página por página.
- No hay reportes ni tendencias: el administrador no sabe si este mes gastó más o menos que el anterior, ni cuál actividad (lechería vs. ganado) es más rentable.
- El cuaderno se puede dañar, perder o mojar en campo.

**P2 — Falta de coordinación digital:**
En fincas con más de un trabajador, las transacciones que ocurren en campo (compra de sal, pago al jornalero, venta de un ternero) se comunican verbalmente al propietario. Esto causa registros tardíos, duplicados u olvidados.

**P3 — Rutas de entrega sin trazabilidad:**
La leche se entrega diariamente a acopios o compradores por rutas que varían. No hay registro de qué ruta se tomó, cuánto se recorrió, ni cuánto tiempo tomó. Si hay un reclamo de un comprador, no se puede verificar si la entrega se hizo.

**P4 — Información de contactos dispersa:**
Los números del veterinario, del proveedor de concentrado, del comprador de leche están en la agenda personal del propietario (o de memoria). No están asociados a las transacciones de la finca, y un trabajador nuevo no tiene acceso a ellos.

### 2. Segmento de clientes

| Segmento | Descripción | Necesidad principal |
|---|---|---|
| **Propietario/Administrador** | Dueño o encargado de una finca ganadera lechera pequeña/mediana. Maneja las finanzas, toma decisiones operativas. Usa smartphone pero no es experto en tecnología. | Saber en cualquier momento cuánto entra y cuánto sale, sin sumar a mano |
| **Trabajador de campo** | Empleado que realiza tareas diarias (ordeño, entrega, compras). Tiene un celular Android básico. | Registrar transacciones y rutas desde el campo sin depender del propietario |

### 3. Propuesta de valor única

> **"Control financiero y operativo de tu finca desde el celular — diseñado para el campo, no para la oficina."**

- Simple: botones grandes, flujos de máximo 3 pasos, iconografía intuitiva (RNF-01).
- Completa: cubre desde el registro de un gasto hasta el reporte mensual y el rastreo de entregas.
- Para el campo: funciona offline, interfaz pensada para manos sucias y sol directo.

### 4. Solución

| # | Funcionalidad | Problema que resuelve | Requisito del curso |
|---|---|---|---|
| S1 | Registro de ingresos/egresos con categorías ganaderas predefinidas | P1 — Control financiero manual | Almacenamiento local + backend |
| S2 | Balance automático, historial filtrable y reportes exportables (PDF/Excel) | P1 — Sin totales ni reportes | Backend (Firebase) |
| S3 | Multi-usuario con roles (propietario/trabajador) y sincronización | P2 — Sin coordinación digital | Multi-usuario + backend (Firebase Auth + Firestore) |
| S4 | Rastreo GPS de rutas de entrega con mapa en tiempo real | P3 — Rutas sin trazabilidad | GPS + mapas + seguimiento en tiempo real |
| S5 | Captura de foto de anotación manuscrita → extracción de datos con IA | P1 — Cuaderno de papel | Cámara/galería + API REST externa (IA multimodal) |
| S6 | Directorio de contactos de la finca importado desde el dispositivo | P4 — Contactos dispersos | Contactos del dispositivo |
| S7 | Recordatorios con notificaciones push (vacunación, pagos, ordeño) | P2 — Comunicación verbal | Notificaciones |
| S8 | Registro rápido por agitación del dispositivo (shake) + auto-pausa en bolsillo | — Conveniencia en campo | 2 sensores (acelerómetro + proximidad) |

### 5. Métricas clave

- **Adopción:** usuarios activos por finca por semana.
- **Eficiencia:** transacciones registradas digitalmente vs. estimación en cuaderno.
- **Cobertura:** % de rutas de entrega rastreadas con GPS.
- **Calidad IA:** % de campos correctamente extraídos de fotos (sin corrección manual).

### 6. Ventaja especial

1. **Dominio específico:** las categorías financieras (Venta de leche, Compra de concentrado, Inseminación, etc.) fueron validadas con un usuario real de ganadería lechera colombiana — no son genéricas.
2. **Puente analógico-digital:** la captura por foto permite que el usuario siga escribiendo en su cuaderno si quiere, y la app extrae los datos automáticamente.
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
| Cámara y galería | Captura de foto de anotación manuscrita (UC-09); adjuntar fotos a transacciones |
| Almacenamiento | Room/SQLite como caché local (offline-first) + Firestore como fuente remota |
| Contactos | Directorio de contactos de la finca, importación desde contactos del dispositivo (UC-18) |
| 2 sensores ≠ luz ambiental | **Acelerómetro:** shake-to-register (gesto de agitación para abrir registro rápido). **Proximidad:** auto-pausa de pantalla cuando el teléfono está en bolsillo durante tracking GPS |
| GPS + mapas + seguimiento en tiempo real | Rastreo de rutas de entrega con Google Maps SDK; posición en tiempo real; historial de rutas (UC-15, UC-16, UC-17) |
| Manejo de rutas | Registro, seguimiento y consulta de rutas de entrega de leche |
| Notificaciones | Recordatorios locales (vacunación, pagos) + push notifications vía Firebase Cloud Messaging (UC-19) |
| Backend de tercero | Firebase Auth (autenticación) + Firestore (almacenamiento) + FCM (notificaciones) |
| API REST externa ≠ clima | API de IA multimodal para extracción de datos de fotos de anotaciones manuscritas (UC-10) |
