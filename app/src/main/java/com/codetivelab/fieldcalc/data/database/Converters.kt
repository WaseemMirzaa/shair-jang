package com.codetivelab.fieldcalc.data.database

import androidx.room.TypeConverter
import com.codetivelab.fieldcalc.domain.models.UnitSystem

/** Room type converters for enum and the opaque aero-parameter map. */
class Converters {
    @TypeConverter fun unitToString(u: UnitSystem): String = u.name
    @TypeConverter fun stringToUnit(s: String): UnitSystem = UnitSystem.valueOf(s)

    @TypeConverter
    fun mapToString(m: Map<String, Double>): String =
        m.entries.joinToString(";") { "${it.key}=${it.value}" }

    @TypeConverter
    fun stringToMap(s: String): Map<String, Double> =
        if (s.isBlank()) emptyMap()
        else s.split(";").mapNotNull { pair ->
            val parts = pair.split("=")
            if (parts.size == 2) {
                val v = parts[1].toDoubleOrNull() ?: return@mapNotNull null
                parts[0] to v
            } else null
        }.toMap()
}
