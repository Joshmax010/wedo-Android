package com.example.nutrition.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.nutrition.data.local.entity.MetaEntity
import kotlinx.coroutines.flow.Flow

/**
 * 应用元信息 DAO
 *
 * 全局只有一行（id = 1），提供 get / insert / update 操作
 */
@Dao
interface MetaDao {

    @Query("SELECT * FROM meta WHERE id = 1")
    suspend fun get(): MetaEntity?

    @Query("SELECT * FROM meta WHERE id = 1")
    fun getFlow(): Flow<MetaEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MetaEntity): Long

    @Update
    suspend fun update(entity: MetaEntity): Int
}
