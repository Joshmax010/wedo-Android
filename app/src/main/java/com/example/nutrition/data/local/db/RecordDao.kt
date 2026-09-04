package com.example.nutrition.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.nutrition.data.local.entity.DayRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * 每日饮食记录 DAO
 *
 * 以日期字符串为主键，提供完整的 CRUD 操作
 */
@Dao
interface RecordDao {

    @Query("SELECT * FROM day_records ORDER BY dateStr DESC")
    suspend fun getAll(): List<DayRecordEntity>

    @Query("SELECT * FROM day_records ORDER BY dateStr DESC")
    fun getAllFlow(): Flow<List<DayRecordEntity>>

    @Query("SELECT * FROM day_records WHERE dateStr = :dateStr")
    suspend fun getByDate(dateStr: String): DayRecordEntity?

    @Query("SELECT * FROM day_records WHERE dateStr = :dateStr")
    fun getByDateFlow(dateStr: String): Flow<DayRecordEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DayRecordEntity): Long

    @Update
    suspend fun update(entity: DayRecordEntity): Int

    @Query("DELETE FROM day_records WHERE dateStr = :dateStr")
    suspend fun delete(dateStr: String): Int

    @Query("DELETE FROM day_records")
    suspend fun deleteAll(): Int

    /** 查询日期范围内的记录 */
    @Query("SELECT * FROM day_records WHERE dateStr BETWEEN :start AND :end ORDER BY dateStr ASC")
    suspend fun getByDateRange(start: String, end: String): List<DayRecordEntity>

    /** 获取记录总天数 */
    @Query("SELECT COUNT(*) FROM day_records")
    suspend fun count(): Int
}
