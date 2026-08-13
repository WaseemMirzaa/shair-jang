package com.codetivelab.fieldcalc.domain.engine

import kotlin.math.sqrt

/**
 * Minimal 3-component vector used by the trajectory integrator.
 *
 * Frame convention (right-handed, launch point at the origin):
 *   x = downrange (positive toward the target)
 *   y = vertical  (positive up)
 *   z = lateral   (positive to the right when looking downrange)
 *
 * Deliberately free of any Android/platform types so the engine can be ported to
 * ESP32 / Raspberry Pi / dedicated hardware unchanged.
 */
data class Vec3(val x: Double, val y: Double, val z: Double) {

    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Double) = Vec3(x * s, y * s, z * s)

    val length: Double get() = sqrt(x * x + y * y + z * z)

    /** Unit vector, or [ZERO] for a zero-length vector. */
    fun normalized(): Vec3 {
        val l = length
        return if (l <= 0.0) ZERO else Vec3(x / l, y / l, z / l)
    }

    companion object {
        val ZERO = Vec3(0.0, 0.0, 0.0)
    }
}
