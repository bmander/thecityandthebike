package com.thecityandthebike

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import com.thecityandthebike.data.local.OnboardingPrefs
import com.thecityandthebike.data.network.NetworkHealthMonitor
import com.thecityandthebike.navigation.AppNavGraph
import com.thecityandthebike.ui.theme.TheCityAndTheBikeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var onboardingPrefs: OnboardingPrefs
    @Inject lateinit var networkHealthMonitor: NetworkHealthMonitor

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        networkHealthMonitor.startMonitoring()
        ComposeUiFlags.isSemanticAutofillEnabled = true
        setContent {
            TheCityAndTheBikeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavGraph(onboardingPrefs = onboardingPrefs, networkStatusProvider = networkHealthMonitor)
                }
            }
        }
    }
}
