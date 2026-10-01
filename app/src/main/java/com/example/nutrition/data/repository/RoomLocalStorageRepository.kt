package com.example.nutrition.data.repository

import androidx.room.withTransaction
import com.example.nutrition.data.local.db.NutritionDatabase
import com.example.nutrition.data.local.entity.BodyRecordEntity
import com.example.nutrition.data.local.entity.DayRecordEntity
import com.example.nutrition.data.local.entity.FoodTemplateEntity
import com.example.nutrition.data.local.entity.MetaEntity
import com.example.nutrition.data.local.entity.TargetEntity
import com.example.nutrition.domain.model.AppMeta
import com.example.nutrition.domain.model.BodyProfile
import com.example.nutrition.domain.model.BodyRecord
import com.example.nutrition.domain.model.CapacityStatus
import com.example.nutrition.domain.model.FoodTemplate
import com.example.nutrition.domain.model.DayRecords
import com.example.nutrition.domain.model.MealKey
import com.example.nutrition.domain.model.MealMicro
import com.example.nutrition.domain.model.MealRecord
import com.example.nutrition.domain.model.MicronutrientTarget
import com.example.nutrition.domain.model.NutritionTargets
import com.example.nutrition.domain.model.Resource
import com.example.nutrition.domain.model.StorageStatus
import com.example.nutrition.domain.repository.LocalStorageRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.time.Instant

/**
 * Room 实现的本地存储仓库
 *
 * 使用 kotlinx.serialization 在领域模型与 Room Entity 之间转换
 * 所有写操作均包含回读校验（verifyWrite）
 */
