package com.codetivelab.fieldcalc

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FieldCalcApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Seed the built-in DEMO / TRAINING profiles on first launch so the app is demoable offline.
        val repo = ServiceLocator.provideRepository(this)
        appScope.launch { repo.seedIfEmpty() }
    }
}
