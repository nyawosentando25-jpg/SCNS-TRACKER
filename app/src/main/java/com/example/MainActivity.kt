package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ScnsAppRoot
import com.example.ui.theme.SCNSRecordsTheme
import com.example.ui.viewmodel.ScnsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SCNSRecordsTheme {
                val scnsViewModel: ScnsViewModel = viewModel()
                ScnsAppRoot(viewModel = scnsViewModel)
            }
        }
    }
}
