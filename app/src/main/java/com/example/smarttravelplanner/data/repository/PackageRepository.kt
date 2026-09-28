package com.example.smarttravelplanner.data.repository

import com.example.smarttravelplanner.domain.model.agent.AgentPackage
import com.example.smarttravelplanner.domain.model.agent.PackageListing
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Agents' ready-made packages. Only the agent who owns a package can create,
 * edit, publish or delete it; travellers can only browse published ones and choose one.
 */
interface PackageRepository {

    // ── Agent ────────────────────────────────────────────────────────────────
    /** The logged-in agent's packages (published and drafts). */
    fun observeMyPackages(): Flow<List<AgentPackage>>

    /** Creates (id = 0) or updates a package owned by the logged-in agent. Returns its id. */
    suspend fun savePackage(pkg: AgentPackage): Result<Long>

    suspend fun setPublished(packageId: Long, published: Boolean): Result<Unit>

    suspend fun deletePackage(packageId: Long): Result<Unit>

    // ── Everyone ─────────────────────────────────────────────────────────────
    fun observePackage(packageId: Long): Flow<AgentPackage?>

    // ── Traveller ────────────────────────────────────────────────────────────
    /** Published packages with agent name + rating, optionally filtered by a search text. */
    fun observePublishedPackages(query: String = ""): Flow<List<PackageListing>>

    /**
     * Creates the traveller's own trip from a package (copy of its itinerary) and
     * links it to the package's agent at the "Chosen" stage, so the agent can send a quote.
     * Returns the new trip id.
     */
    suspend fun choosePackage(packageId: Long, startDate: LocalDate, adults: Int, kids: Int): Result<Long>
}
