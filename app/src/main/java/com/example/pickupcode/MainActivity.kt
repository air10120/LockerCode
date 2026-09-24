package com.example.pickupcode

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.pickupcode.ui.compose.AppRoot
import com.example.pickupcode.ui.compose.theme.PickupCodeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PickupCodeTheme {
                Surface(
                    color = MaterialTheme.colorScheme.surface
                ) {
                    AppRoot()
                }
            }
        }
    }
}
