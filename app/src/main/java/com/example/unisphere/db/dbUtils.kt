package com.example.unisphere.db

import com.example.unisphere.db.local.AppDatabase
import com.example.unisphere.db.local.dao.UserDao
import android.content.Context
import androidx.room.Room
import com.example.unisphere.db.local.dao.CalendarDao
import com.example.unisphere.db.local.dao.WalletDao
import com.example.unisphere.repository.WalletRepository

import com.example.unisphere.db.local.dao.EventDao
import com.example.unisphere.db.local.dao.PoiDao
import com.example.unisphere.db.local.dao.RecipeDao
import com.example.unisphere.repository.UserRepository
import com.example.unisphere.repository.EventRepository
import com.example.unisphere.repository.PoiRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

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
    @Provides
    fun provideEventDao(database: AppDatabase): EventDao {
        return database.eventDao()
    }
    @Provides
    fun provideCalendarDao(database: AppDatabase): CalendarDao {
        return database.calendarDao()
    }


    // 3. Diciamo ad Hilt come costruire il Repository (gli passiamo il DAO in automatico!)
    @Provides
    @Singleton
    fun provideUserRepository(userDao: UserDao): UserRepository {
        return UserRepository(userDao)
    }
    @Provides
    @Singleton
    fun provideEventRepository(
        eventDao: EventDao,
        calendarDao: CalendarDao
    ): EventRepository {
        return EventRepository(
            eventDao = eventDao,
            calendarDao = calendarDao
        )
    }

    //WALLET
    @Provides
    fun provideWalletDao(database: AppDatabase): WalletDao {
        return database.walletDao()
    }

    @Provides
    @Singleton
    fun provideWalletRepository(walletDao: WalletDao): WalletRepository {
        return WalletRepository(walletDao)
    }

    //RECIPE
    @Module
    @InstallIn(SingletonComponent::class)
    object NetworkModule {

        @Provides
        @Singleton
        fun provideHttpClient(): HttpClient {
            return HttpClient(OkHttp) {
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        coerceInputValues = true
                    })
                }
            }
        }

        @Provides
        fun provideRecipeDao(database: AppDatabase): RecipeDao {
            return database.recipeDao()
        }
    }
    //Point Of Interest
    @Provides
    fun providePoiDao(database: AppDatabase): PoiDao {
        return database.poiDao()
    }

    @Provides
    @Singleton
    fun provideMapRepository(poiDao: PoiDao): PoiRepository {
        return PoiRepository(poiDao)
    }
}