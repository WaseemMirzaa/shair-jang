package com.codetivelab.fieldcalc

import android.content.Context
import com.codetivelab.fieldcalc.data.database.AppDatabase
import com.codetivelab.fieldcalc.data.prefs.SettingsStore
import com.codetivelab.fieldcalc.data.repository.ProfileRepository
import com.codetivelab.fieldcalc.domain.engine.EngineConfig
import com.codetivelab.fieldcalc.domain.engine.SimulationEngine
import com.codetivelab.fieldcalc.domain.engine.TrajectorySimulationEngine

/**
 * Tiny manual dependency container (kept deliberately simple instead of a DI framework).
 *
 * >>> ENGINE SEAM <<<
 * [provideEngine] is the ONLY place the app names a concrete engine. Swapping in a different
 * [SimulationEngine] — a tuned build, a hardware-backed one, or a stub for tests — is a one-line
 * change here and nothing else in the app has to know.
 */
object ServiceLocator {

    @Volatile private var repository: ProfileRepository? = null
    @Volatile private var settings: SettingsStore? = null
    @Volatile private var engine: SimulationEngine? = null

    fun provideRepository(context: Context): ProfileRepository =
        repository ?: synchronized(this) {
            repository ?: ProfileRepository(AppDatabase.get(context).profileDao()).also { repository = it }
        }

    fun provideSettings(context: Context): SettingsStore =
        settings ?: synchronized(this) {
            settings ?: SettingsStore(context.applicationContext).also { settings = it }
        }

    fun provideEngine(): SimulationEngine =
        engine ?: synchronized(this) {
            engine ?: TrajectorySimulationEngine(EngineConfig()).also { engine = it }
        }

    /** Test hook: install a different engine (or null to restore the default). */
    fun setEngine(replacement: SimulationEngine?) = synchronized(this) { engine = replacement }
}
