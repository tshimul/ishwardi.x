package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.LocationAreaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationAreaDao {
    @Query("SELECT * FROM location_areas ORDER BY displayOrder ASC, name ASC")
    fun getAllAreas(): Flow<List<LocationAreaEntity>>

    @Query("SELECT * FROM location_areas WHERE isEnabled = 1 ORDER BY displayOrder ASC, name ASC")
    fun getEnabledAreas(): Flow<List<LocationAreaEntity>>

    @Query("SELECT COUNT(*) FROM location_areas")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(areas: List<LocationAreaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(area: LocationAreaEntity)

    @Update
    suspend fun update(area: LocationAreaEntity)

    @Delete
    suspend fun delete(area: LocationAreaEntity)
}
