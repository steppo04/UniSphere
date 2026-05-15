package com.example.unisphere.db.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.unisphere.db.local.dao.CalendarDao
import com.example.unisphere.db.local.dao.WalletDao
import com.example.unisphere.db.local.entity.TransactionCategoryEntity
import com.example.unisphere.db.local.entity.TransactionEntity
import com.example.unisphere.db.local.dao.UserDao
import com.example.unisphere.db.local.dao.EventDao
import com.example.unisphere.db.local.entity.UserEntity
import com.example.unisphere.db.local.entity.EventEntity
import com.example.unisphere.db.local.entity.CalendarTypeEntity // Assicurati che l'import sia corretto
import com.example.unisphere.ui.utils.CalendarConverters

@Database(
    entities = [
        UserEntity::class,
        EventEntity::class,
        CalendarTypeEntity::class,
        TransactionEntity::class,
        TransactionCategoryEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(CalendarConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun eventDao(): EventDao

    abstract fun walletDao(): WalletDao

    // 3. Corretto il nome della funzione in minuscolo (camelCase)
    abstract fun calendarDao(): CalendarDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "unisphere_database"
                )
                    // 4. FallbackDestructiveMigration è utile in sviluppo:
                    // se cambi versione, cancella e ricrea il DB invece di crashare
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}