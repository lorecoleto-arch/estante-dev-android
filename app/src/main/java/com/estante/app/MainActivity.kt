package com.estante.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.estante.app.ui.EstanteApp
import com.estante.app.ui.theme.EstanteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as EstanteApplication).container
        setContent {
            EstanteTheme {
                EstanteApp(container)
            }
        }
    }
}
