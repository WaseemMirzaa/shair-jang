package com.codetivelab.fieldcalc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.codetivelab.fieldcalc.data.prefs.SettingsStore
import com.codetivelab.fieldcalc.navigation.FieldCalcNavHost
import com.codetivelab.fieldcalc.ui.theme.FieldCalcTheme
import com.codetivelab.fieldcalc.ui.theme.RuggedBlack

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = ServiceLocator.provideSettings(this)
        setContent {
            val s by settings.settings.collectAsState(initial = SettingsStore.Settings())
            FieldCalcTheme(theme = s.theme) {
                Surface(Modifier.fillMaxSize().background(RuggedBlack)) {
                    FieldCalcNavHost()
                }
            }
        }
    }
}
