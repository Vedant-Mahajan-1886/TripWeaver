package com.example.smarttravelplanner.data.remote.cloud

import android.util.Log
import androidx.room.withTransaction
import com.example.smarttravelplanner.data.local.dao.AgentDao
import com.example.smarttravelplanner.data.local.dao.GroupDao
import com.example.smarttravelplanner.data.local.dao.PackageDao
import com.example.smarttravelplanner.data.local.db.TravelDatabase
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Keeps agents' packages, group trips, group members and agency details in Firestore. */
interface CatalogSync {
    suspend fun pushPackage(packageId: Long)
    /** Call BEFORE deleting locally (needs the cloud id). */
    suspend fun removePackage(packageId: Long)
    suspend fun pushGroup(groupId: Long)
    /** Call BEFORE deleting locally (needs the cloud id). */
    suspend fun removeGroup(groupId: Long)
    suspend fun pushMember(groupId: Long, email: String, name: String, joinedAt: Long)
    suspend fun removeMember(groupId: Long, email: String)
    suspend fun pushAgentProfile(email: String)
    /** Live download of the catalogue while [email] is signed in; returns when cancelled. */
    suspend fun listen(email: String)
}

/** Cloud off: everything stays on this phone. Also used by unit tests. */
object NoCatalogSync : CatalogSync {
    override suspend fun pushPackage(packageId: Long) = Unit
    override suspend fun removePackage(packageId: Long) = Unit
    override suspend fun pushGroup(groupId: Long) = Unit
    override suspend fun removeGroup(groupId: Long) = Unit
    override suspend fun pushMember(groupId: Long, email: String, name: String, joinedAt: Long) = Unit
    override suspend fun removeMember(groupId: Long, email: String) = Unit
    override suspend fun pushAgentProfile(email: String) = Unit
    override suspend fun listen(email: String) = Unit
}

/**
 * Firestore implementation. Uploads are not awaited (Firestore's offline queue sends them
 * when online). Downloads use live listeners, so a package published on the agent's phone
 * appears on travellers' phones within seconds. Room stays the copy the screens read.
 */
