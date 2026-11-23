package com.sge.finca.platform.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Recibe los mensajes de FCM. sge-notifier manda mensajes de solo datos con prioridad alta,
 * así que este método se ejecuta también con la app en segundo plano o cerrada.
 */
@AndroidEntryPoint
class SgeMessagingService : FirebaseMessagingService() {

    @Inject lateinit var tokenRegistrar: PushTokenRegistrar

    @Inject lateinit var transactionNotifier: TransactionPushNotifier

    @Inject lateinit var alertNotifier: AlertPushNotifier

    @Inject lateinit var chatNotifier: ChatPushNotifier

    override fun onNewToken(token: String) {
        tokenRegistrar.onNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        when (data["type"]) {
            TYPE_TRANSACTION_CREATED -> {
                val transactionId = data["transactionId"] ?: return
                transactionNotifier.show(
                    transactionId = transactionId,
                    title = data["title"] ?: "Nuevo movimiento",
                    body = data["body"] ?: "Se registró un movimiento en la finca",
                )
            }
            // Vaca fuera de los potreros (#73): al propietario, o reenviada a los trabajadores.
            TYPE_ANIMAL_OUTSIDE, TYPE_ANIMAL_OUTSIDE_FORWARDED -> {
                val alertId = data["alertId"] ?: return
                alertNotifier.show(
                    alertId = alertId,
                    title = data["title"] ?: "Una vaca salió de los potreros",
                    body = data["body"] ?: "Toca para ver dónde está",
                )
            }
            // Mensaje del chat propietario ↔ trabajador (#74).
            TYPE_CHAT_MESSAGE -> {
                val workerUid = data["workerUid"] ?: return
                chatNotifier.show(
                    workerUid = workerUid,
                    title = data["title"] ?: "Mensaje nuevo",
                    body = data["body"] ?: "",
                )
            }
            // Tipos desconocidos (versiones futuras del servidor) se ignoran.
        }
    }

    private companion object {
        const val TYPE_TRANSACTION_CREATED = "transaction_created"
        const val TYPE_ANIMAL_OUTSIDE = "animal_outside"
        const val TYPE_ANIMAL_OUTSIDE_FORWARDED = "animal_outside_forwarded"
        const val TYPE_CHAT_MESSAGE = "chat_message"
    }
}
