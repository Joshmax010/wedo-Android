package com.example.nutrition.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.nutrition.data.local.entity.BodyRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * 身体记录 DAO
 */
@Dao
interface BodyRecordDao {

    @Query("SELECT * FROM body_records ORDER BY dateStr DESC")
    suspend fun getAll(): List<BodyRecordEntity>

    @Query("SELECT * FROM body_records ORDER BY dateStr DESC")
    fun getAllFlow(): Flow<List<BodyRecordEntity>>

    @Query("SELECT * FROM body_records WHERE dateStr = :dateStr LIMIT 1")
    suspend fun getByDate(dateStr: String): BodyRecordEntity?

    @Query("SELECT * FROM body_records WHERE dateStr = :dateStr LIMIT 1")
    fun getByDateFlow(dateStr: String): Flow<BodyRecordEntity?>

    @Query("SELECT * FROM body_records WHERE dateStr BETWEEN :start AND :end ORDER BY dateStr ASC")
    suspend fun getByDateRange(start: String, end: String): List<BodyRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BodyRecordEntity): Long

    @Query("DELETE FROM body_records WHERE dateStr = :dateStr")
    suspend fun delete(dateStr: String): Int

    @Query("DELETE FROM body_records")
    suspend fun deleteAll(): Int

    @Query("SELECT COUNT(*) FROM body_records")
    suspend fun count(): Int
}