@Singleton
class FirestoreCatalogSync @Inject constructor(
    private val cloudAuth: CloudAuth,
    private val firestore: dagger.Lazy<FirebaseFirestore>,
    private val database: TravelDatabase,
    private val packageDao: PackageDao,
    private val groupDao: GroupDao,
    private val agentDao: AgentDao
) : CatalogSync {

    private val db get() = firestore.get()
    private val packages get() = db.collection("packages")
    private val groups get() = db.collection("groups")
    private val agents get() = db.collection("agents")

    /** Firebase uid + email, only when the Firebase account matches [ownerEmail]. */
    private fun owner(ownerEmail: String): String? {
        val uid = cloudAuth.signedInUid ?: return null
        return if (cloudAuth.signedInEmail == ownerEmail.lowercase()) uid else null
    }

    // -- Uploads ---------------------------------------------------------------

    override suspend fun pushPackage(packageId: Long) {
        val withDays = packageDao.getPackage(packageId) ?: return
        val uid = owner(withDays.pkg.agentEmail) ?: return
        val cloudId = withDays.pkg.cloudId ?: newId().also { packageDao.setCloudId(packageId, it) }
        packages.document(cloudId)
            .set(CatalogCloudMapper.packageToMap(withDays.pkg, withDays.days, uid))
            .addOnFailureListener { Log.w(TAG, "Package upload failed: ${it.message}") }
        pushAgentProfile(withDays.pkg.agentEmail) // travellers need the agency name too
    }

    override suspend fun removePackage(packageId: Long) {
        val row = packageDao.getPackageRow(packageId) ?: return
        val uid = owner(row.agentEmail) ?: return
        val cloudId = row.cloudId ?: return
        packages.document(cloudId).set(tombstone(uid), SetOptions.merge())
            .addOnFailureListener { Log.w(TAG, "Package delete failed: ${it.message}") }
    }

    override suspend fun pushGroup(groupId: Long) {
        val row = groupDao.getGroup(groupId) ?: return
        val uid = owner(row.agentEmail) ?: return
        val cloudId = row.cloudId ?: newId().also { groupDao.setCloudId(groupId, it) }
        val packageCloudId = row.packageId?.let { packageDao.getPackageRow(it)?.cloudId }
        groups.document(cloudId)
            .set(CatalogCloudMapper.groupToMap(row, packageCloudId, uid))
            .addOnFailureListener { Log.w(TAG, "Group upload failed: ${it.message}") }
        pushAgentProfile(row.agentEmail)
    }

    override suspend fun removeGroup(groupId: Long) {
        val row = groupDao.getGroup(groupId) ?: return
        val uid = owner(row.agentEmail) ?: return
        val cloudId = row.cloudId ?: return
        groups.document(cloudId).set(tombstone(uid), SetOptions.merge())
            .addOnFailureListener { Log.w(TAG, "Group delete failed: ${it.message}") }
    }

    override suspend fun pushMember(groupId: Long, email: String, name: String, joinedAt: Long) {
        owner(email) ?: return
        val cloudId = groupDao.getGroup(groupId)?.cloudId ?: return
        val member = com.example.smarttravelplanner.data.local.entity.GroupMemberEntity(
            groupId = groupId, travellerEmail = email, travellerName = name, joinedAt = joinedAt
        )
        groups.document(cloudId).collection("members").document(email)
            .set(CatalogCloudMapper.memberToMap(member))
            .addOnFailureListener { Log.w(TAG, "Join upload failed: ${it.message}") }
    }

    override suspend fun removeMember(groupId: Long, email: String) {
        owner(email) ?: return
        val cloudId = groupDao.getGroup(groupId)?.cloudId ?: return
        groups.document(cloudId).collection("members").document(email).delete()
            .addOnFailureListener { Log.w(TAG, "Leave upload failed: ${it.message}") }
    }

    override suspend fun pushAgentProfile(email: String) {
        val uid = owner(email) ?: return
        val profile = agentDao.getAgentProfile(email.lowercase()) ?: return
        agents.document(profile.email).set(CatalogCloudMapper.agentToMap(profile, uid))
            .addOnFailureListener { Log.w(TAG, "Agency upload failed: ${it.message}") }
    }

    // -- Live downloads --------------------------------------------------------

    override suspend fun listen(email: String) {
        val uid = cloudAuth.signedInUid ?: return
        val me = email.lowercase()
        // An agent's own items that never reached the cloud (made offline or before sync)
        uploadMissing(me)
        coroutineScope {
            launch { snapshots(agents).collect { applyAgents(it) } }
            launch { snapshots(packages.whereEqualTo("published", true)).collect { applyPackages(it, me) } }
            launch { snapshots(packages.whereEqualTo("ownerUid", uid)).collect { applyPackages(it, me) } }
            launch { snapshots(groups.whereEqualTo("published", true)).collect { applyGroups(it, me) } }
            launch { snapshots(groups.whereEqualTo("ownerUid", uid)).collect { applyGroups(it, me) } }
            launch { snapshots(db.collectionGroup("members")).collect { applyMembers(it) } }
        }
    }

    private suspend fun uploadMissing(me: String) {
        packageDao.getPackagesForAgentOnce(me).filter { it.cloudId == null }.forEach { pushPackage(it.id) }
        groupDao.getGroupsForAgentOnce(me).filter { it.cloudId == null }.forEach { pushGroup(it.id) }
        if (agentDao.getAgentProfile(me) != null) pushAgentProfile(me)
    }

    private suspend fun applyAgents(snapshot: QuerySnapshot) {
        snapshot.documentChanges
            .filter { it.type != DocumentChange.Type.REMOVED }
            .mapNotNull { CatalogCloudMapper.agentFromMap(it.document.data) }
            .forEach { remote ->
                val local = agentDao.getAgentProfile(remote.email)
                if (local == null || remote.updatedAt > local.updatedAt) agentDao.upsertAgentProfile(remote)
            }
    }

    private suspend fun applyPackages(snapshot: QuerySnapshot, me: String) {
        for (change in snapshot.documentChanges) {
            val doc = change.document
            val local = packageDao.getPackageByCloudId(doc.id)
            if (change.type == DocumentChange.Type.REMOVED) {
                // Left the "published" list (unpublished or deleted): drop other agents' copies
                if (local != null && local.agentEmail != me) packageDao.deletePackage(local.id)
                continue
            }
            val remote = CatalogCloudMapper.packageFromMap(doc.id, doc.data) ?: continue
            when (TripCloudMapper.decide(local?.updatedAt, remote.pkg.updatedAt, remote.deleted)) {
                SyncAction.DOWNLOAD -> database.withTransaction {
                    val id = packageDao.upsertPackage(remote.pkg.copy(id = local?.id ?: 0))
                        .let { if (local != null) local.id else it }
                    packageDao.deleteDays(id)
                    packageDao.insertDays(remote.days.map { it.copy(id = 0, packageId = id) })
                }
                SyncAction.DELETE_LOCAL -> local?.let { packageDao.deletePackage(it.id) }
                SyncAction.UPLOAD -> local?.let { pushPackage(it.id) }
                SyncAction.NOTHING -> Unit
            }
        }
    }

    private suspend fun applyGroups(snapshot: QuerySnapshot, me: String) {
        for (change in snapshot.documentChanges) {
            val doc = change.document
            val local = groupDao.getGroupByCloudId(doc.id)
            if (change.type == DocumentChange.Type.REMOVED) {
                if (local != null && local.agentEmail != me) groupDao.deleteGroup(local.id)
                continue
            }
            val remote = CatalogCloudMapper.groupFromMap(doc.id, doc.data) ?: continue
            when (TripCloudMapper.decide(local?.updatedAt, remote.group.updatedAt, remote.deleted)) {
                SyncAction.DOWNLOAD -> {
                    val packageId = remote.packageCloudId?.let { packageDao.getPackageByCloudId(it)?.id }
                    val entity = remote.group.copy(id = local?.id ?: 0, packageId = packageId)
                    val id = if (local != null) {
                        groupDao.updateGroup(entity)
                        local.id
                    } else {
                        groupDao.insertGroup(entity)
                    }
                    refreshMembers(id, doc)
                }
                SyncAction.DELETE_LOCAL -> local?.let { groupDao.deleteGroup(it.id) }
                SyncAction.UPLOAD -> local?.let { pushGroup(it.id) }
                SyncAction.NOTHING -> Unit
            }
        }
    }

    /** One-time copy of a group's members (the live members listener keeps them current). */
    private suspend fun refreshMembers(localGroupId: Long, groupDoc: DocumentSnapshot) {
        val members = try {
            groupDoc.reference.collection("members").get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return
        }
        database.withTransaction {
            groupDao.deleteAllMembers(localGroupId)
            members.documents.mapNotNull { CatalogCloudMapper.memberFromMap(localGroupId, it.data ?: return@mapNotNull null) }
                .forEach { groupDao.upsertMember(it) }
        }
    }

    private suspend fun applyMembers(snapshot: QuerySnapshot) {
        for (change in snapshot.documentChanges) {
            val doc = change.document
            val groupCloudId = doc.reference.parent.parent?.id ?: continue
            val group = groupDao.getGroupByCloudId(groupCloudId) ?: continue // group not downloaded yet
            if (change.type == DocumentChange.Type.REMOVED) {
                groupDao.deleteMember(group.id, doc.id)
            } else {
                CatalogCloudMapper.memberFromMap(group.id, doc.data)?.let { groupDao.upsertMember(it) }
            }
        }
    }

    // -- Helpers ---------------------------------------------------------------

    private fun snapshots(query: Query): Flow<QuerySnapshot> = callbackFlow {
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Catalogue listener stopped: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot != null) trySend(snapshot)
        }
        awaitClose { registration.remove() }
    }

    private fun tombstone(uid: String) = mapOf(
        "ownerUid" to uid,
        "published" to false,
        "deleted" to true,
        "updatedAt" to System.currentTimeMillis()
    )

    private fun newId() = UUID.randomUUID().toString()

    private companion object {
        const val TAG = "Cloud"
    }
}