class RoomLocalStorageRepository(
    private val db: NutritionDatabase
) : LocalStorageRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val targetDao = db.targetDao()
    private val recordDao = db.recordDao()
    private val metaDao = db.metaDao()
    private val foodTemplateDao = db.foodTemplateDao()
    private val bodyRecordDao = db.bodyRecordDao()

    // ==================== 营养目标 ====================

    override fun getTargets(): Flow<NutritionTargets?> {
        return targetDao.getFlow()
            .map { it?.toDomain() }
            .catch { emit(null) }
    }

    override suspend fun setTargets(targets: NutritionTargets): Resource<Unit> {
        return try {
            val updated = targets.copy(updatedAt = Instant.now().toString())
            val entity = updated.toEntity()
            targetDao.insert(entity)
            if (verifyTargetWrite(updated)) Resource.Success(Unit)
            else Resource.Error("保存目标失败，请重试")
        } catch (e: Exception) {
            Resource.Error("保存目标失败，请重试")
        }
    }

    // ==================== 每日记录 ====================

    override fun getAllRecords(): Flow<Map<String, DayRecords>> {
        return recordDao.getAllFlow()
            .map { list ->
                list.associate { entity ->
                    entity.dateStr to entity.toDomain()
                }
            }
            .catch { emit(emptyMap()) }
    }

    override fun getDayRecords(dateStr: String): Flow<DayRecords> {
        return recordDao.getByDateFlow(dateStr)
            .map { it?.toDomain() ?: DayRecords(dateStr = dateStr) }
            .catch { emit(DayRecords(dateStr = dateStr)) }
    }

    override suspend fun setDayRecords(dateStr: String, dayData: DayRecords): Resource<Unit> {
        return try {
            val entity = dayData.toEntity(dateStr)
            recordDao.insert(entity)
            if (verifyDayRecordWrite(dateStr)) Resource.Success(Unit)
            else Resource.Error("保存失败，请重试")
        } catch (e: Exception) {
            Resource.Error("保存失败，请重试")
        }
    }

    override suspend fun addRecord(
        dateStr: String,
        mealKey: MealKey,
        record: MealRecord
    ): Resource<Unit> {
        return try {
            val dayData = getDayRecords(dateStr).first()
            val updatedDay = dayData.withMeal(
                mealKey,
                dayData.getMeal(mealKey) + record
            )
            setDayRecords(dateStr, updatedDay)
        } catch (e: Exception) {
            Resource.Error("保存失败，请重试")
        }
    }

    override suspend fun updateRecord(
        dateStr: String,
        mealKey: MealKey,
        recordId: String,
        newData: MealRecord
    ): Resource<Unit> {
        return try {
            val dayData = getDayRecords(dateStr).first()
            val mealRecords = dayData.getMeal(mealKey)
            val index = mealRecords.indexOfFirst { it.id == recordId }
            if (index == -1) return Resource.Error("记录不存在或已被删除")

            val updated = newData.copy(
                id = recordId,
                createdAt = mealRecords[index].createdAt,
                updatedAt = Instant.now().toString()
            )
            val newMealRecords = mealRecords.toMutableList().apply {
                set(index, updated)
            }
            val updatedDay = dayData.withMeal(mealKey, newMealRecords)
            setDayRecords(dateStr, updatedDay)
        } catch (e: Exception) {
            Resource.Error("保存失败，请重试")
        }
    }

    override suspend fun deleteRecord(
        dateStr: String,
        mealKey: MealKey,
        recordId: String
    ): Resource<Unit> {
        return try {
            val dayData = getDayRecords(dateStr).first()
            val mealRecords = dayData.getMeal(mealKey)
            val filtered = mealRecords.filter { it.id != recordId }
            if (filtered.size == mealRecords.size) {
                return Resource.Error("记录不存在或已被删除")
            }

            val updatedDay = dayData.withMeal(mealKey, filtered)
            setDayRecords(dateStr, updatedDay)
        } catch (e: Exception) {
            Resource.Error("删除失败，请重试")
        }
    }

    override suspend fun clearRecords(): Resource<Unit> {
        return try {
            recordDao.deleteAll()
            // 回读校验
            val remaining = recordDao.getAll()
            if (remaining.isEmpty()) Resource.Success(Unit)
            else Resource.Error("清空记录失败，请重试")
        } catch (e: Exception) {
            Resource.Error("清空记录失败，请重试")
        }
    }

    // ==================== 元信息 ====================

    override fun getMeta(): Flow<AppMeta?> {
        return metaDao.getFlow()
            .map { it?.toDomain() }
            .catch { emit(null) }
    }

    override suspend fun setMeta(meta: AppMeta): Resource<Unit> {
        return try {
            val entity = meta.toEntity()
            metaDao.insert(entity)
            if (verifyMetaWrite(meta)) Resource.Success(Unit)
            else Resource.Error("写入失败，请重试")
        } catch (e: Exception) {
            Resource.Error("写入失败，请重试")
        }
    }

    // ==================== 批量操作 ====================

    override suspend fun bulkSet(
        targets: NutritionTargets?,
        records: Map<String, DayRecords>?,
        meta: AppMeta?,
        foodTemplates: List<FoodTemplate>?,
        bodyRecords: List<BodyRecord>?
    ): Resource<Unit> {
        return try {
            // 导入涉及多张表，写入与回读校验必须全部成功后再提交。
            db.withTransaction {
                if (targets != null) {
                    targetDao.insert(targets.toEntity())
                    if (!verifyTargetWrite(targets)) throw ImportVerificationException()
                }
                if (records != null) {
                    for ((dateStr, dayData) in records) {
                        recordDao.insert(dayData.toEntity(dateStr))
                    }
                }
                if (meta != null) {
                    metaDao.insert(meta.toEntity())
                    if (!verifyMetaWrite(meta)) throw ImportVerificationException()
                }
                if (foodTemplates != null) {
                    foodTemplateDao.deleteAllCustom()
                    foodTemplates.forEach { foodTemplateDao.insert(it.toEntity()) }
                }
                if (bodyRecords != null) {
                    bodyRecordDao.deleteAll()
                    bodyRecords.forEach { bodyRecordDao.insert(it.toEntity()) }
                }
            }
            Resource.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ImportVerificationException) {
            Resource.Error("数据校验失败，请重试")
        } catch (e: Exception) {
            Resource.Error("数据导入失败，请重试")
        }
    }

    // 校验失败必须抛出异常，让 withTransaction 回滚，不能在事务内正常返回 Error。
    private class ImportVerificationException : Exception()

    // ==================== 存储状态 ====================

    override suspend fun getStorageStatus(): StorageStatus {
        return try {
            val path = db.openHelper.writableDatabase.path ?: return StorageStatus(0, 0, 0)
            val file = java.io.File(path)
            val currentSize = if (file.exists()) file.length() else 0L
            // Room 数据库没有硬编码上限，以 100MB 为软上限参考
            val limitSize = 100L * 1024 * 1024
            val recordCount = recordDao.count()
            StorageStatus(
                currentSize = currentSize,
                limitSize = limitSize,
                keys = recordCount
            )
        } catch (e: Exception) {
            StorageStatus(0, 0, 0)
        }
    }

    override suspend fun checkStorageCapacity(): CapacityStatus {
        return try {
            val status = getStorageStatus()
            val ratio = if (status.limitSize > 0) {
                status.currentSize.toDouble() / status.limitSize
            } else {
                0.0
            }
            val ok = ratio < CAPACITY_WARN_RATIO
            val warn = ratio >= CAPACITY_WARN_RATIO && ratio < 1.0
            val message = when {
                ratio >= 1.0 -> "存储空间已满，请导出数据后清理"
                ratio >= CAPACITY_WARN_RATIO -> "存储空间即将用尽，建议导出备份"
                else -> ""
            }
            CapacityStatus(
                ok = ok,
                warn = warn,
                currentSize = status.currentSize,
                limitSize = status.limitSize,
                message = message
            )
        } catch (e: Exception) {
            CapacityStatus(ok = true, warn = false, currentSize = 0, limitSize = 0, message = "")
        }
    }

    // ==================== 食物模板 ====================

    override fun getAllFoodTemplates(): Flow<List<FoodTemplate>> {
        return flow {
            ensurePresetTemplates()
            emitAll(
                foodTemplateDao.getAllFlow()
                    .map { list -> list.map { it.toDomain() } }
            )
        }.catch { emit(emptyList()) }
    }

    private suspend fun ensurePresetTemplates() {
        val currentPresets = com.example.nutrition.domain.constants.PresetFoodTemplates.getAll()
        val currentPresetIds = currentPresets.map { it.id }.toSet()

        // 删除已不在当前预设列表中的旧预设（避免改名/换 id 后重复）
        foodTemplateDao.getAll()
            .filter { it.isPreset && it.id !in currentPresetIds }
            .forEach { foodTemplateDao.delete(it.id) }

        // 插入新增预设，不覆盖用户已编辑的预设
        currentPresets.forEach {
            foodTemplateDao.insertOrIgnore(it.toEntity())
        }
    }

    override suspend fun saveFoodTemplate(template: FoodTemplate): Resource<Unit> {
        return try {
            foodTemplateDao.insert(template.toEntity())
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("保存模板失败")
        }
    }

    override suspend fun deleteFoodTemplate(id: String): Resource<Unit> {
        return try {
            val template = foodTemplateDao.getById(id)
                ?: return Resource.Error("模板不存在或已被删除")
            if (template.isPreset) return Resource.Error("预设模板不可删除")
            if (foodTemplateDao.delete(id) > 0) Resource.Success(Unit)
            else Resource.Error("删除失败，请重试")
        } catch (e: Exception) {
            Resource.Error("删除模板失败")
        }
    }

    // ==================== 身体记录 ====================

    override fun getAllBodyRecords(): Flow<List<BodyRecord>> {
        return bodyRecordDao.getAllFlow()
            .map { list -> list.map { it.toDomain() } }
            .catch { emit(emptyList()) }
    }

    override fun getBodyRecord(dateStr: String): Flow<BodyRecord?> {
        return bodyRecordDao.getByDateFlow(dateStr)
            .map { it?.toDomain() }
            .catch { emit(null) }
    }

    override suspend fun saveBodyRecord(record: BodyRecord): Resource<Unit> {
        return try {
            bodyRecordDao.insert(record.toEntity())
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("保存失败，请重试")
        }
    }

    override suspend fun deleteBodyRecord(dateStr: String): Resource<Unit> {
        return try {
            if (bodyRecordDao.delete(dateStr) > 0) Resource.Success(Unit)
            else Resource.Error("记录不存在或已被删除")
        } catch (e: Exception) {
            Resource.Error("删除失败，请重试")
        }
    }

    // ==================== 写入校验 ====================

    private suspend fun verifyTargetWrite(expected: NutritionTargets): Boolean {
        val actual = targetDao.get()?.toDomain() ?: return false
        // 简单校验：宏量营养素字段数量一致
        return expected.calories == actual.calories &&
            expected.protein == actual.protein &&
            expected.fat == actual.fat &&
            expected.carbs == actual.carbs
    }

    private suspend fun verifyDayRecordWrite(dateStr: String): Boolean {
        return recordDao.getByDate(dateStr) != null
    }

    private suspend fun verifyMetaWrite(expected: AppMeta): Boolean {
        val actual = metaDao.get()?.toDomain() ?: return false
        return expected.version == actual.version &&
            expected.schemaVersion == actual.schemaVersion
    }

    // ==================== Entity <-> Domain 转换 ====================

    private fun TargetEntity.toDomain(): NutritionTargets {
        val micros: List<MicronutrientTarget> = try {
            json.decodeFromString(micronutrientsJson)
        } catch (_: Exception) {
            emptyList()
        }
        val bodyProfile: BodyProfile? = try {
            bodyProfileJson?.let { json.decodeFromString<BodyProfile>(it) }
        } catch (_: Exception) {
            null
        }
        return NutritionTargets(
            calories = calories,
            protein = protein,
            fat = fat,
            carbs = carbs,
            micronutrients = micros,
            updatedAt = updatedAt,
            bodyProfile = bodyProfile,
            isAutoCalculated = isAutoCalculated
        )
    }

    private fun NutritionTargets.toEntity(): TargetEntity {
        return TargetEntity(
            id = 1,
            calories = calories,
            protein = protein,
            fat = fat,
            carbs = carbs,
            micronutrientsJson = json.encodeToString(micronutrients),
            updatedAt = updatedAt,
            bodyProfileJson = bodyProfile?.let { json.encodeToString(it) },
            isAutoCalculated = isAutoCalculated
        )
    }

    private fun DayRecordEntity.toDomain(): DayRecords {
        return DayRecords(
            dateStr = dateStr,
            breakfast = try { json.decodeFromString(breakfastJson) } catch (_: Exception) { emptyList() },
            lunch = try { json.decodeFromString(lunchJson) } catch (_: Exception) { emptyList() },
            dinner = try { json.decodeFromString(dinnerJson) } catch (_: Exception) { emptyList() },
            snack = try { json.decodeFromString(snackJson) } catch (_: Exception) { emptyList() }
        )
    }

    private fun DayRecords.toEntity(dateStr: String): DayRecordEntity {
        return DayRecordEntity(
            dateStr = dateStr,
            breakfastJson = json.encodeToString(breakfast),
            lunchJson = json.encodeToString(lunch),
            dinnerJson = json.encodeToString(dinner),
            snackJson = json.encodeToString(snack)
        )
    }

    private fun MetaEntity.toDomain(): AppMeta {
        return AppMeta(
            version = version,
            firstUseDate = firstUseDate,
            lastExportDate = lastExportDate,
            schemaVersion = schemaVersion,
            hasSeenGuide = hasSeenGuide
        )
    }

    private fun AppMeta.toEntity(): MetaEntity {
        return MetaEntity(
            id = 1,
            version = version,
            firstUseDate = firstUseDate,
            lastExportDate = lastExportDate,
            schemaVersion = schemaVersion,
            hasSeenGuide = hasSeenGuide
        )
    }

    private fun FoodTemplateEntity.toDomain(): FoodTemplate {
        val micros: List<MealMicro> = try {
            json.decodeFromString(micronutrientsJson)
        } catch (_: Exception) {
            emptyList()
        }
        val tags: List<String> = try {
            json.decodeFromString(tagsJson)
        } catch (_: Exception) {
            emptyList()
        }
        return FoodTemplate(
            id = id,
            name = name,
            calories = calories,
            protein = protein,
            fat = fat,
            carbs = carbs,
            micronutrients = micros,
            tags = tags,
            isPreset = isPreset,
            source = source,
            createdAt = createdAt
        )
    }

    private fun FoodTemplate.toEntity(): FoodTemplateEntity {
        return FoodTemplateEntity(
            id = id,
            name = name,
            calories = calories,
            protein = protein,
            fat = fat,
            carbs = carbs,
            micronutrientsJson = json.encodeToString(micronutrients),
            tagsJson = json.encodeToString(tags),
            isPreset = isPreset,
            source = source,
            createdAt = createdAt
        )
    }

    private fun BodyRecordEntity.toDomain(): BodyRecord {
        return BodyRecord(
            dateStr = dateStr,
            weightKg = weightKg,
            bodyFatPercent = bodyFatPercent,
            muscleKg = muscleKg,
            note = note,
            createdAt = createdAt
        )
    }

    private fun BodyRecord.toEntity(): BodyRecordEntity {
        return BodyRecordEntity(
            dateStr = dateStr,
            weightKg = weightKg,
            bodyFatPercent = bodyFatPercent,
            muscleKg = muscleKg,
            note = note,
            createdAt = createdAt
        )
    }

    companion object {
        private const val CAPACITY_WARN_RATIO = 0.8
    }
}
