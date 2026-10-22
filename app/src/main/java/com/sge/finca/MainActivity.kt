package com.sge.finca

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.sge.finca.ui.theme.SGEFincaTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Extiende FragmentActivity (que a su vez extiende ComponentActivity) porque
 * BiometricPrompt necesita una FragmentActivity para mostrarse.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SGEFincaTheme {
                FincaApp()
            }
        }
    }
}
