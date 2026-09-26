package com.example.smarttravelplanner.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.smarttravelplanner.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: Long): UserEntity?

    @Query("SELECT COUNT(*) FROM users WHERE LOWER(email) = LOWER(:email)")
    suspend fun emailExists(email: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET passwordHash = :passwordHash, salt = :salt WHERE LOWER(email) = LOWER(:email)")
    suspend fun updatePassword(email: String, passwordHash: String, salt: String)

    @Query("""
        UPDATE users SET 
            fullName = :fullName,
            phoneNumber = :phoneNumber,
            profilePictureUrl = COALESCE(:profilePictureUrl, profilePictureUrl),
            gender = :gender,
            homeLocation = :homeLocation,
            dateOfBirth = :dateOfBirth,
            maritalStatus = :maritalStatus
        WHERE LOWER(email) = LOWER(:email)
    """)
    suspend fun updateProfileDetails(
        email: String,
        fullName: String,
        phoneNumber: String,
        profilePictureUrl: String?,
        gender: String,
        homeLocation: String,
        dateOfBirth: String,
        maritalStatus: String
    )

    @Query("UPDATE users SET profilePictureUrl = :profilePictureUrl WHERE LOWER(email) = LOWER(:email)")
    suspend fun updateProfilePicture(email: String, profilePictureUrl: String?)

    /** Role of the user with [email]; emits again whenever it changes. Null if no such user. */
    @Query("SELECT role FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    fun observeRole(email: String): Flow<String?>

    @Query("UPDATE users SET role = :role WHERE LOWER(email) = LOWER(:email)")
    suspend fun updateRole(email: String, role: String)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUser(userId: Long)
}
