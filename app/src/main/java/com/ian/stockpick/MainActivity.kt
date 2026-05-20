package com.ian.stockpick

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ian.stockpick.screen.EditorViewModel
import com.ian.stockpick.ui.screen.EditorScreen
import com.ian.stockpick.ui.theme.StockPickTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as StockPickApp
        enableEdgeToEdge()
        setContent {
            StockPickTheme {
                val vm: EditorViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return EditorViewModel(app.runScreenUseCase, app.repository) as T
                        }
                    },
                )
                EditorScreen(viewModel = vm)
            }
        }
    }
}
