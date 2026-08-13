package com.codetivelab.fieldcalc

import com.codetivelab.fieldcalc.domain.models.Trajectory
import com.codetivelab.fieldcalc.domain.models.TrajectoryPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrajectoryModelTest {

    /** A straight ramp: every quantity equals the distance, so interpolation is easy to check. */
    private val ramp = Trajectory(
        (0..10).map { i ->
            val d = i * 100.0
            TrajectoryPoint(
                distanceM = d, heightM = d, driftM = d, velocityMs = d,
                timeS = d, mach = d, energyJ = d
            )
        }
    )

    @Test fun atDistance_interpolatesBetweenSamples() {
        val p = ramp.atDistance(150.0)!!
        assertEquals(150.0, p.distanceM, 1e-9)
        assertEquals(150.0, p.heightM, 1e-9)
        assertEquals(150.0, p.velocityMs, 1e-9)
        assertEquals(150.0, p.timeS, 1e-9)
    }

    @Test fun atDistance_clampsOutsideTheSampledRange() {
        assertEquals(0.0, ramp.atDistance(-50.0)!!.distanceM, 1e-9)
        assertEquals(1000.0, ramp.atDistance(99999.0)!!.distanceM, 1e-9)
    }

    @Test fun atDistance_onAnEmptyTrajectory_isNull() {
        assertNull(Trajectory().atDistance(100.0))
    }

    @Test fun sampleEvery_generatesTheTableRows() {
        val rows = ramp.sampleEvery(100.0)
        // 100, 200 … 900 plus the final 1000 m point.
        assertEquals(10, rows.size)
        assertEquals(100.0, rows.first().distanceM, 1e-9)
        assertEquals(1000.0, rows.last().distanceM, 1e-9)
    }

    @Test fun sampleEvery_withAnIrregularStep_stillEndsAtTheLastPoint() {
        val rows = ramp.sampleEvery(300.0)
        assertEquals(listOf(300.0, 600.0, 900.0, 1000.0), rows.map { it.distanceM })
    }

    @Test fun sampleEvery_rejectsNonPositiveSteps() {
        assertTrue(ramp.sampleEvery(0.0).isEmpty())
        assertTrue(ramp.sampleEvery(-10.0).isEmpty())
    }

    @Test fun summaryProperties() {
        assertEquals(1000.0, ramp.maxHeightM, 1e-9)
        assertEquals(1000.0, ramp.flightTimeS, 1e-9)
        assertEquals(1000.0, ramp.maxDistanceM, 1e-9)
        assertTrue(Trajectory().isEmpty)
        assertEquals(0.0, Trajectory().maxHeightM, 1e-9)
    }
}
