package com.example.nutrition.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.nutrition.data.local.entity.TargetEntity
import kotlinx.coroutines.flow.Flow

/**
 * 营养目标 DAO
 *
 * 全局只有一行（id = 1），提供 get / insert / update 操作
 */
@Dao
interface TargetDao {

    @Query("SELECT * FROM targets WHERE id = 1")
    suspend fun get(): TargetEntity?

    @Query("SELECT * FROM targets WHERE id = 1")
    fun getFlow(): Flow<TargetEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TargetEntity): Long

    @Update
    suspend fun update(entity: TargetEntity): Int
}
