package com.codetivelab.fieldcalc.domain.engine

import com.codetivelab.fieldcalc.domain.atmosphere.AtmosphereModel
import com.codetivelab.fieldcalc.domain.models.AtmosphereState
import com.codetivelab.fieldcalc.domain.models.ClockDirection
import com.codetivelab.fieldcalc.domain.models.EngineStatus
import com.codetivelab.fieldcalc.domain.models.OperatorInput
import com.codetivelab.fieldcalc.domain.models.Profile
import com.codetivelab.fieldcalc.domain.models.SolveResult
import com.codetivelab.fieldcalc.domain.models.Trajectory
import com.codetivelab.fieldcalc.domain.models.TrajectoryPoint
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Tunable knobs of the simulation. All of it is configurable per the brief (§12, §13). */
data class EngineConfig(
    /** Integration step in seconds. */
    val timeStepS: Double = 0.001,
    /** Hard stop so a badly conditioned input can never spin forever. */
    val maxFlightTimeS: Double = 60.0,
    /** How many points the sampled [Trajectory] contains. */
    val sampleCount: Int = 120,
    val method: IntegrationMethod = IntegrationMethod.RK4,
    /** Stop once the body has fallen this far below the launch point. */
    val maxDropM: Double = 500.0
) {
    init {
        require(timeStepS > 0.0) { "timeStepS must be positive" }
        require(sampleCount >= 2) { "sampleCount must be at least 2" }
    }
}

/**
 * Generic educational point-mass ("3-DOF") trajectory simulator.
 *
 * Physics, in one line each:
 *  - gravity acts downward at g
 *  - aerodynamic drag opposes the velocity **relative to the moving air**, with magnitude
 *    ½·ρ·Cd(M)·A·|v_rel|²
 *  - the resulting acceleration is integrated forward in time (RK4 by default)
 *
 * It reports what the body does — position, speed, Mach, kinetic energy, time — and nothing else.
 * No sighting, aiming, hold or correction output of any kind is produced or intended; this is the
 * same model used to teach projectile motion with air resistance.
 *
 * Contains no Android types, so it ports unchanged to ESP32 / Raspberry Pi / dedicated hardware.
 */
