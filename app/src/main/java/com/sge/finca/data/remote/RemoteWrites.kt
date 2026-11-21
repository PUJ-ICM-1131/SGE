package com.sge.finca.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "SgeSync"

/**
 * Write-through: la fila ya quedó en Room (la UI la ve al instante) y aquí se manda a Firestore
 * SIN esperar la respuesta.
 *
 * Por qué no se usa await(): sin red, el Task de Firestore no termina hasta que vuelve la
 * conexión, y la pantalla se quedaría "guardando" para siempre. El SDK de Firestore guarda la
 * escritura en su cola offline y la envía solo cuando hay red.
 *
 * [onSynced] corre cuando el servidor confirma (normalmente marca la fila como SYNCED). Si la app
 * se cierra antes, la fila queda PENDING; reintentar esas filas es el issue #32.
 */
fun FirebaseFirestore.pushDocument(
    path: String,
    data: Map<String, Any?>,
    scope: CoroutineScope,
    onSynced: suspend () -> Unit,
) {
    document(path).set(data)
        .addOnSuccessListener { scope.launch { onSynced() } }
        .addOnFailureListener { e -> Log.w(TAG, "No se pudo sincronizar $path", e) }
}

/** Borrado remoto, con la misma lógica de "enviar y no esperar". */
fun FirebaseFirestore.deleteDocument(path: String) {
    document(path).delete()
        .addOnFailureListener { e -> Log.w(TAG, "No se pudo borrar $path", e) }
}
