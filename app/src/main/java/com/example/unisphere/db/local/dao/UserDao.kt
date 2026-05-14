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

    // Recupera l'utente. Usiamo 'Flow' così se i dati cambiano,
    // l'interfaccia si aggiorna in tempo reale da sola!
    @Query("SELECT * FROM users WHERE uid = :uid")
    fun getUserById(uid: String): Flow<UserEntity?>

    // (Opzionale) Svuota la tabella quando l'utente fa il logout
    @Query("DELETE FROM users")
    suspend fun clearUsers()

    @Query("UPDATE users SET profilePictureUri = :uri WHERE uid = :uid")
    suspend fun updateProfileImage(uid: String, uri: String)

    @Query("UPDATE users SET currentTheme = :theme WHERE uid = :uid")
    suspend fun updateTheme(uid: String, theme: String)
}