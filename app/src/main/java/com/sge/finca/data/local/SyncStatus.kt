package com.sge.finca.data.local

/**
 * Estado de sincronización de una fila de Room con Firestore (issue #32).
 *
 * - PENDING: el cambio está en el celular y falta subirlo. SyncWorker lo sube cuando hay red
 *   y lo reintenta (con espera creciente) si falla por red o tiempo de espera.
 * - SYNCED: Firestore confirmó la última versión de la fila, o la fila llegó desde Firestore.
 * - FAILED: Firestore la rechazó de forma definitiva (Security Rules o datos inválidos).
 *   No se reintenta sola para no quedar en un ciclo infinito: vuelve a PENDING si el usuario
 *   la edita o pulsa "Reintentar" en el inicio.
 *
 * Room lo guarda como TEXT con el nombre del valor ("PENDING"/"SYNCED"/"FAILED").
 */
enum class SyncStatus { PENDING, SYNCED, FAILED }
