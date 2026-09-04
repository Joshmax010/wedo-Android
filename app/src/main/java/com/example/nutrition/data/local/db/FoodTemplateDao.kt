package com.example.nutrition.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.nutrition.data.local.entity.FoodTemplateEntity
import kotlinx.coroutines.flow.Flow

/**
 * 食物模板 DAO
 */
@Dao
interface FoodTemplateDao {

    @Query("SELECT * FROM food_templates ORDER BY isPreset DESC, name ASC")
    suspend fun getAll(): List<FoodTemplateEntity>

    @Query("SELECT * FROM food_templates ORDER BY isPreset DESC, name ASC")
    fun getAllFlow(): Flow<List<FoodTemplateEntity>>

    @Query("SELECT * FROM food_templates WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): FoodTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FoodTemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(entity: FoodTemplateEntity): Long

    @Query("DELETE FROM food_templates WHERE id = :id")
    suspend fun delete(id: String): Int

    @Query("DELETE FROM food_templates WHERE isPreset = 0")
    suspend fun deleteAllCustom(): Int

    @Query("SELECT COUNT(*) FROM food_templates")
    suspend fun count(): Int
}
