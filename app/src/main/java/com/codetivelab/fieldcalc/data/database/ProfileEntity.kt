package com.codetivelab.fieldcalc.data.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.codetivelab.fieldcalc.domain.models.EnvironmentDefaults
import com.codetivelab.fieldcalc.domain.models.ProjectileSpec
import com.codetivelab.fieldcalc.domain.models.ReferenceGeometry
import com.codetivelab.fieldcalc.domain.models.UnitSystem

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val isDemo: Boolean,
    val isLocked: Boolean,
    val unitSystem: UnitSystem,
    @Embedded(prefix = "proj_") val projectile: ProjectileSpec,
    @Embedded(prefix = "geo_") val geometry: ReferenceGeometry,
    @Embedded(prefix = "env_") val environmentDefaults: EnvironmentDefaults
)
