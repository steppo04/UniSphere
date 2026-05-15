package com.example.unisphere.db.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.unisphere.db.local.entity.PointOfInterestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PoiDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePoi(poi: PointOfInterestEntity)

    @Delete
    suspend fun deletePoi(poi: PointOfInterestEntity)

    @Query("SELECT * FROM points_of_interest WHERE userUid = :userId")
    fun getPoisForUser(userId: String): Flow<List<PointOfInterestEntity>>
}