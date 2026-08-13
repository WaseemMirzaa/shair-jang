package com.codetivelab.fieldcalc

import com.codetivelab.fieldcalc.domain.engine.IntegrationMethod
import com.codetivelab.fieldcalc.domain.engine.Integrator
import com.codetivelab.fieldcalc.domain.engine.MotionState
import com.codetivelab.fieldcalc.domain.engine.Vec3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class IntegratorTest {

    private val g = Vec3(0.0, -9.80665, 0.0)
    private val gravity = Integrator.Acceleration { g }

    private fun freeFall(method: IntegrationMethod, dt: Double, steps: Int): MotionState {
        var s = MotionState(Vec3.ZERO, Vec3(10.0, 0.0, 0.0))
        repeat(steps) { s = Integrator.step(s, dt, method, gravity) }
        return s
    }

    @Test fun rk4_isExactForConstantAcceleration() {
        val dt = 0.01
        val steps = 100
        val t = dt * steps
        val end = freeFall(IntegrationMethod.RK4, dt, steps)

        assertEquals(10.0 * t, end.position.x, 1e-9)
        assertEquals(0.5 * g.y * t * t, end.position.y, 1e-9)
        assertEquals(g.y * t, end.velocity.y, 1e-9)
    }

    @Test fun heun_isExactForConstantAcceleration() {
        val dt = 0.01
        val steps = 100
        val t = dt * steps
        val end = freeFall(IntegrationMethod.HEUN, dt, steps)
        assertEquals(0.5 * g.y * t * t, end.position.y, 1e-9)
    }

    @Test fun euler_accumulatesTheExpectedFirstOrderError() {
        val dt = 0.01
        val steps = 100
        val t = dt * steps
        val end = freeFall(IntegrationMethod.EULER, dt, steps)
        val exact = 0.5 * g.y * t * t

        // Explicit Euler lags by exactly ½·g·dt·t on a constant acceleration.
        assertEquals(exact - 0.5 * g.y * dt * t, end.position.y, 1e-9)
        assertTrue(abs(end.position.y - exact) > 1e-4)
    }

    @Test fun rk4_convergesOnAnExponentialDecay() {
        // dv/dt = -k·v has the closed form v(t) = v0·e^(-k·t); position is not used here.
        val k = 2.0
        val decay = Integrator.Acceleration { s -> s.velocity * -k }
        var s = MotionState(Vec3.ZERO, Vec3(100.0, 0.0, 0.0))
        val dt = 0.001
        repeat(1000) { s = Integrator.step(s, dt, IntegrationMethod.RK4, decay) }

        assertEquals(100.0 * Math.exp(-k * 1.0), s.velocity.x, 1e-6)
    }

    @Test fun vectorMath_behaves() {
        val a = Vec3(3.0, 4.0, 0.0)
        assertEquals(5.0, a.length, 1e-12)
        assertEquals(1.0, a.normalized().length, 1e-12)
        assertEquals(Vec3.ZERO, Vec3.ZERO.normalized())
        assertEquals(Vec3(4.0, 6.0, 0.0), Vec3(1.0, 2.0, 0.0) + Vec3(3.0, 4.0, 0.0))
        assertEquals(Vec3(2.0, 4.0, 6.0), Vec3(1.0, 2.0, 3.0) * 2.0)
    }
}
