package com.codetivelab.fieldcalc

import com.codetivelab.fieldcalc.domain.engine.EngineConfig
import com.codetivelab.fieldcalc.domain.engine.IntegrationMethod
import com.codetivelab.fieldcalc.domain.engine.TrajectorySimulationEngine
import com.codetivelab.fieldcalc.domain.models.ClockDirection
import com.codetivelab.fieldcalc.domain.models.EngineStatus
import com.codetivelab.fieldcalc.domain.models.OperatorInput
import com.codetivelab.fieldcalc.domain.models.Profile
import com.codetivelab.fieldcalc.domain.models.ProjectileSpec
import com.codetivelab.fieldcalc.domain.models.ReferenceGeometry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

class TrajectoryEngineTest {

    // A vacuum profile: constant drag model with cd = 0 removes all aerodynamic force, so the
    // simulation must reproduce the analytic parabola exactly.
    private fun vacuumProfile(v0: Double = 300.0) = Profile(
        name = "VACUUM",
        projectile = ProjectileSpec(
            calibreMm = 7.62, massKg = 0.01, nominalLaunchVelocityMs = v0,
            dragModelId = "CONSTANT", aeroParams = mapOf("cd" to 0.0)
        ),
        geometry = ReferenceGeometry(referenceHeightM = 0.0)
    )

    private fun genericProfile(v0: Double = 800.0) = Profile(
        name = "GENERIC",
        projectile = ProjectileSpec(
            calibreMm = 7.62, massKg = 0.010, nominalLaunchVelocityMs = v0, dragModelId = "GENERIC"
        ),
        geometry = ReferenceGeometry(referenceHeightM = 0.0)
    )

    private fun input(
        range: Double = 650.0, wind: Double = 0.0, clock: Int = 3,
        temp: Double = 15.0, alt: Double = 0.0, humidity: Double = 0.0, incl: Double = 0.0
    ) = OperatorInput(
        rangeM = range, windSpeedMs = wind, windDirection = ClockDirection(clock),
        temperatureC = temp, altitudeM = alt, humidityPct = humidity, inclinationDeg = incl
    )

    // ---- Numerical integration accuracy -------------------------------------------------

    @Test
    fun vacuumTrajectory_matchesAnalyticParabola() {
        val v0 = 300.0
        val angleDeg = 10.0
        val engine = TrajectorySimulationEngine(EngineConfig(timeStepS = 0.001))
        val result = engine.solveBlocking(vacuumProfile(v0), input(range = 500.0, incl = angleDeg))

        val a = angleDeg * PI / 180.0
        val g = TrajectorySimulationEngine.GRAVITY_MS2

        result.trajectory.points.forEach { p ->
            // y = x·tan(a) − g·x² / (2·v0²·cos²a)
            val expected = p.distanceM * tan(a) -
                g * p.distanceM * p.distanceM / (2 * v0 * v0 * cos(a) * cos(a))
            assertEquals("height at ${p.distanceM} m", expected, p.heightM, 1e-3)
        }
    }

    @Test
    fun vacuumTrajectory_conservesTotalEnergy() {
        val v0 = 300.0
        val mass = 0.01
        val engine = TrajectorySimulationEngine(EngineConfig(timeStepS = 0.001))
        val result = engine.solveBlocking(vacuumProfile(v0), input(range = 500.0, incl = 15.0))

        val initial = 0.5 * mass * v0 * v0
        result.trajectory.points.forEach { p ->
            val total = p.energyJ + mass * TrajectorySimulationEngine.GRAVITY_MS2 * p.heightM
            assertEquals("total energy at ${p.distanceM} m", initial, total, initial * 1e-6)
        }
    }

    @Test
    fun rk4_isMoreAccurateThanEuler_atTheSameStep() {
        val v0 = 300.0
        val angleDeg = 10.0
        val a = angleDeg * PI / 180.0
        val g = TrajectorySimulationEngine.GRAVITY_MS2
        fun analytic(x: Double) = x * tan(a) - g * x * x / (2 * v0 * v0 * cos(a) * cos(a))

        fun errorAtEnd(method: IntegrationMethod): Double {
            val engine = TrajectorySimulationEngine(EngineConfig(timeStepS = 0.05, method = method))
            val last = engine.solveBlocking(vacuumProfile(v0), input(range = 500.0, incl = angleDeg))
                .trajectory.points.last()
            return kotlin.math.abs(last.heightM - analytic(last.distanceM))
        }

        assertTrue("RK4 should beat Euler", errorAtEnd(IntegrationMethod.RK4) < errorAtEnd(IntegrationMethod.EULER))
    }

