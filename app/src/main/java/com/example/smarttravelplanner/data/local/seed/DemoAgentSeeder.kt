package com.example.smarttravelplanner.data.local.seed

import com.example.smarttravelplanner.data.local.dao.AgentDao
import com.example.smarttravelplanner.data.local.dao.UserDao
import com.example.smarttravelplanner.data.local.entity.AgentProfileEntity
import com.example.smarttravelplanner.data.local.entity.UserEntity
import com.example.smarttravelplanner.domain.model.UserRole
import com.example.smarttravelplanner.util.SecurityUtils
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Creates a few ready-made travel-agent accounts so the agent feature can be
 * demoed without signing up agents by hand. Safe to call repeatedly: accounts
 * that already exist are left untouched.
 */
@Singleton
class DemoAgentSeeder @Inject constructor(
    private val userDao: UserDao,
    private val agentDao: AgentDao
) {

    data class DemoAgent(
        val email: String,
        val fullName: String,
        val agencyName: String,
        val city: String,
        val specialities: List<String>
    )

    companion object {
        /** Password for every demo agent account. */
        const val DEMO_PASSWORD = "agent123"

        val DEMO_AGENTS = listOf(
            DemoAgent(
                email = "priya@wanderlust.demo",
                fullName = "Priya Sharma",
                agencyName = "Wanderlust Journeys",
                city = "Jaipur",
                specialities = listOf("Rajasthan", "Heritage", "Luxury")
            ),
            DemoAgent(
                email = "arjun@keralatrails.demo",
                fullName = "Arjun Nair",
                agencyName = "Kerala Trails",
                city = "Kochi",
                specialities = listOf("Kerala", "Backwaters", "Nature")
            ),
            DemoAgent(
                email = "tenzin@himalayanhigh.demo",
                fullName = "Tenzin Dorje",
                agencyName = "Himalayan High",
                city = "Manali",
                specialities = listOf("Himachal", "Ladakh", "Adventure")
            )
        )
    }

    private val mutex = Mutex()

    suspend fun seedIfNeeded() = mutex.withLock {
        DEMO_AGENTS.forEach { agent ->
            if (userDao.emailExists(agent.email) == 0) {
                val salt = SecurityUtils.generateSalt()
                userDao.insertUser(
                    UserEntity(
                        email = agent.email,
                        fullName = agent.fullName,
                        passwordHash = SecurityUtils.hashPassword(DEMO_PASSWORD, salt),
                        salt = salt,
                        isEmailVerified = true,
                        homeLocation = agent.city,
                        role = UserRole.AGENT.name
                    )
                )
                agentDao.upsertAgentProfile(
                    AgentProfileEntity(
                        email = agent.email,
                        agencyName = agent.agencyName,
                        city = agent.city,
                        specialities = agent.specialities
                    )
                )
            }
        }
    }
}
