package com.sge.finca.data.local

/**
 * Estado de sincronización de una fila de Room con Firestore.
 *
 * - PENDING: el cambio está en el celular pero Firestore todavía no confirmó la escritura
 *   (sin red, o la app se cerró antes de la confirmación).
 * - SYNCED: Firestore confirmó la última versión de la fila.
 *
 * Room lo guarda como TEXT con el nombre del valor ("PENDING"/"SYNCED").
 * Reintentar las filas PENDING al recuperar conexión es parte del issue #32.
 */
enum class SyncStatus { PENDING, SYNCED }
