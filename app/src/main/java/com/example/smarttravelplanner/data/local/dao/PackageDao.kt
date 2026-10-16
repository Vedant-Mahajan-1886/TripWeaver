package com.example.smarttravelplanner.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import com.example.smarttravelplanner.data.local.entity.AgentPackageEntity
import com.example.smarttravelplanner.data.local.entity.PackageDayEntity
import kotlinx.coroutines.flow.Flow

/** A package with its days. */
data class PackageWithDays(
    @Embedded val pkg: AgentPackageEntity,
    @Relation(parentColumn = "id", entityColumn = "packageId")
    val days: List<PackageDayEntity>
)

@Dao
interface PackageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPackage(pkg: AgentPackageEntity): Long

    @Insert
    suspend fun insertDays(days: List<PackageDayEntity>)

    @Query("DELETE FROM package_days WHERE packageId = :packageId")
    suspend fun deleteDays(packageId: Long)

    @Query("DELETE FROM agent_packages WHERE id = :packageId")
    suspend fun deletePackage(packageId: Long)

    @Query("SELECT * FROM agent_packages WHERE id = :packageId LIMIT 1")
    suspend fun getPackageRow(packageId: Long): AgentPackageEntity?

    @androidx.room.Transaction
    @Query("SELECT * FROM agent_packages WHERE id = :packageId LIMIT 1")
    suspend fun getPackage(packageId: Long): PackageWithDays?

    @androidx.room.Transaction
    @Query("SELECT * FROM agent_packages WHERE id = :packageId LIMIT 1")
    fun observePackage(packageId: Long): Flow<PackageWithDays?>

    /** One agent's packages (published or not), newest first. */
    @androidx.room.Transaction
    @Query("SELECT * FROM agent_packages WHERE agentEmail = :agentEmail ORDER BY updatedAt DESC")
    fun observePackagesForAgent(agentEmail: String): Flow<List<PackageWithDays>>

    /** Everything travellers can see. */
    @androidx.room.Transaction
    @Query("SELECT * FROM agent_packages WHERE isPublished = 1 ORDER BY updatedAt DESC")
    fun observePublishedPackages(): Flow<List<PackageWithDays>>

    @Query("UPDATE agent_packages SET isPublished = :published, updatedAt = :now WHERE id = :packageId")
    suspend fun setPublished(packageId: Long, published: Boolean, now: Long)

    // -- Cloud sync ------------------------------------------------------------

    @Query("SELECT * FROM agent_packages WHERE cloudId = :cloudId LIMIT 1")
    suspend fun getPackageByCloudId(cloudId: String): AgentPackageEntity?

    @Query("UPDATE agent_packages SET cloudId = :cloudId WHERE id = :packageId")
    suspend fun setCloudId(packageId: Long, cloudId: String)

    @Query("SELECT * FROM agent_packages WHERE agentEmail = :agentEmail")
    suspend fun getPackagesForAgentOnce(agentEmail: String): List<AgentPackageEntity>
}
