package com.codetivelab.fieldcalc.navigation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.codetivelab.fieldcalc.ui.admin.AdminScreen
import com.codetivelab.fieldcalc.ui.calculator.CalculatorScreen
import com.codetivelab.fieldcalc.ui.calculator.CalculatorViewModel
import com.codetivelab.fieldcalc.ui.profile.ProfileScreen
import com.codetivelab.fieldcalc.ui.results.ResultsScreen
import com.codetivelab.fieldcalc.ui.settings.SettingsScreen

object Routes {
    const val CALCULATOR = "calculator"
    const val RESULTS = "results"
    const val PROFILES = "profiles"
    const val SETTINGS = "settings"
    const val ADMIN = "admin"
}

@Composable
fun FieldCalcNavHost() {
    val nav = rememberNavController()
    val activity = LocalContext.current as ComponentActivity
    // Shared across CALCULATOR and RESULTS so the solve result survives navigation.
    val calcVm: CalculatorViewModel = viewModel(viewModelStoreOwner = activity)

    NavHost(navController = nav, startDestination = Routes.CALCULATOR) {
        composable(Routes.CALCULATOR) {
            CalculatorScreen(
                onOpenResults = { nav.navigate(Routes.RESULTS) },
                onOpenProfiles = { nav.navigate(Routes.PROFILES) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                onOpenAdmin = { nav.navigate(Routes.ADMIN) },
                vm = calcVm
            )
        }
        composable(Routes.RESULTS) {
            ResultsScreen(
                vm = calcVm,
                onNewInput = { nav.popBackStack(Routes.CALCULATOR, inclusive = false) },
                onBack = { nav.popBackStack() }
            )
        }
        composable(Routes.PROFILES) { ProfileScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.SETTINGS) { SettingsScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.ADMIN) { AdminScreen(onBack = { nav.popBackStack() }) }
    }
}
