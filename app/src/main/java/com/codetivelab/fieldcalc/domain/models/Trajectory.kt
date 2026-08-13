package com.codetivelab.fieldcalc.domain.models

/**
 * One sampled point along a simulated trajectory. SI units throughout.
 *
 * [driftM] is the lateral displacement produced by the wind — a state variable of the simulation,
 * in metres, exactly like [heightM]. It is reported as physics, not as an aiming correction.
 */
data class TrajectoryPoint(
    val distanceM: Double,
    val heightM: Double,
    val driftM: Double,
    val velocityMs: Double,
    val timeS: Double,
    val mach: Double,
    val energyJ: Double
)

/** Ordered list of trajectory points produced by a [com.codetivelab.fieldcalc.domain.engine.SimulationEngine]. */
data class Trajectory(val points: List<TrajectoryPoint> = emptyList()) {

    val isEmpty: Boolean get() = points.isEmpty()

    /** Highest point reached, in metres relative to the launch height. */
    val maxHeightM: Double get() = points.maxOfOrNull { it.heightM } ?: 0.0

    /** Time of flight covered by the sampled points. */
    val flightTimeS: Double get() = points.lastOrNull()?.timeS ?: 0.0

    /** Furthest sampled distance. */
    val maxDistanceM: Double get() = points.lastOrNull()?.distanceM ?: 0.0

    /** Linear interpolation between the two points bracketing [distanceM]; null if out of range. */
    fun atDistance(distanceM: Double): TrajectoryPoint? {
        if (points.isEmpty()) return null
        if (distanceM <= points.first().distanceM) return points.first()
        if (distanceM >= points.last().distanceM) return points.last()

        val i = points.indexOfFirst { it.distanceM >= distanceM }.coerceAtLeast(1)
        val a = points[i - 1]
        val b = points[i]
        val span = b.distanceM - a.distanceM
        if (span <= 0.0) return b
        val t = (distanceM - a.distanceM) / span
        fun mix(x: Double, y: Double) = x + t * (y - x)
        return TrajectoryPoint(
            distanceM = distanceM,
            heightM = mix(a.heightM, b.heightM),
            driftM = mix(a.driftM, b.driftM),
            velocityMs = mix(a.velocityMs, b.velocityMs),
            timeS = mix(a.timeS, b.timeS),
            mach = mix(a.mach, b.mach),
            energyJ = mix(a.energyJ, b.energyJ)
        )
    }

    /**
     * Rows every [stepM] metres (100, 200, 300 … as in the brief's table), always including the
     * final point. Generated on demand so the table stays dynamic.
     */
    fun sampleEvery(stepM: Double): List<TrajectoryPoint> {
        if (points.isEmpty() || stepM <= 0.0) return emptyList()
        val end = points.last().distanceM
        val rows = ArrayList<TrajectoryPoint>()
        var d = stepM
        while (d < end) {
            atDistance(d)?.let { rows.add(it) }
            d += stepM
        }
        rows.add(points.last())
        return rows
    }
}

/** Derived atmospheric state. Generic atmospheric physics, computed by AtmosphereModel. */
data class AtmosphereState(
    val densityKgM3: Double,
    val speedOfSoundMs: Double,
    val pressureHpa: Double,
    val temperatureC: Double
)

/** Outcome of a simulation run. */
enum class EngineStatus {
    /** The body reached the requested range; the trajectory covers it end to end. */
    OK,

    /** The body came to ground (or the time limit) before the requested range was reached. */
    INCOMPLETE,

    /** No engine is wired in — used only by [com.codetivelab.fieldcalc.domain.engine.NullSimulationEngine]. */
    NOT_INTEGRATED
}

/**
 * Result of a SOLVE: the atmospheric state the run used, plus the sampled trajectory.
 * Contains physics/simulation quantities only.
 */
data class SolveResult(
    val atmosphere: AtmosphereState,
    val trajectory: Trajectory = Trajectory(),
    val engineStatus: EngineStatus = EngineStatus.OK,
    val requestedRangeM: Double = 0.0
) {
    /** The point at the requested range, or the last point reached if it fell short. */
    val terminal: TrajectoryPoint?
        get() = trajectory.atDistance(requestedRangeM)
}
