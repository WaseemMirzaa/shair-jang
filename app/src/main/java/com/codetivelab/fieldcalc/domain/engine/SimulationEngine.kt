package com.codetivelab.fieldcalc.domain.engine

import com.codetivelab.fieldcalc.domain.atmosphere.AtmosphereModel
import com.codetivelab.fieldcalc.domain.models.EngineStatus
import com.codetivelab.fieldcalc.domain.models.OperatorInput
import com.codetivelab.fieldcalc.domain.models.Profile
import com.codetivelab.fieldcalc.domain.models.SolveResult
import com.codetivelab.fieldcalc.domain.models.Trajectory

/**
 * Boundary between the app and the simulation.
 *
 * The UI, the database, the unit system and the atmospheric model all talk to this interface and
 * never to a concrete engine, which is what keeps the long-term port ("Android UI → Simulation
 * Engine API → physics modules", later "ESP32 UI → the same API") a drop-in swap.
 *
 * The engine shipped in the app is [TrajectorySimulationEngine]. Implementations must not block the
 * UI thread — [solve] is a suspend function and the ViewModel calls it on a background dispatcher.
 */
interface SimulationEngine {
    suspend fun solve(profile: Profile, input: OperatorInput): SolveResult
}

/**
 * Engine that computes the atmospheric state but no trajectory. Useful for UI tests and for hosts
 * where the integrator is unavailable; the results screen renders it as "ENGINE NOT LOADED".
 */
class NullSimulationEngine : SimulationEngine {
    override suspend fun solve(profile: Profile, input: OperatorInput): SolveResult = SolveResult(
        atmosphere = AtmosphereModel.calculateAtmosphere(
            altitudeM = input.altitudeM,
            temperatureC = input.temperatureC,
            humidityPct = input.humidityPct
        ),
        trajectory = Trajectory(emptyList()),
        engineStatus = EngineStatus.NOT_INTEGRATED,
        requestedRangeM = input.rangeM
    )
}
