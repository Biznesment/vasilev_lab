package com.vasilev.beeropener

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.vasilev.beeropener.ui.BeerOpenerRoute
import com.vasilev.beeropener.ui.theme.BeerOpenerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BeerOpenerTheme {
                BeerOpenerRoute()
            }
        }
    }
}
