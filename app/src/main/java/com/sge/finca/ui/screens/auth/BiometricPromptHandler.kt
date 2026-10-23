package com.sge.finca.ui.screens.auth

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity

/** Busca la FragmentActivity que contiene este Context (BiometricPrompt la necesita). */
fun Context.findFragmentActivity(): FragmentActivity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is FragmentActivity) return current
        current = current.baseContext
    }
    return null
}

/**
 * Puente entre Compose y BiometricPrompt: cuando [show] pasa a true, entrega la actividad a
 * [onLaunch], que es quien muestra el prompt. No dibuja nada.
 *
 * Si no se encuentra una FragmentActivity (no debería pasar) se llama a [onUnavailable]
 * para que el flujo caiga al login normal en lugar de quedarse esperando.
 */
@Composable
fun BiometricPromptHandler(
    show: Boolean,
    onLaunch: (FragmentActivity) -> Unit,
    onUnavailable: () -> Unit = {},
) {
    val context = LocalContext.current
    LaunchedEffect(show) {
        if (show) {
            val activity = context.findFragmentActivity()
            if (activity != null) onLaunch(activity) else onUnavailable()
        }
    }
}
