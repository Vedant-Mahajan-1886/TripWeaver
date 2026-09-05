package com.example.smarttravelplanner.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.smarttravelplanner.data.local.entity.UserEntity

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

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUser(userId: Long)
}
