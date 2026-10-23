package com.sge.finca.ui.screens.splash

data class SplashState(
    val navigateHome: Boolean = false,
    val navigateLogin: Boolean = false,
    /** true mientras hay que mostrar (o está mostrándose) el prompt de huella. */
    val showBiometricPrompt: Boolean = false,
)
