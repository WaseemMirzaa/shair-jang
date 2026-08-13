package com.codetivelab.fieldcalc.domain.engine

/** Instantaneous state of the simulated body. */
data class MotionState(val position: Vec3, val velocity: Vec3)

/** Selectable numerical integration scheme (§12 of the brief: "configurable integration method"). */
enum class IntegrationMethod { EULER, HEUN, RK4 }

/**
 * Fixed-step integrators for the second-order system
 *
 *     dp/dt = v
 *     dv/dt = a(p, v)
 *
 * Pure maths, no platform dependencies. RK4 is the default: it is fourth-order accurate, so a
 * 1 ms step reproduces the analytic drag-free parabola to well under a millimetre, while EULER is
 * kept for teaching the cost of a first-order scheme.
 */
object Integrator {

    /** Acceleration as a function of the current state. */
    fun interface Acceleration {
        fun at(state: MotionState): Vec3
    }

    fun step(state: MotionState, dt: Double, method: IntegrationMethod, accel: Acceleration): MotionState =
        when (method) {
            IntegrationMethod.EULER -> euler(state, dt, accel)
            IntegrationMethod.HEUN -> heun(state, dt, accel)
            IntegrationMethod.RK4 -> rk4(state, dt, accel)
        }

    private fun euler(s: MotionState, dt: Double, a: Acceleration): MotionState {
        val acc = a.at(s)
        return MotionState(s.position + s.velocity * dt, s.velocity + acc * dt)
    }

    /** Heun's method (explicit trapezoidal / improved Euler), second order. */
    private fun heun(s: MotionState, dt: Double, a: Acceleration): MotionState {
        val a0 = a.at(s)
        val predictor = MotionState(s.position + s.velocity * dt, s.velocity + a0 * dt)
        val a1 = a.at(predictor)
        return MotionState(
            s.position + (s.velocity + predictor.velocity) * (dt / 2.0),
            s.velocity + (a0 + a1) * (dt / 2.0)
        )
    }

    /** Classical fourth-order Runge–Kutta. */
    private fun rk4(s: MotionState, dt: Double, a: Acceleration): MotionState {
        val k1v = s.velocity
        val k1a = a.at(s)

        val s2 = MotionState(s.position + k1v * (dt / 2.0), s.velocity + k1a * (dt / 2.0))
        val k2v = s2.velocity
        val k2a = a.at(s2)

        val s3 = MotionState(s.position + k2v * (dt / 2.0), s.velocity + k2a * (dt / 2.0))
        val k3v = s3.velocity
        val k3a = a.at(s3)

        val s4 = MotionState(s.position + k3v * dt, s.velocity + k3a * dt)
        val k4v = s4.velocity
        val k4a = a.at(s4)

        val dp = (k1v + k2v * 2.0 + k3v * 2.0 + k4v) * (dt / 6.0)
        val dv = (k1a + k2a * 2.0 + k3a * 2.0 + k4a) * (dt / 6.0)
        return MotionState(s.position + dp, s.velocity + dv)
    }
}
