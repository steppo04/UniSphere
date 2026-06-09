package com.example.unisphere.db.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.unisphere.db.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    // Inserisce o aggiorna un utente (se l'UID esiste già, lo sovrascrive)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE uid = :uid")
    fun getUserById(uid: String): Flow<UserEntity?>

    //  Svuota la tabella quando l'utente fa il logout
    @Query("DELETE FROM users")
    suspend fun clearUsers()

    @Query("UPDATE users SET profilePictureUri = :uri WHERE uid = :uid")
    suspend fun updateProfileImage(uid: String, uri: String)

    @Query("UPDATE users SET currentTheme = :theme WHERE uid = :uid")
    suspend fun updateTheme(uid: String, theme: String)

    // Conta quante volte è presente questa email (0 se libera, 1 se presa)
    @Query("SELECT COUNT(*) FROM users WHERE email = :email LIMIT 1")
    suspend fun countUsersByEmail(email: String): Int

    // Conta quantote volte è presente questo username (0 se libero, 1 se preso)
    @Query("SELECT COUNT(*) FROM users WHERE username = :username LIMIT 1")
    suspend fun countUsersByUsername(username: String): Int

    @Query("UPDATE users SET username = :newUsername WHERE uid = :uid")
    suspend fun updateUsername(uid: String, newUsername: String)

    @Query("UPDATE users SET email = :newEmail WHERE uid = :uid")
    suspend fun updateEmail(uid: String, newEmail: String)
}