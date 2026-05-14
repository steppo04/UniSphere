package com.example.unisphere.db

import com.example.unisphere.db.local.AppDatabase
import com.example.unisphere.db.local.dao.UserDao
import android.content.Context
import androidx.room.Room
import com.example.unisphere.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // Significa: "Questi oggetti vivono per tutta la durata dell'app"
object DatabaseModule {

    // 1. Diciamo ad Hilt come creare il Database Room
    @Provides
    @Singleton // Ne vogliamo solo uno in tutta l'app!
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "unisphere_database"
        ).build()
    }

    // 2. Diciamo ad Hilt come estrarre il DAO dal database
    @Provides
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }

    // 3. Diciamo ad Hilt come costruire il Repository (gli passiamo il DAO in automatico!)
    @Provides
    @Singleton
    fun provideUserRepository(userDao: UserDao): UserRepository {
        return UserRepository(userDao)
    }
}