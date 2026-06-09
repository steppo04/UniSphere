package com.example.unisphere.db.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.unisphere.db.local.dao.CalendarDao
import com.example.unisphere.db.local.dao.WalletDao
import com.example.unisphere.db.local.dao.UserDao
import com.example.unisphere.db.local.dao.EventDao
import com.example.unisphere.db.local.dao.PoiDao
import com.example.unisphere.db.local.dao.RecipeDao
import com.example.unisphere.db.local.dao.HouseDao // <--- NUOVO IMPORT
import com.example.unisphere.db.local.entity.TransactionCategoryEntity
import com.example.unisphere.db.local.entity.TransactionEntity
import com.example.unisphere.db.local.entity.FavoriteRecipeEntity
import com.example.unisphere.db.local.entity.PointOfInterestEntity
import com.example.unisphere.db.local.entity.UserEntity
import com.example.unisphere.db.local.entity.EventEntity
import com.example.unisphere.db.local.entity.CalendarTypeEntity
import com.example.unisphere.db.local.entity.* // <--- NUOVO IMPORT PER LE ENTITÀ COABITAZIONE
import com.example.unisphere.ui.utils.CalendarConverters

@Database(
    entities = [
        UserEntity::class,
        EventEntity::class,
        CalendarTypeEntity::class,
        TransactionEntity::class,
        TransactionCategoryEntity::class,
        FavoriteRecipeEntity::class,
        PointOfInterestEntity::class,
        HouseEntity::class,
        HouseMemberEntity::class,
        HouseInvitationEntity::class,
        CleaningServiceEntity::class,
        CleaningAssignmentEntity::class,
        GroupTransactionEntity::class,
        TransactionSplitEntity::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(CalendarConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun eventDao(): EventDao
    abstract fun walletDao(): WalletDao
    abstract fun calendarDao(): CalendarDao
    abstract fun recipeDao(): RecipeDao
    abstract fun poiDao(): PoiDao

    abstract fun houseDao(): HouseDao

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
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}