class TrajectorySimulationEngine(
    private val config: EngineConfig = EngineConfig()
) : SimulationEngine {

    override suspend fun solve(profile: Profile, input: OperatorInput): SolveResult =
        solveBlocking(profile, input)

    /**
     * Blocking entry point — handy for tests, command-line harnesses and future non-Android hosts
     * that have no coroutines available.
     */
    fun solveBlocking(profile: Profile, input: OperatorInput): SolveResult {
        val atmosphere = AtmosphereModel.calculateAtmosphere(
            altitudeM = input.altitudeM,
            temperatureC = input.temperatureC,
            humidityPct = input.humidityPct
        )
        val (trajectory, reachedRange) = simulate(profile, input, atmosphere)
        return SolveResult(
            atmosphere = atmosphere,
            trajectory = trajectory,
            engineStatus = if (reachedRange) EngineStatus.OK else EngineStatus.INCOMPLETE,
            requestedRangeM = input.rangeM
        )
    }

    // ---------------------------------------------------------------------------------------
    // Core integration
    // ---------------------------------------------------------------------------------------

    /** @return the sampled trajectory and whether the requested range was actually reached. */
    private fun simulate(
        profile: Profile,
        input: OperatorInput,
        atmosphere: AtmosphereState
    ): Pair<Trajectory, Boolean> {

        // No range means nothing to simulate. The validator normally stops this earlier; the guard
        // keeps the engine well-defined for any caller (tests, future hardware hosts).
        if (input.rangeM <= 0.0) return Trajectory(emptyList()) to false

        val spec = profile.projectile
        val mass = spec.massKg.takeIf { it > 0.0 } ?: DEFAULT_MASS_KG
        val launchSpeed = spec.nominalLaunchVelocityMs.takeIf { it > 0.0 } ?: DEFAULT_LAUNCH_SPEED_MS
        val area = referenceArea(spec.calibreMm)
        val drag = DragModels.forId(spec.dragModelId, spec.aeroParams)
        val wind = windVector(input.windSpeedMs, input.windDirection)
        val targetRange = input.rangeM.coerceAtLeast(0.0)

        // Launch along the line of departure: elevation = the operator's inclination angle.
        val elevation = input.inclinationDeg * PI / 180.0
        val startHeight = profile.geometry.referenceHeightM

        var state = MotionState(
            position = Vec3(0.0, startHeight, 0.0),
            velocity = Vec3(launchSpeed * cos(elevation), launchSpeed * sin(elevation), 0.0)
        )

        val accel = Integrator.Acceleration { s ->
            val relative = s.velocity - wind
            val speed = relative.length
            if (speed <= 0.0) return@Acceleration GRAVITY_VECTOR
            val mach = speed / atmosphere.speedOfSoundMs
            val cd = drag.dragCoefficient(mach)
            val decel = 0.5 * atmosphere.densityKgM3 * cd * area * speed * speed / mass
            relative.normalized() * (-decel) + GRAVITY_VECTOR
        }

        // Integrate, keeping the raw states so we can interpolate an evenly-spaced sample later.
        val states = ArrayList<Sample>(4096)
        states.add(Sample(0.0, state))

        var t = 0.0
        val dt = config.timeStepS
        val floorHeight = startHeight - config.maxDropM
        var reachedRange = targetRange <= 0.0

        while (t < config.maxFlightTimeS) {
            val next = Integrator.step(state, dt, config.method, accel)
            t += dt
            state = next
            states.add(Sample(t, state))

            if (state.position.x >= targetRange && targetRange > 0.0) { reachedRange = true; break }
            if (state.position.y < floorHeight) break
            // A body that has stopped moving downrange can never reach the target.
            if (state.velocity.x <= 0.0) break
        }

        val trajectory = sample(states, targetRange, reachedRange, mass, atmosphere, startHeight)
        return trajectory to reachedRange
    }

    /** Resample the raw integration steps onto evenly spaced distances. */
    private fun sample(
        states: List<Sample>,
        targetRange: Double,
        reachedRange: Boolean,
        mass: Double,
        atmosphere: AtmosphereState,
        startHeight: Double
    ): Trajectory {
        if (states.size < 2) return Trajectory(emptyList())

        val end = if (reachedRange && targetRange > 0.0) targetRange else states.last().state.position.x
        if (end <= 0.0) return Trajectory(emptyList())

        val step = end / (config.sampleCount - 1)
        val points = ArrayList<TrajectoryPoint>(config.sampleCount)
        var cursor = 0

        for (i in 0 until config.sampleCount) {
            val distance = if (i == config.sampleCount - 1) end else i * step
            while (cursor < states.size - 2 && states[cursor + 1].state.position.x < distance) cursor++

            val a = states[cursor]
            val b = states[cursor + 1]
            val span = b.state.position.x - a.state.position.x
            val f = if (span > 0.0) ((distance - a.state.position.x) / span).coerceIn(0.0, 1.0) else 0.0

            val position = a.state.position + (b.state.position - a.state.position) * f
            val velocity = a.state.velocity + (b.state.velocity - a.state.velocity) * f
            val time = a.timeS + f * (b.timeS - a.timeS)
            val speed = velocity.length

            points.add(
                TrajectoryPoint(
                    distanceM = distance,
                    heightM = position.y - startHeight,
                    driftM = position.z,
                    velocityMs = speed,
                    timeS = time,
                    mach = speed / atmosphere.speedOfSoundMs,
                    energyJ = 0.5 * mass * speed * speed
                )
            )
        }
        return Trajectory(points)
    }

    private data class Sample(val timeS: Double, val state: MotionState)

    companion object {
        const val GRAVITY_MS2 = 9.80665
        private val GRAVITY_VECTOR = Vec3(0.0, -GRAVITY_MS2, 0.0)

        /** Used when a profile leaves the field blank, so the demo profile always runs. */
        const val DEFAULT_MASS_KG = 0.010
        const val DEFAULT_LAUNCH_SPEED_MS = 800.0
        const val DEFAULT_CALIBRE_MM = 7.62

        /** Frontal reference area from the calibre (a circle of that diameter), in m². */
        fun referenceArea(calibreMm: Double): Double {
            val mm = calibreMm.takeIf { it > 0.0 } ?: DEFAULT_CALIBRE_MM
            val radiusM = mm / 2000.0
            return PI * radiusM * radiusM
        }

        /**
         * Wind velocity vector from the clock-face direction.
         *
         * The clock names where the wind blows *from*: 12 o'clock is a headwind coming from the
         * target, 6 o'clock a tailwind, 3 o'clock blows from the right across to the left.
         */
        fun windVector(speedMs: Double, direction: ClockDirection): Vec3 {
            if (speedMs == 0.0) return Vec3.ZERO
            val bearing = direction.bearingDegrees * PI / 180.0
            // Unit vector pointing toward the source of the wind, then reversed to get its velocity.
            val source = Vec3(cos(bearing), 0.0, sin(bearing))
            return source * -speedMs
        }
    }
}
