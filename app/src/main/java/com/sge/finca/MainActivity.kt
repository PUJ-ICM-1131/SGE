package com.sge.finca

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import com.sge.finca.navigation.Screen
import com.sge.finca.platform.notifications.NotificationDestinations
import com.sge.finca.ui.theme.SGEFincaTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Extiende FragmentActivity (que a su vez extiende ComponentActivity) porque
 * BiometricPrompt necesita una FragmentActivity para mostrarse.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    /** Pantalla que pidió abrir una notificación; FincaApp la abre al llegar al inicio. */
    private val pendingRoute = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Solo en el primer arranque: al rotar no se debe volver a abrir.
        if (savedInstanceState == null) pendingRoute.value = routeFrom(intent)

        setContent {
            val route by pendingRoute.collectAsState()
            SGEFincaTheme {
                FincaApp(
                    pendingRoute = route,
                    onPendingRouteHandled = { pendingRoute.value = null },
                )
            }
        }
    }

    /** La app ya estaba abierta y el usuario tocó una notificación. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        routeFrom(intent)?.let { pendingRoute.value = it }
    }

    private fun routeFrom(intent: Intent?): String? =
        when (intent?.getStringExtra(NotificationDestinations.EXTRA_DESTINATION)) {
            NotificationDestinations.REMINDERS -> Screen.Reminders.route
            NotificationDestinations.TRANSACTION ->
                intent.getStringExtra(NotificationDestinations.EXTRA_TRANSACTION_ID)
                    ?.let(Screen.TransactionDetail::createRoute)
            NotificationDestinations.ALERT ->
                intent.getStringExtra(NotificationDestinations.EXTRA_ALERT_ID)
                    ?.let(Screen.AlertDetail::createRoute)
            else -> null
        }
}
