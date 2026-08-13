package com.codetivelab.fieldcalc.domain.engine

/**
 * Drag coefficient as a function of Mach number.
 *
 * These are generic, textbook aerodynamic curves for simple body shapes — the same data used in
 * any fluid-dynamics or physics-education context. They describe how much a shape resists the air;
 * they are not tied to any specific piece of equipment.
 */
interface DragModel {
    val id: String

    /** Drag coefficient (dimensionless) at the given Mach number. */
    fun dragCoefficient(mach: Double): Double
}

/**
 * Piecewise-linear Cd(M) curve driven by a lookup table, scaled by an optional form factor.
 * Values outside the table are clamped to the first/last entry.
 */
open class TabulatedDragModel(
    override val id: String,
    private val machPoints: DoubleArray,
    private val cdPoints: DoubleArray,
    private val formFactor: Double = 1.0
) : DragModel {

    init {
        require(machPoints.size == cdPoints.size && machPoints.size >= 2) {
            "Drag table needs matching mach/cd arrays of at least two entries"
        }
    }

    override fun dragCoefficient(mach: Double): Double {
        val m = if (mach.isNaN()) 0.0 else mach.coerceAtLeast(0.0)
        if (m <= machPoints.first()) return cdPoints.first() * formFactor
        if (m >= machPoints.last()) return cdPoints.last() * formFactor

        var i = 1
        while (i < machPoints.size && machPoints[i] < m) i++
        val m0 = machPoints[i - 1]
        val m1 = machPoints[i]
        val t = (m - m0) / (m1 - m0)
        return (cdPoints[i - 1] + t * (cdPoints[i] - cdPoints[i - 1])) * formFactor
    }
}

/** Constant Cd — the simplest teaching case, and the way to model a drag-free vacuum (cd = 0). */
class ConstantDragModel(private val cd: Double, override val id: String = ID) : DragModel {
    override fun dragCoefficient(mach: Double): Double = cd

    companion object { const val ID = "CONSTANT" }
}

/**
 * Factory turning a profile's `dragModelId` + aerodynamic parameter bag into a [DragModel].
 *
 * Recognised `aeroParams` keys:
 *  - "formFactor" — multiplies the tabulated Cd (default 1.0)
 *  - "cd"         — the constant Cd used by the CONSTANT model (default 0.30)
 */
object DragModels {

    const val GENERIC = "GENERIC"
    const val SPHERE = "SPHERE"
    const val CONSTANT = ConstantDragModel.ID

    /** All ids the admin profile editor may offer. */
    val available: List<String> = listOf(GENERIC, SPHERE, CONSTANT)

    /**
     * Streamlined body: low subsonic drag, a transonic rise peaking just above Mach 1, then a
     * slow supersonic decay. This is the generic shape of every pointed-body drag curve.
     */
    private val genericMach = doubleArrayOf(
        0.0, 0.50, 0.70, 0.85, 0.90, 0.95, 1.00, 1.05, 1.10,
        1.20, 1.35, 1.50, 1.75, 2.00, 2.50, 3.00, 4.00, 5.00
    )
    private val genericCd = doubleArrayOf(
        0.230, 0.228, 0.235, 0.290, 0.360, 0.450, 0.520, 0.545, 0.540,
        0.520, 0.490, 0.460, 0.425, 0.400, 0.365, 0.340, 0.310, 0.295
    )

    /** Smooth sphere: much higher drag everywhere, classic wind-tunnel curve. */
    private val sphereMach = doubleArrayOf(0.0, 0.60, 0.80, 0.90, 1.00, 1.20, 1.50, 2.00, 3.00, 5.00)
    private val sphereCd = doubleArrayOf(0.470, 0.480, 0.550, 0.700, 0.920, 1.020, 0.980, 0.920, 0.900, 0.880)

    fun forId(id: String, aeroParams: Map<String, Double> = emptyMap()): DragModel {
        val formFactor = aeroParams["formFactor"]?.takeIf { it > 0.0 } ?: 1.0
        return when (id.trim().uppercase()) {
            SPHERE -> TabulatedDragModel(SPHERE, sphereMach, sphereCd, formFactor)
            CONSTANT, "FLAT" -> ConstantDragModel((aeroParams["cd"] ?: 0.30) * formFactor)
            else -> TabulatedDragModel(GENERIC, genericMach, genericCd, formFactor)
        }
    }
}
