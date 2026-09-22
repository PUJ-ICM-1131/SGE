package com.sge.finca

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sge.finca.navigation.AppNavigation
import com.sge.finca.ui.theme.SGEFincaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SGEFincaTheme {
                AppNavigation()
            }
        }
    }
}
