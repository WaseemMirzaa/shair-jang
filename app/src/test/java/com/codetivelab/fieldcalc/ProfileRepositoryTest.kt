package com.codetivelab.fieldcalc

import com.codetivelab.fieldcalc.data.database.ProfileDao
import com.codetivelab.fieldcalc.data.database.ProfileEntity
import com.codetivelab.fieldcalc.data.repository.ProfileRepository
import com.codetivelab.fieldcalc.domain.models.EnvironmentDefaults
import com.codetivelab.fieldcalc.domain.models.Profile
import com.codetivelab.fieldcalc.domain.models.ProjectileSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Repository behaviour against an in-memory fake of the DAO — no Android or Room runtime needed,
 * so this runs as a plain JVM unit test.
 */
class ProfileRepositoryTest {

    private class FakeDao : ProfileDao {
        private val rows = MutableStateFlow<List<ProfileEntity>>(emptyList())
        private var nextId = 1L

        override fun observeAll(): Flow<List<ProfileEntity>> = rows.map { it.sortedBy(ProfileEntity::name) }
        override suspend fun getById(id: Long): ProfileEntity? = rows.value.firstOrNull { it.id == id }
        override suspend fun count(): Int = rows.value.size

        override suspend fun upsert(profile: ProfileEntity): Long {
            val id = if (profile.id == 0L) nextId++ else profile.id
            val stored = profile.copy(id = id)
            rows.value = rows.value.filterNot { it.id == id } + stored
            return id
        }

        override suspend fun delete(profile: ProfileEntity) = deleteById(profile.id)

        override suspend fun deleteById(id: Long) {
            rows.value = rows.value.filterNot { it.id == id }
        }
    }

    private fun repo() = ProfileRepository(FakeDao())

    @Test fun seeding_addsTheBuiltInProfilesExactlyOnce() = runTest {
        val repo = repo()
        repo.seedIfEmpty()
        val afterFirst = repo.profiles.first()
        repo.seedIfEmpty()
        val afterSecond = repo.profiles.first()

        assertEquals(repo.seedProfiles().size, afterFirst.size)
        assertEquals(afterFirst.size, afterSecond.size)
    }

    @Test fun seededSet_containsALockedDemoProfile() = runTest {
        val repo = repo()
        repo.seedIfEmpty()
        val demo = repo.profiles.first().firstOrNull { it.name == "DEMO PROFILE" }

        assertNotNull(demo)
        assertTrue("the demo profile must ship locked", demo!!.isLocked)
        assertTrue(demo.isDemo)
        assertTrue("the demo profile needs a launch velocity to be demoable", demo.projectile.nominalLaunchVelocityMs > 0.0)
    }

    @Test fun save_thenLoad_roundTripsEveryField() = runTest {
        val repo = repo()
        val id = repo.save(
            Profile(
                name = "UNIT TEST",
                isDemo = false,
                isLocked = false,
                projectile = ProjectileSpec(
                    calibreMm = 9.0, massKg = 0.008, lengthM = 0.015,
                    nominalLaunchVelocityMs = 360.0, dragModelId = "SPHERE"
                ),
                environmentDefaults = EnvironmentDefaults(temperatureC = 21.0, humidityPct = 40.0, altitudeM = 250.0)
            )
        )

        val loaded = repo.getById(id)
        assertNotNull(loaded)
        assertEquals("UNIT TEST", loaded!!.name)
        assertEquals(9.0, loaded.projectile.calibreMm, 1e-9)
        assertEquals(360.0, loaded.projectile.nominalLaunchVelocityMs, 1e-9)
        assertEquals("SPHERE", loaded.projectile.dragModelId)
        assertEquals(250.0, loaded.environmentDefaults.altitudeM, 1e-9)
        assertTrue(!loaded.isLocked)
    }

    @Test fun save_withAnExistingId_updatesInsteadOfDuplicating() = runTest {
        val repo = repo()
        val id = repo.save(Profile(name = "FIRST"))
        repo.save(Profile(id = id, name = "RENAMED"))

        val all = repo.profiles.first()
        assertEquals(1, all.size)
        assertEquals("RENAMED", all.single().name)
    }

    @Test fun delete_removesTheProfile() = runTest {
        val repo = repo()
        val id = repo.save(Profile(name = "TEMPORARY"))
        repo.delete(Profile(id = id, name = "TEMPORARY"))

        assertNull(repo.getById(id))
        assertTrue(repo.profiles.first().isEmpty())
    }

    @Test fun missingProfile_returnsNullRatherThanThrowing() = runTest {
        assertNull(repo().getById(4242L))
    }
}
