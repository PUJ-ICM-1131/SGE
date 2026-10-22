package com.sge.finca.ui.components

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.LifecycleResumeEffect

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShakeHandler(
    onStart: (() -> Unit) -> Boolean,
    onStop: () -> Unit,
    onShake: () -> Unit,
    enabled: Boolean = true,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val keyboardVisible = WindowInsets.isImeVisible
    val shouldListen = enabled && !keyboardVisible

    val currentStart by rememberUpdatedState(onStart)
    val currentStop by rememberUpdatedState(onStop)
    val currentOnShake by rememberUpdatedState(onShake)
    val currentEnabled by rememberUpdatedState(shouldListen)

    LifecycleResumeEffect(shouldListen) {
        var active = shouldListen
        var handled = false

        val registered = if (shouldListen) {
            currentStart {
                if (active && currentEnabled && !handled) {
                    handled = true

                    hapticFeedback.performHapticFeedback(
                        HapticFeedbackType.LongPress,
                    )

                    currentOnShake()
                }
            }
        } else {
            false
        }

        onPauseOrDispose {
            active = false

            if (registered) {
                currentStop()
            }
        }
    }
}