package com.example.unisphere.repository


import com.example.unisphere.db.local.dao.UserDao
import com.example.unisphere.db.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val userDao: UserDao
){

    // La UI chiama questa funzione per salvare i dati dopo la registrazione
    suspend fun saveUserProfile(
        uid: String,
        email: String,
        name: String,
        surname: String,
        username: String,
        theme: String
    ) {
        val newUser = UserEntity(
            uid = uid,
            email = email,
            name = name,
            surname = surname,
            username = username,
            currentTheme = theme
        )
        userDao.insertUser(newUser)
    }

    // La UI chiama questa funzione per leggere i dati del profilo
    fun getCurrentUserProfile(uid: String): Flow<UserEntity?> {
        return userDao.getUserById(uid)
    }

    // Per il logout
    suspend fun clearLocalData() {
        userDao.clearUsers()
    }
    suspend fun updateProfileImage(uid: String, uri: String) {
        userDao.updateProfileImage(uid, uri)
    }
    suspend fun updateLocalTheme(uid: String, theme: String) {
        userDao.updateTheme(uid, theme)
    }
}