    @Test
    fun stepSizeIndependence_rk4() {
        val coarse = TrajectorySimulationEngine(EngineConfig(timeStepS = 0.01))
            .solveBlocking(genericProfile(), input()).trajectory.points.last()
        val fine = TrajectorySimulationEngine(EngineConfig(timeStepS = 0.0005))
            .solveBlocking(genericProfile(), input()).trajectory.points.last()

        assertEquals(fine.velocityMs, coarse.velocityMs, 0.1)
        assertEquals(fine.heightM, coarse.heightM, 0.01)
    }

    // ---- Trajectory shape and derived values --------------------------------------------

    @Test
    fun trajectory_isSampledAcrossTheRequestedRange() {
        val engine = TrajectorySimulationEngine(EngineConfig(sampleCount = 50))
        val result = engine.solveBlocking(genericProfile(), input(range = 650.0))

        assertEquals(EngineStatus.OK, result.engineStatus)
        assertEquals(50, result.trajectory.points.size)
        assertEquals(0.0, result.trajectory.points.first().distanceM, 1e-9)
        assertEquals(650.0, result.trajectory.points.last().distanceM, 1e-6)
    }

    @Test
    fun distanceAndTime_increaseMonotonically() {
        val result = TrajectorySimulationEngine().solveBlocking(genericProfile(), input(range = 1000.0))
        result.trajectory.points.zipWithNext { a, b ->
            assertTrue("distance must increase", b.distanceM > a.distanceM)
            assertTrue("time must increase", b.timeS > a.timeS)
        }
    }

    @Test
    fun drag_slowsTheBodyDown() {
        val withDrag = TrajectorySimulationEngine().solveBlocking(genericProfile(800.0), input(range = 800.0))
        val noDrag = TrajectorySimulationEngine().solveBlocking(vacuumProfile(800.0), input(range = 800.0))

        val dragged = withDrag.trajectory.points.last().velocityMs
        val free = noDrag.trajectory.points.last().velocityMs
        assertTrue("drag must reduce the remaining velocity ($dragged vs $free)", dragged < free - 50.0)
        assertTrue("velocity must stay positive", dragged > 0.0)
    }

    @Test
    fun velocityAndEnergy_decayAlongTheFlight() {
        val result = TrajectorySimulationEngine().solveBlocking(genericProfile(), input(range = 800.0))
        val first = result.trajectory.points.first()
        val last = result.trajectory.points.last()

        assertTrue(last.velocityMs < first.velocityMs)
        assertTrue(last.energyJ < first.energyJ)
    }

    @Test
    fun machNumber_isConsistentWithSpeedOfSound() {
        val result = TrajectorySimulationEngine().solveBlocking(genericProfile(), input(range = 600.0))
        result.trajectory.points.forEach { p ->
            assertEquals(p.velocityMs / result.atmosphere.speedOfSoundMs, p.mach, 1e-9)
        }
    }

    @Test
    fun kineticEnergy_matchesHalfMVSquared() {
        val mass = 0.010
        val result = TrajectorySimulationEngine().solveBlocking(genericProfile(), input(range = 600.0))
        result.trajectory.points.forEach { p ->
            assertEquals(0.5 * mass * p.velocityMs * p.velocityMs, p.energyJ, 1e-6)
        }
    }

    // ---- Environmental influences ---------------------------------------------------------

    @Test
    fun thinnerAir_preservesMoreVelocity() {
        val seaLevel = TrajectorySimulationEngine()
            .solveBlocking(genericProfile(), input(range = 800.0, alt = 0.0, temp = 15.0))
        val highAltitude = TrajectorySimulationEngine()
            .solveBlocking(genericProfile(), input(range = 800.0, alt = 3000.0, temp = 15.0))

        assertTrue(highAltitude.atmosphere.densityKgM3 < seaLevel.atmosphere.densityKgM3)
        assertTrue(
            "less dense air means less drag",
            highAltitude.trajectory.points.last().velocityMs > seaLevel.trajectory.points.last().velocityMs
        )
    }

    @Test
    fun headwindAndTailwind_pushInOppositeDirections() {
        fun terminalVelocity(clock: Int) = TrajectorySimulationEngine()
            .solveBlocking(genericProfile(), input(range = 800.0, wind = 20.0, clock = clock))
            .trajectory.points.last().velocityMs

        val head = terminalVelocity(12)   // blowing back from the target
        val tail = terminalVelocity(6)    // blowing from behind
        assertTrue("a headwind must cost more speed than a tailwind", head < tail)
    }

