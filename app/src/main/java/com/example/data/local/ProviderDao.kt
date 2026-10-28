package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ProviderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProviderDao {
    @Query("SELECT * FROM providers WHERE isActive = 1 ORDER BY isVerified DESC, name COLLATE NOCASE ASC")
    fun getAllActiveProviders(): Flow<List<ProviderEntity>>

    @Query("SELECT * FROM providers ORDER BY updatedAt DESC")
    fun getAllProvidersAdmin(): Flow<List<ProviderEntity>>

    @Query("SELECT * FROM providers WHERE id = :id LIMIT 1")
    fun getProviderById(id: String): Flow<ProviderEntity?>

    @Query("SELECT * FROM providers WHERE id = :id LIMIT 1")
    suspend fun getProviderByIdDirect(id: String): ProviderEntity?

    @Query("""
        SELECT p.* FROM providers p
        INNER JOIN favorites f ON p.id = f.providerId
        WHERE p.isActive = 1
        ORDER BY f.pinnedOrder ASC, f.pinnedAt DESC
    """)
    fun getPinnedProviders(): Flow<List<ProviderEntity>>

    @Query("""
        SELECT * FROM providers 
        WHERE isActive = 1 AND (
            name LIKE '%' || :query || '%' OR 
            banglaName LIKE '%' || :query || '%' OR 
            contactNumber LIKE '%' || :query || '%' OR 
            additionalContact LIKE '%' || :query || '%' OR 
            primaryCategory LIKE '%' || :query || '%' OR 
            additionalCategories LIKE '%' || :query || '%' OR 
            locationArea LIKE '%' || :query || '%' OR 
            description LIKE '%' || :query || '%' OR 
            specificAddress LIKE '%' || :query || '%'
        )
        ORDER BY isVerified DESC, name COLLATE NOCASE ASC
    """)
    fun searchProviders(query: String): Flow<List<ProviderEntity>>

    @Query("SELECT * FROM providers WHERE contactNumber = :phone OR additionalContact = :phone")
    suspend fun findByPhone(phone: String): List<ProviderEntity>

    @Query("""
        SELECT * FROM providers 
        WHERE (contactNumber = :phone OR additionalContact = :phone)
           OR (LOWER(name) = LOWER(:name))
    """)
    suspend fun findDuplicates(name: String, phone: String): List<ProviderEntity>

    @Query("SELECT COUNT(*) FROM providers WHERE isActive = 1")
    fun getActiveCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM providers")
    suspend fun getTotalCountDirect(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(provider: ProviderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(providers: List<ProviderEntity>): List<Long>

    @Update
    suspend fun update(provider: ProviderEntity)

    @Delete
    suspend fun delete(provider: ProviderEntity)

    @Query("DELETE FROM providers WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM providers WHERE isDemoData = 1")
    suspend fun deleteDemoProviders()
}
