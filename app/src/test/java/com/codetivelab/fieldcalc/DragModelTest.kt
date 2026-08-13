package com.codetivelab.fieldcalc

import com.codetivelab.fieldcalc.domain.engine.ConstantDragModel
import com.codetivelab.fieldcalc.domain.engine.DragModels
import com.codetivelab.fieldcalc.domain.engine.TrajectorySimulationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class DragModelTest {

    @Test fun genericModel_peaksInTheTransonicRegion() {
        val m = DragModels.forId(DragModels.GENERIC)
        val subsonic = m.dragCoefficient(0.5)
        val transonic = m.dragCoefficient(1.05)
        val supersonic = m.dragCoefficient(3.0)

        assertTrue("transonic drag rise", transonic > subsonic)
        assertTrue("supersonic decay", supersonic < transonic)
        assertTrue("supersonic still above low subsonic", supersonic > subsonic)
    }

    @Test fun genericModel_interpolatesLinearlyBetweenTableEntries() {
        val m = DragModels.forId(DragModels.GENERIC)
        val midpoint = m.dragCoefficient(2.25)               // halfway between the 2.0 and 2.5 rows
        val expected = (m.dragCoefficient(2.0) + m.dragCoefficient(2.5)) / 2.0
        assertEquals(expected, midpoint, 1e-9)
    }

    @Test fun tabulatedModel_clampsOutsideItsRange() {
        val m = DragModels.forId(DragModels.GENERIC)
        assertEquals(m.dragCoefficient(0.0), m.dragCoefficient(-5.0), 1e-12)
        assertEquals(m.dragCoefficient(5.0), m.dragCoefficient(50.0), 1e-12)
    }

    @Test fun sphere_hasMoreDragThanAStreamlinedBody() {
        val sphere = DragModels.forId(DragModels.SPHERE)
        val generic = DragModels.forId(DragModels.GENERIC)
        listOf(0.2, 0.8, 1.0, 2.0, 4.0).forEach { mach ->
            assertTrue("at Mach $mach", sphere.dragCoefficient(mach) > generic.dragCoefficient(mach))
        }
    }

    @Test fun constantModel_ignoresMach() {
        val m = ConstantDragModel(0.42)
        assertEquals(0.42, m.dragCoefficient(0.1), 1e-12)
        assertEquals(0.42, m.dragCoefficient(4.0), 1e-12)
    }

    @Test fun formFactor_scalesTheCurve() {
        val plain = DragModels.forId(DragModels.GENERIC)
        val doubled = DragModels.forId(DragModels.GENERIC, mapOf("formFactor" to 2.0))
        assertEquals(plain.dragCoefficient(1.5) * 2.0, doubled.dragCoefficient(1.5), 1e-12)
    }

    @Test fun unknownId_fallsBackToTheGenericCurve() {
        val unknown = DragModels.forId("NOT-A-MODEL")
        val generic = DragModels.forId(DragModels.GENERIC)
        assertEquals(generic.dragCoefficient(1.0), unknown.dragCoefficient(1.0), 1e-12)
    }

    @Test fun constantModel_readsCdFromAeroParams() {
        val m = DragModels.forId("CONSTANT", mapOf("cd" to 0.0))
        assertEquals(0.0, m.dragCoefficient(2.0), 1e-12)
    }

    @Test fun referenceArea_isTheCircleOfTheCalibre() {
        val area = TrajectorySimulationEngine.referenceArea(7.62)
        assertEquals(PI * 0.00381 * 0.00381, area, 1e-12)
    }

    @Test fun referenceArea_fallsBackWhenCalibreIsMissing() {
        assertEquals(
            TrajectorySimulationEngine.referenceArea(TrajectorySimulationEngine.DEFAULT_CALIBRE_MM),
            TrajectorySimulationEngine.referenceArea(0.0),
            1e-12
        )
    }
}
