package com.aire.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aire.data.IntegrationManager
import com.aire.data.LocationProvider
import com.aire.data.MemoryDatabase
import com.aire.data.SettingsRepository
import com.aire.ui.theme.AireTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val dao = MemoryDatabase.get(applicationContext).dao()
        val settings = SettingsRepository(applicationContext)
        val locationProvider = LocationProvider(applicationContext)
        val integrationManager = IntegrationManager(applicationContext)
        
        setContent {
            val vm: MemoryViewModel = viewModel(factory = MemoryViewModel.Factory(dao, settings, locationProvider, integrationManager))
            val uiState by vm.uiState.collectAsState()

            AireTheme(appearance = uiState.appearance) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Crossfade(targetState = uiState.currentScreen, label = "ScreenTransition") { screen ->
                        when (screen) {
                            AppScreen.HOME -> AssistantScreen(vm)
                            AppScreen.LENS -> LensScreen(
                                onCaptured = { vm.onImageCaptured(it) },
                                onClose = { vm.navigateTo(AppScreen.HOME) }
                            )
                            AppScreen.SETTINGS -> SettingsScreen(vm)
                        }
                    }
                }
            }
        }
    }
}
