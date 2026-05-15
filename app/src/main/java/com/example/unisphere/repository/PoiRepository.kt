package com.example.unisphere.repository

import com.example.unisphere.db.local.dao.PoiDao
import com.example.unisphere.db.local.entity.PointOfInterestEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PoiRepository @Inject constructor(
    private val poiDao: PoiDao
) {
    fun getPois(userId: String): Flow<List<PointOfInterestEntity>> = poiDao.getPoisForUser(userId)

    suspend fun savePoi(poi: PointOfInterestEntity) = poiDao.savePoi(poi)

    suspend fun deletePoi(poi: PointOfInterestEntity) = poiDao.deletePoi(poi)
}