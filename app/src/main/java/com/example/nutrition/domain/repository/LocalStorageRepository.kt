package com.example.nutrition.domain.repository

import com.example.nutrition.domain.model.AppMeta
import com.example.nutrition.domain.model.BodyRecord
import com.example.nutrition.domain.model.CapacityStatus
import com.example.nutrition.domain.model.DayRecords
import com.example.nutrition.domain.model.FoodTemplate
import com.example.nutrition.domain.model.MealKey
import com.example.nutrition.domain.model.MealRecord
import com.example.nutrition.domain.model.NutritionTargets
import com.example.nutrition.domain.model.Resource
import com.example.nutrition.domain.model.StorageStatus
import kotlinx.coroutines.flow.Flow

/**
 * 本地存储仓库接口 —— 对应小程序 utils/storage.js 的 16 个导出函数
 *
 * 读操作返回 Flow；读取或本地 JSON 解码失败时抛出异常，由调用方反馈和重试。
 * 写操作返回 Resource<Unit>（成功 / 失败+错误信息），协程取消继续传播。
 */
interface LocalStorageRepository {

    // ==================== 营养目标 ====================

    /** 读取营养目标配置，不存在则返回 null */
    fun getTargets(): Flow<NutritionTargets?>

    /** 写入营养目标配置（含回读校验） */
    suspend fun setTargets(targets: NutritionTargets): Resource<Unit>

    // ==================== 每日记录 ====================

    /** 读取全部记录，返回日期 -> DayRecords 映射 */
    fun getAllRecords(): Flow<Map<String, DayRecords>>

    /** 读取某天记录，不存在则返回空 DayRecords */
    fun getDayRecords(dateStr: String): Flow<DayRecords>

    /** 写入某天记录（整体覆盖，含回读校验） */
    suspend fun setDayRecords(dateStr: String, dayData: DayRecords): Resource<Unit>

    /** 添加一条记录到指定日期的指定餐次（含回读校验） */
    suspend fun addRecord(dateStr: String, mealKey: MealKey, record: MealRecord): Resource<Unit>

    /** 更新一条记录 */
    suspend fun updateRecord(
        dateStr: String,
        mealKey: MealKey,
        recordId: String,
        newData: MealRecord
    ): Resource<Unit>

    /** 删除一条记录 */
    suspend fun deleteRecord(dateStr: String, mealKey: MealKey, recordId: String): Resource<Unit>

    /** 清空全部饮食记录（保留目标设置） */
    suspend fun clearRecords(): Resource<Unit>

    // ==================== 元信息 ====================

    /** 读取元信息，不存在则返回 null */
    fun getMeta(): Flow<AppMeta?>

    /** 写入元信息（含回读校验） */
    suspend fun setMeta(meta: AppMeta): Resource<Unit>

    // ==================== 批量操作 ====================

    /**
     * 在单个事务中批量写入数据（用于导入恢复，含回读校验）。
     * 写入或校验失败时回滚全部变更；null 字段保留现有数据。
     * 模板/身体记录传空列表时清空对应数据，其中预设模板保留。
     */
    suspend fun bulkSet(
        targets: NutritionTargets? = null,
        records: Map<String, DayRecords>? = null,
        meta: AppMeta? = null,
        foodTemplates: List<FoodTemplate>? = null,
        bodyRecords: List<BodyRecord>? = null
    ): Resource<Unit>

    // ==================== 存储状态 ====================

    /** 获取 Storage 使用情况 */
    suspend fun getStorageStatus(): StorageStatus

    /** 检查 Storage 容量是否充足（80% 阈值告警） */
    suspend fun checkStorageCapacity(): CapacityStatus

    // ==================== 食物模板 ====================

    /** 读取全部食物模板（含预设） */
    fun getAllFoodTemplates(): Flow<List<FoodTemplate>>

    /** 保存食物模板 */
    suspend fun saveFoodTemplate(template: FoodTemplate): Resource<Unit>

    /** 删除食物模板（预设不可删除） */
    suspend fun deleteFoodTemplate(id: String): Resource<Unit>

    // ==================== 身体记录 ====================

    /** 读取全部身体记录，按日期升序 */
    fun getAllBodyRecords(): Flow<List<BodyRecord>>

    /** 读取某日期身体记录 */
    fun getBodyRecord(dateStr: String): Flow<BodyRecord?>

    /** 保存身体记录 */
    suspend fun saveBodyRecord(record: BodyRecord): Resource<Unit>

    /** 删除某日期身体记录 */
    suspend fun deleteBodyRecord(dateStr: String): Resource<Unit>
}
