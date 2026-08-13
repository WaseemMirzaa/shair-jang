package com.codetivelab.fieldcalc.data.repository

import com.codetivelab.fieldcalc.data.database.ProfileDao
import com.codetivelab.fieldcalc.data.database.ProfileEntity
import com.codetivelab.fieldcalc.domain.engine.DragModels
import com.codetivelab.fieldcalc.domain.models.EnvironmentDefaults
import com.codetivelab.fieldcalc.domain.models.Profile
import com.codetivelab.fieldcalc.domain.models.ProjectileSpec
import com.codetivelab.fieldcalc.domain.models.ReferenceGeometry
import com.codetivelab.fieldcalc.domain.models.UnitSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Single source of truth for profiles. Maps between Room entities and domain models. */
class ProfileRepository(private val dao: ProfileDao) {

    val profiles: Flow<List<Profile>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: Long): Profile? = dao.getById(id)?.toDomain()

    suspend fun save(profile: Profile): Long = dao.upsert(profile.toEntity())

    suspend fun delete(profile: Profile) = dao.deleteById(profile.id)

    /**
     * Seeds the built-in profiles the first time the app runs, so it is demoable offline with no
     * setup. Every value is generic placeholder data for a physics demonstration.
     */
    suspend fun seedIfEmpty() {
        if (dao.count() > 0) return
        seedProfiles().forEach { dao.upsert(it.toEntity()) }
    }

    /** The factory profile set. Exposed for tests and for a future "restore defaults" action. */
    fun seedProfiles(): List<Profile> = listOf(demoProfile(), trainingProfile(), sphereProfile())

    private fun demoProfile() = Profile(
        name = "DEMO PROFILE",
        isDemo = true,
        isLocked = true,
        projectile = ProjectileSpec(
            calibreMm = 7.62,
            massKg = 0.0100,
            lengthM = 0.032,
            nominalLaunchVelocityMs = 800.0,
            dragModelId = DragModels.GENERIC,
            aeroParams = mapOf("formFactor" to 1.0)
        ),
        geometry = ReferenceGeometry(referenceHeightM = 1.5, referenceDistanceM = 100.0),
        environmentDefaults = EnvironmentDefaults(temperatureC = 15.0, humidityPct = 50.0, altitudeM = 0.0),
        unitSystem = UnitSystem.METRIC
    )

    /** A slower, heavier body — useful for showing how much more the drag term matters. */
    private fun trainingProfile() = Profile(
        name = "TRAINING PROFILE",
        isDemo = false,
        isLocked = true,
        projectile = ProjectileSpec(
            calibreMm = 12.0,
            massKg = 0.045,
            lengthM = 0.050,
            nominalLaunchVelocityMs = 450.0,
            dragModelId = DragModels.GENERIC
        ),
        geometry = ReferenceGeometry(referenceHeightM = 1.5, referenceDistanceM = 100.0),
        environmentDefaults = EnvironmentDefaults(temperatureC = 15.0, humidityPct = 50.0, altitudeM = 0.0),
        unitSystem = UnitSystem.METRIC
    )

    /** The classroom case: a thrown sphere, where drag dominates almost immediately. */
    private fun sphereProfile() = Profile(
        name = "SPHERE DEMO",
        isDemo = true,
        isLocked = false,
        projectile = ProjectileSpec(
            calibreMm = 73.0,
            massKg = 0.145,
            lengthM = 0.073,
            nominalLaunchVelocityMs = 40.0,
            dragModelId = DragModels.SPHERE
        ),
        geometry = ReferenceGeometry(referenceHeightM = 1.8, referenceDistanceM = 20.0),
        environmentDefaults = EnvironmentDefaults(temperatureC = 20.0, humidityPct = 50.0, altitudeM = 0.0),
        unitSystem = UnitSystem.METRIC
    )

    private fun ProfileEntity.toDomain() = Profile(
        id = id, name = name, isDemo = isDemo, isLocked = isLocked,
        projectile = projectile, geometry = geometry,
        environmentDefaults = environmentDefaults, unitSystem = unitSystem
    )

    private fun Profile.toEntity() = ProfileEntity(
        id = id, name = name, isDemo = isDemo, isLocked = isLocked,
        unitSystem = unitSystem, projectile = projectile,
        geometry = geometry, environmentDefaults = environmentDefaults
    )
}