    @Test
    fun crosswind_pushesTheBodyDownwind() {
        // 3 o'clock: the wind comes from the right, so the body drifts to the left (negative z).
        val fromRight = TrajectorySimulationEngine()
            .solveBlocking(genericProfile(), input(range = 800.0, wind = 10.0, clock = 3))
            .trajectory.points.last().driftM
        // 9 o'clock: the mirror image.
        val fromLeft = TrajectorySimulationEngine()
            .solveBlocking(genericProfile(), input(range = 800.0, wind = 10.0, clock = 9))
            .trajectory.points.last().driftM

        assertTrue("3 o'clock wind must drift left", fromRight < 0.0)
        assertTrue("9 o'clock wind must drift right", fromLeft > 0.0)
        assertEquals("mirrored winds must mirror the drift", fromRight, -fromLeft, 1e-6)
    }

    @Test
    fun noWind_producesNoLateralDrift() {
        val result = TrajectorySimulationEngine().solveBlocking(genericProfile(), input(range = 800.0, wind = 0.0))
        result.trajectory.points.forEach { assertEquals(0.0, it.driftM, 1e-12) }
    }

    @Test
    fun positiveInclination_liftsTheTrajectory() {
        val level = TrajectorySimulationEngine()
            .solveBlocking(genericProfile(), input(range = 600.0, incl = 0.0)).trajectory.maxHeightM
        val uphill = TrajectorySimulationEngine()
            .solveBlocking(genericProfile(), input(range = 600.0, incl = 8.0)).trajectory.maxHeightM

        assertTrue(uphill > level)
    }

    @Test
    fun launchHeight_isTheTrajectoryOrigin() {
        val profile = genericProfile().copy(geometry = ReferenceGeometry(referenceHeightM = 1.5))
        val result = TrajectorySimulationEngine().solveBlocking(profile, input(range = 400.0))
        // Heights are reported relative to the launch point, so the first sample is zero.
        assertEquals(0.0, result.trajectory.points.first().heightM, 1e-9)
    }

    // ---- Robustness -----------------------------------------------------------------------

    @Test
    fun rangeBeyondReach_isReportedAsIncomplete() {
        // A slow, draggy sphere cannot cover 5 km before hitting the ground.
        val sphere = Profile(
            name = "SPHERE",
            projectile = ProjectileSpec(
                calibreMm = 60.0, massKg = 0.5, nominalLaunchVelocityMs = 60.0, dragModelId = "SPHERE"
            ),
            geometry = ReferenceGeometry(referenceHeightM = 1.5)
        )
        val result = TrajectorySimulationEngine().solveBlocking(sphere, input(range = 5000.0))
        assertEquals(EngineStatus.INCOMPLETE, result.engineStatus)
        assertTrue(result.trajectory.maxDistanceM < 5000.0)
    }

    @Test
    fun emptyProfileFields_fallBackToDefaultsInsteadOfFailing() {
        val bare = Profile(name = "BARE", projectile = ProjectileSpec())
        val result = TrajectorySimulationEngine().solveBlocking(bare, input(range = 300.0))
        assertTrue(result.trajectory.points.isNotEmpty())
        assertTrue(result.trajectory.points.last().velocityMs > 0.0)
    }

    @Test
    fun zeroRange_yieldsNoTrajectoryButStillAnAtmosphere() {
        val result = TrajectorySimulationEngine().solveBlocking(genericProfile(), input(range = 0.0))
        assertTrue(result.trajectory.isEmpty)
        assertEquals(EngineStatus.INCOMPLETE, result.engineStatus)
        assertTrue(result.atmosphere.densityKgM3 > 0.0)
    }

    @Test
    fun terminalPoint_isReportedAtTheRequestedRange() {
        val result = TrajectorySimulationEngine().solveBlocking(genericProfile(), input(range = 650.0))
        val terminal = result.terminal
        assertNotNull(terminal)
        assertEquals(650.0, terminal!!.distanceM, 1e-6)
    }

    @Test
    fun steepClimb_stillIntegratesWithoutRunningAway() {
        // sin/cos guard: a 60° launch is the validator's limit.
        val result = TrajectorySimulationEngine()
            .solveBlocking(genericProfile(), input(range = 500.0, incl = 60.0))
        assertTrue(result.trajectory.points.isNotEmpty())
        val expectedApexSlope = sin(60.0 * PI / 180.0)
        assertTrue("a 60 degree launch must climb steeply", result.trajectory.maxHeightM > 500.0 * expectedApexSlope * 0.5)
    }
}
