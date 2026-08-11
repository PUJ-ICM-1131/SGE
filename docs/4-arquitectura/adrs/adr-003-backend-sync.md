# ADR-003: Backend y sincronización — Local-only con backup a Google Drive

| Campo | Valor |
|---|---|
| **Estado** | Aceptada |
| **Fecha** | 2026-07-09 |
| **Decisores** | Administrador del proyecto |
| **Origen** | RNF-04 (respaldo periódico), RNF-07 (offline-first), restricción de no contar con servidor propio |

## Contexto

El proyecto necesita una estrategia de respaldo (backup) que garantice que los datos del administrador no se pierdan si el dispositivo se daña o se pierde. Además, el desarrollador no dispone de un servidor propio para alojar un backend, y cualquier servicio cloud de terceros debe ser estable a largo plazo.

Restricciones relevantes:
- **Un solo usuario** (D4): no hay sincronización multi-dispositivo.
- **Offline-first** (RNF-07): el sistema debe funcionar 100% sin conexión.
- **Sin servidor propio:** el desarrollador no puede hospedar ni mantener infraestructura.
- **Estabilidad a largo plazo:** no depender de un servicio que pueda cerrar o cambiar su modelo de precios.

## Opciones evaluadas

### Opción A: Local-only + backup automático a Google Drive (elegida)
- Room/SQLite es la fuente de verdad local.
- WorkManager programa un backup periódico que exporta la BD a Google Drive.
- Restauración: importar la BD desde Google Drive tras reinstalar la app.
- Auth: contraseña local (hash almacenado en Room).
- **Costo:** $0 (Google Drive viene con la cuenta de Android del usuario, 15 GB gratuitos).

### Opción B: Firebase (Firestore + Auth)
- BaaS de Google con sync offline nativo.
- Firestore tiene cache local que permite operación offline.
- **Contras:** dependencia de un servicio de terceros; el free tier (Spark) podría cambiar; si Google depreca Firebase, hay que migrar; agrega complejidad de configuración (consola Firebase, reglas de seguridad, etc.).

### Opción C: Supabase
- BaaS open-source con Postgres.
- Self-hostable (pero el desarrollador no tiene servidor).
- **Contras:** SDK Android menos maduro para offline; la instancia cloud gratuita pausa por inactividad; startup con menor trayectoria que Google.

## Decisión

**Local-only (Room) con backup automático a Google Drive.**

## Justificación

1. **Cero dependencias de BaaS:** no hay riesgo de cambio de precios, cierre de servicio, ni migración forzada. Los datos están siempre bajo control del cliente.
2. **Google Drive es inherente a Android:** todo dispositivo Android tiene una cuenta Google con 15 GB de Drive incluidos. No requiere crear cuenta en un servicio adicional.
3. **Un solo usuario = sin sync multi-dispositivo:** la complejidad de un backend remoto no se justifica cuando no hay sincronización entre dispositivos ni entre usuarios.
4. **WorkManager** resuelve el backup periódico de forma robusta, incluso si la app no está en primer plano.
5. **Restauración simple:** reinstalar la app + importar el backup de Drive recupera todos los datos.

## Mecanismo de backup (alto nivel)

```
┌──────────────┐    WorkManager     ┌───────────────┐
│  Room/SQLite │───(periódico)────►│ Google Drive   │
│  (fuente de  │                    │ (cuenta del    │
│   verdad)    │◄───(restaurar)────│  administrador)│
└──────────────┘                    └───────────────┘
```

1. **Backup automático:** WorkManager ejecuta periódicamente (ej. diario o al detectar cambios) una tarea que exporta la BD SQLite y la sube al Google Drive del administrador.
2. **Backup manual:** el administrador puede forzar un backup desde la configuración de la app.
3. **Restauración:** al instalar la app en un dispositivo nuevo, se ofrece importar el último backup desde Google Drive.
4. **Formato:** archivo SQLite comprimido (o JSON exportado), nombrado con fecha para mantener historial de backups.

## Consecuencias

- **No hay backend remoto** — la app es completamente autónoma.
- **Auth es local:** contraseña hasheada almacenada en Room. No hay reset de contraseña remoto; si el usuario la olvida, necesita restaurar desde backup o crear una nueva cuenta local.
- **El épico de IA (RF-09/10/11)** sí requiere conexión a internet, pero solo para la llamada a la API multimodal, no para un backend propio.
- **La exportación de reportes (RF-08)** genera archivos locales (PDF/Excel) que el usuario comparte vía Android share sheet — no depende de infraestructura.
