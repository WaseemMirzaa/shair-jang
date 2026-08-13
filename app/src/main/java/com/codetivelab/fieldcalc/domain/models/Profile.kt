package com.codetivelab.fieldcalc.domain.models

/**
 * A simulation profile. Admin-editable; operators may only SELECT one.
 *
 * NOTE ON THE PROJECTILE / DRAG FIELDS BELOW:
 * These are plain data holders. This app ships WITHOUT a ballistic solver — the
 * [com.codetivelab.fieldcalc.domain.engine.SimulationEngine] is an interface with a
 * non-computing stub. These fields exist so that whoever integrates a real physics/education
 * engine has a place to read parameters from. Nothing in this project turns them into a
 * firing solution. See engine/SimulationEngine.kt and the README.
 */
data class Profile(
    val id: Long = 0L,
    val name: String,
    val isDemo: Boolean = false,
    val isLocked: Boolean = true,
    val projectile: ProjectileSpec = ProjectileSpec(),
    val geometry: ReferenceGeometry = ReferenceGeometry(),
    val environmentDefaults: EnvironmentDefaults = EnvironmentDefaults(),
    val unitSystem: UnitSystem = UnitSystem.METRIC
)

/** Generic projectile descriptor. Units are SI (kg, m, m/s). */
data class ProjectileSpec(
    val calibreMm: Double = 0.0,
    val massKg: Double = 0.0,
    val lengthM: Double = 0.0,
    val nominalLaunchVelocityMs: Double = 0.0,
    /** Free-form identifier for whatever drag model an integrated engine expects (e.g. "GENERIC"). */
    val dragModelId: String = "GENERIC",
    /** Opaque aerodynamic parameter bag for an integrated engine; unused by the stub. */
    val aeroParams: Map<String, Double> = emptyMap()
)

/** Reference geometry / simulator configuration placeholders. */
data class ReferenceGeometry(
    val referenceHeightM: Double = 0.0,
    val referenceDistanceM: Double = 0.0,
    val simulatorConfig: String = "DEFAULT"
)

data class EnvironmentDefaults(
    val temperatureC: Double = 15.0,
    val humidityPct: Double = 50.0,
    val altitudeM: Double = 0.0
)
