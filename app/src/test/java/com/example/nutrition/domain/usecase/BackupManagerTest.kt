package com.example.nutrition.domain.usecase

import com.example.nutrition.domain.constants.NutrientConstants
import com.example.nutrition.domain.model.AppMeta
import com.example.nutrition.domain.model.BodyRecord
import com.example.nutrition.domain.model.CapacityStatus
import com.example.nutrition.domain.model.DayRecords
import com.example.nutrition.domain.model.FoodTemplate
import com.example.nutrition.domain.model.MealKey
import com.example.nutrition.domain.model.MealMicro
import com.example.nutrition.domain.model.MealRecord
import com.example.nutrition.domain.model.NutritionTargets
import com.example.nutrition.domain.model.Resource
import com.example.nutrition.domain.model.StorageStatus
import com.example.nutrition.domain.repository.LocalStorageRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * BackupManager 单元测试
 *
 * 使用 FakeRepository 内存实现，覆盖导出/导入/校验/预览/容错
 */
class BackupManagerTest {

    private lateinit var fakeRepo: FakeRepository
    private lateinit var backupManager: BackupManager

    @Before
    fun setUp() {
        fakeRepo = FakeRepository()
        backupManager = BackupManager(fakeRepo)
    }

    // ==================== 辅助方法 ====================

    private fun sampleTargets() = NutritionTargets(
        calories = 2000.0,
        protein = 120.0,
        fat = 65.0,
        carbs = 250.0,
        micronutrients = listOf(
            com.example.nutrition.domain.model.MicronutrientTarget("vitC", "维生素C", "mg", 100.0),
            com.example.nutrition.domain.model.MicronutrientTarget("calcium", "钙", "mg", 800.0)
        )
    )

    private fun sampleRecord(name: String = "鸡蛋", calories: Double = 155.0) = MealRecord(
        name = name,
        calories = calories,
        protein = 13.0,
        fat = 11.0,
        carbs = 1.1,
        micronutrients = listOf(MealMicro("vitC", 5.0))
    )

    private fun sampleMeta() = AppMeta(
        version = "1.0.0",
        firstUseDate = "2026-06-01",
        schemaVersion = 1,
        hasSeenGuide = true
    )

    private fun buildValidBackupJson(
        targets: NutritionTargets? = sampleTargets(),
        records: Map<String, DayRecords> = mapOf(
            "2026-06-29" to DayRecords(
                dateStr = "2026-06-29",
                breakfast = listOf(sampleRecord())
            ),
            "2026-06-30" to DayRecords(
                dateStr = "2026-06-30",
                lunch = listOf(sampleRecord(name = "米饭", calories = 300.0))
            )
        ),
        meta: AppMeta? = sampleMeta()
    ): String {
        val exportObj = BackupManager.BackupExport(
            app = NutrientConstants.APP_NAME,
            schemaVersion = NutrientConstants.SCHEMA_VERSION,
            exportedAt = "2026-06-30T10:00:00Z",
            data = BackupManager.BackupData(
                targets = targets,
                records = records,
                meta = meta
            )
        )
        val json = kotlinx.serialization.json.Json {
            encodeDefaults = true
            prettyPrint = true
        }
        return json.encodeToString(
            BackupManager.BackupExport.serializer(),
            exportObj
        )
    }

    // ==================== verifyExportIntegrity ====================

    @Test
    fun `verifyExportIntegrity - 合法 JSON 通过`() {
        val jsonStr = buildValidBackupJson()
        val result = backupManager.verifyExportIntegrity(jsonStr)
        assertTrue(result.valid)
        assertEquals("", result.error)
    }

    @Test
    fun `verifyExportIntegrity - 非法 JSON`() {
        val result = backupManager.verifyExportIntegrity("not a json{{{")
        assertFalse(result.valid)
        assertTrue(result.error.contains("解析失败"))
    }

    @Test
    fun `verifyExportIntegrity - 缺少 app 字段`() {
        val result = backupManager.verifyExportIntegrity("""{"schemaVersion":1,"exportedAt":"x","data":{}}""")
        assertFalse(result.valid)
        assertTrue(result.error.contains("应用标识"))
    }

    @Test
    fun `verifyExportIntegrity - app 值不匹配`() {
        val result = backupManager.verifyExportIntegrity("""{"app":"wrong","schemaVersion":1,"exportedAt":"x","data":{}}""")
        assertFalse(result.valid)
        assertTrue(result.error.contains("应用标识"))
    }

    @Test
    fun `verifyExportIntegrity - 缺少 schemaVersion`() {
        val result = backupManager.verifyExportIntegrity("""{"app":"nutrition-tracker","exportedAt":"x","data":{}}""")
        assertFalse(result.valid)
        assertTrue(result.error.contains("版本号"))
    }

    @Test
    fun `verifyExportIntegrity - 缺少 exportedAt`() {
        val result = backupManager.verifyExportIntegrity("""{"app":"nutrition-tracker","schemaVersion":1,"data":{}}""")
        assertFalse(result.valid)
        assertTrue(result.error.contains("导出时间"))
    }

    @Test
    fun `verifyExportIntegrity - 缺少 data 字段`() {
        val result = backupManager.verifyExportIntegrity("""{"app":"nutrition-tracker","schemaVersion":1,"exportedAt":"x"}""")
        assertFalse(result.valid)
        assertTrue(result.error.contains("data"))
    }

    @Test
    fun `verifyExportIntegrity - 缺少 targets`() {
        val result = backupManager.verifyExportIntegrity("""{"app":"nutrition-tracker","schemaVersion":1,"exportedAt":"x","data":{"records":{}}}""")
        assertFalse(result.valid)
        assertTrue(result.error.contains("目标配置"))
    }

    // ==================== validateBackup ====================

    @Test
    fun `validateBackup - 合法数据`() = runTest {
        val jsonStr = buildValidBackupJson()
        val parsed = kotlinx.serialization.json.Json.parseToJsonElement(jsonStr).let {
            it as kotlinx.serialization.json.JsonObject
        }
        val result = backupManager.validateBackup(parsed)
        assertTrue(result.valid)
    }

    @Test
    fun `validateBackup - app 标识不匹配`() {
        val json = kotlinx.serialization.json.buildJsonObject {
            put("app", kotlinx.serialization.json.JsonPrimitive("wrong-app"))
            put("schemaVersion", kotlinx.serialization.json.JsonPrimitive(1))
            put("data", kotlinx.serialization.json.JsonObject(emptyMap()))
        }
        val result = backupManager.validateBackup(json)
        assertFalse(result.valid)
        assertTrue(result.error.contains("应用标识"))
    }

    @Test
    fun `validateBackup - 版本过高不兼容`() {
        val json = kotlinx.serialization.json.buildJsonObject {
            put("app", kotlinx.serialization.json.JsonPrimitive(NutrientConstants.APP_NAME))
            put("schemaVersion", kotlinx.serialization.json.JsonPrimitive(99))
            put("data", kotlinx.serialization.json.JsonObject(emptyMap()))
        }
        val result = backupManager.validateBackup(json)
        assertFalse(result.valid)
        assertTrue(result.error.contains("版本不兼容"))
    }

    @Test
    fun `validateBackup - records 中餐次不是数组`() {
        val json = kotlinx.serialization.json.buildJsonObject {
            put("app", kotlinx.serialization.json.JsonPrimitive(NutrientConstants.APP_NAME))
            put("schemaVersion", kotlinx.serialization.json.JsonPrimitive(1))
            put("data", kotlinx.serialization.json.buildJsonObject {
                put("records", kotlinx.serialization.json.buildJsonObject {
                    put("2026-06-30", kotlinx.serialization.json.buildJsonObject {
                        put("breakfast", kotlinx.serialization.json.JsonPrimitive("not-array"))
                    })
                })
            })
        }
        val result = backupManager.validateBackup(json)
        assertFalse(result.valid)
        assertTrue(result.error.contains("不是数组"))
    }

    // ==================== previewData ====================

    @Test
    fun `previewData - 正确生成预览`() {
        val jsonStr = buildValidBackupJson()
        val result = backupManager.previewData(jsonStr)
        assertTrue(result.valid)
        assertNotNull(result.preview)

        val preview = result.preview!!
        assertEquals(2, preview.dayCount)
        assertEquals(2, preview.recordCount)
        assertEquals("2026-06-29", preview.earliestDate)
        assertEquals("2026-06-30", preview.latestDate)
        assertTrue(preview.hasTargets)
        assertEquals(2000.0, preview.targetCalories!!, 0.001)
        assertEquals(120.0, preview.targetProtein!!, 0.001)
        assertEquals(NutrientConstants.SCHEMA_VERSION, preview.schemaVersion)
    }

    @Test
    fun `previewData - 空记录`() {
        val jsonStr = buildValidBackupJson(
            records = emptyMap(),
            targets = null
        )
        val result = backupManager.previewData(jsonStr)
        assertTrue(result.valid)
        assertEquals(0, result.preview!!.dayCount)
        assertEquals(0, result.preview!!.recordCount)
        assertFalse(result.preview!!.hasTargets)
    }

    @Test
    fun `previewData - 非法 JSON`() {
        val result = backupManager.previewData("invalid json")
        assertFalse(result.valid)
        assertTrue(result.error.contains("解析失败"))
    }

    // ==================== importData ====================

    @Test
    fun `importData - 正常导入`() = runTest {
        val jsonStr = buildValidBackupJson()
        val result = backupManager.importData(jsonStr)
        assertTrue(result.success)
        assertTrue(result.summary.contains("2 天"))
        assertTrue(result.summary.contains("2 条"))

        // 验证数据已写入
        val targets = fakeRepo.getTargets().first()
        assertNotNull(targets)
        assertEquals(2000.0, targets!!.calories, 0.001)

        val records = fakeRepo.getAllRecords().first()
        assertEquals(2, records.size)
    }

    @Test
    fun `importData - 非法 JSON`() = runTest {
        val result = backupManager.importData("{broken")
        assertFalse(result.success)
        assertTrue(result.error.contains("解析失败"))
    }

    @Test
    fun `importData - 校验不通过`() = runTest {
        val result = backupManager.importData("""{"app":"wrong","schemaVersion":1,"data":{}}""")
        assertFalse(result.success)
        assertTrue(result.error.contains("应用标识"))
    }

    // ==================== S4.7: 字段级 fallback ====================

    @Test
    fun `importData - targets 缺失字段补零`() = runTest {
        // targets 只有 calories，缺少 protein/fat/carbs/micronutrients
        val jsonStr = """
        {
            "app": "nutrition-tracker",
            "schemaVersion": 1,
            "exportedAt": "2026-06-30T10:00:00Z",
            "data": {
                "targets": {
                    "calories": 2500
                },
                "records": {},
                "meta": null
            }
        }
        """.trimIndent()

        val result = backupManager.importData(jsonStr)
        assertTrue(result.success)

        val targets = fakeRepo.getTargets().first()
        assertNotNull(targets)
        assertEquals(2500.0, targets!!.calories, 0.001)
        assertEquals(0.0, targets.protein, 0.001)
        assertEquals(0.0, targets.fat, 0.001)
        assertEquals(0.0, targets.carbs, 0.001)
        // micronutrients 缺失时补默认列表
        assertTrue(targets.micronutrients.isNotEmpty())
    }

    @Test
    fun `importData - records 中餐次数组缺失补空`() = runTest {
        val jsonStr = """
        {
            "app": "nutrition-tracker",
            "schemaVersion": 1,
            "exportedAt": "2026-06-30T10:00:00Z",
            "data": {
                "targets": { "calories": 2000, "protein": 120, "fat": 65, "carbs": 250, "micronutrients": [] },
                "records": {
                    "2026-06-30": {
                        "breakfast": [
                            { "id": "r1", "name": "面包", "calories": 300 }
                        ]
                    }
                },
                "meta": null
            }
        }
        """.trimIndent()

        val result = backupManager.importData(jsonStr)
        assertTrue(result.success)

        val records = fakeRepo.getAllRecords().first()
        assertEquals(1, records.size)
        val day = records["2026-06-30"]!!
        assertEquals(1, day.breakfast.size)
        assertEquals("面包", day.breakfast[0].name)
        assertEquals(300.0, day.breakfast[0].calories, 0.001)
        // 缺失字段补 0
        assertEquals(0.0, day.breakfast[0].protein, 0.001)
        assertEquals(0.0, day.breakfast[0].fat, 0.001)
        assertEquals(0.0, day.breakfast[0].carbs, 0.001)
        // 缺失的餐次补空
        assertEquals(0, day.lunch.size)
        assertEquals(0, day.dinner.size)
        assertEquals(0, day.snack.size)
    }

    @Test
    fun `importData - meta 缺失字段补默认值`() = runTest {
        val jsonStr = """
        {
            "app": "nutrition-tracker",
            "schemaVersion": 1,
            "exportedAt": "2026-06-30T10:00:00Z",
            "data": {
                "targets": { "calories": 2000, "protein": 120, "fat": 65, "carbs": 250, "micronutrients": [] },
                "records": {},
                "meta": { "version": "2.0.0" }
            }
        }
        """.trimIndent()

        val result = backupManager.importData(jsonStr)
        assertTrue(result.success)

        val meta = fakeRepo.getMeta().first()
        assertNotNull(meta)
        assertEquals("2.0.0", meta!!.version)
        assertEquals(NutrientConstants.SCHEMA_VERSION, meta.schemaVersion) // 缺失补默认（当前 schema 版本）
        assertFalse(meta.hasSeenGuide) // 缺失补默认
    }

    @Test
    fun `importData - records 完全缺失返回空`() = runTest {
        val jsonStr = """
        {
            "app": "nutrition-tracker",
            "schemaVersion": 1,
            "exportedAt": "2026-06-30T10:00:00Z",
            "data": {
                "targets": { "calories": 2000, "protein": 120, "fat": 65, "carbs": 250, "micronutrients": [] },
                "records": {}
            }
        }
        """.trimIndent()

        val result = backupManager.importData(jsonStr)
        assertTrue(result.success)

        val records = fakeRepo.getAllRecords().first()
        assertTrue(records.isEmpty())
    }

    @Test
    fun `importData - 微量营养素正确导入`() = runTest {
        val jsonStr = """
        {
            "app": "nutrition-tracker",
            "schemaVersion": 1,
            "exportedAt": "2026-06-30T10:00:00Z",
            "data": {
                "targets": { "calories": 2000, "protein": 120, "fat": 65, "carbs": 250, "micronutrients": [] },
                "records": {
                    "2026-06-30": {
                        "breakfast": [
                            {
                                "id": "r1",
                                "name": "综合维生素",
                                "calories": 10,
                                "protein": 0,
                                "fat": 0,
                                "carbs": 0,
                                "micronutrients": [
                                    { "key": "vitC", "value": 100 },
                                    { "key": "calcium", "value": 500 }
                                ]
                            }
                        ]
                    }
                },
                "meta": null
            }
        }
        """.trimIndent()

        val result = backupManager.importData(jsonStr)
        assertTrue(result.success)

        val day = fakeRepo.getDayRecords("2026-06-30").first()
        val micros = day.breakfast[0].micronutrients
        assertEquals(2, micros.size)
        assertEquals("vitC", micros[0].key)
        assertEquals(100.0, micros[0].value, 0.001)
        assertEquals("calcium", micros[1].key)
        assertEquals(500.0, micros[1].value, 0.001)
    }

    // ==================== exportData ====================

    @Test
    fun `exportData - 正常导出`() = runTest {
        fakeRepo.setTargets(sampleTargets())
        fakeRepo.addRecord("2026-06-30", MealKey.BREAKFAST, sampleRecord())
        fakeRepo.setMeta(sampleMeta())

        val result = backupManager.exportData()
        assertTrue(result.success)
        assertTrue(result.json.isNotEmpty())
        assertTrue(result.error.isEmpty())

        // 导出 JSON 应通过完整性校验
        val verification = backupManager.verifyExportIntegrity(result.json)
        assertTrue(verification.valid)
    }

    @Test
    fun `exportData - 空数据也能导出`() = runTest {
        val result = backupManager.exportData()
        // 空数据时 targets 为 null，导出校验会失败（缺少目标配置）
        // 这是预期行为：至少需要有 targets 才能通过完整性校验
        assertFalse(result.success)
        assertTrue(result.error.contains("目标配置"))
    }

    // ==================== 导入摘要 ====================

    @Test
    fun `importData - 摘要包含天数和条数`() = runTest {
        val jsonStr = buildValidBackupJson()
        val result = backupManager.importData(jsonStr)
        assertTrue(result.success)
        assertTrue(result.summary.contains("2 天"))
        assertTrue(result.summary.contains("2 条"))
        assertTrue(result.summary.contains("2000 kcal"))
    }

    // ==================== FakeRepository ====================

    /**
     * 内存实现的 FakeRepository，用于单元测试
     */
    class FakeRepository : LocalStorageRepository {
        private var targets: NutritionTargets? = null
        private val records = mutableMapOf<String, DayRecords>()
        private var meta: AppMeta? = null
        private val foodTemplates = mutableListOf<FoodTemplate>()
        private val bodyRecords = mutableMapOf<String, BodyRecord>()

        override fun getTargets(): Flow<NutritionTargets?> = flowOf(targets)

        override suspend fun setTargets(targets: NutritionTargets): Resource<Unit> {
            this.targets = targets
            return Resource.Success(Unit)
        }

        override fun getAllRecords(): Flow<Map<String, DayRecords>> = flowOf(records.toMap())

        override fun getDayRecords(dateStr: String): Flow<DayRecords> {
            return flowOf(records[dateStr] ?: DayRecords(dateStr = dateStr))
        }

        override suspend fun setDayRecords(dateStr: String, dayData: DayRecords): Resource<Unit> {
            records[dateStr] = dayData
            return Resource.Success(Unit)
        }

        override suspend fun addRecord(
            dateStr: String,
            mealKey: MealKey,
            record: MealRecord
        ): Resource<Unit> {
            val day = records[dateStr] ?: DayRecords(dateStr = dateStr)
            records[dateStr] = day.withMeal(mealKey, day.getMeal(mealKey) + record)
            return Resource.Success(Unit)
        }

        override suspend fun updateRecord(
            dateStr: String,
            mealKey: MealKey,
            recordId: String,
            newData: MealRecord
        ): Resource<Unit> {
            val day = records[dateStr] ?: DayRecords(dateStr = dateStr)
            val mealRecords = day.getMeal(mealKey).toMutableList()
            val idx = mealRecords.indexOfFirst { it.id == recordId }
            if (idx == -1) return Resource.Error("记录不存在或已被删除")
            mealRecords[idx] = newData.copy(id = recordId)
            records[dateStr] = day.withMeal(mealKey, mealRecords)
            return Resource.Success(Unit)
        }

        override suspend fun deleteRecord(
            dateStr: String,
            mealKey: MealKey,
            recordId: String
        ): Resource<Unit> {
            val day = records[dateStr] ?: DayRecords(dateStr = dateStr)
            val filtered = day.getMeal(mealKey).filter { it.id != recordId }
            records[dateStr] = day.withMeal(mealKey, filtered)
            return Resource.Success(Unit)
        }

        override suspend fun clearRecords(): Resource<Unit> {
            records.clear()
            return Resource.Success(Unit)
        }

        override fun getMeta(): Flow<AppMeta?> = flowOf(meta)

        override suspend fun setMeta(meta: AppMeta): Resource<Unit> {
            this.meta = meta
            return Resource.Success(Unit)
        }

        override suspend fun bulkSet(
            targets: NutritionTargets?,
            records: Map<String, DayRecords>?,
            meta: AppMeta?,
            foodTemplates: List<FoodTemplate>?,
            bodyRecords: List<BodyRecord>?
        ): Resource<Unit> {
            if (targets != null) this.targets = targets
            if (records != null) {
                this.records.clear()
                this.records.putAll(records)
            }
            if (meta != null) this.meta = meta
            if (foodTemplates != null) {
                this.foodTemplates.clear()
                this.foodTemplates.addAll(foodTemplates)
            }
            if (bodyRecords != null) {
                this.bodyRecords.clear()
                bodyRecords.forEach { this.bodyRecords[it.dateStr] = it }
            }
            return Resource.Success(Unit)
        }

        override suspend fun getStorageStatus(): StorageStatus {
            return StorageStatus(0, 100 * 1024 * 1024, records.size)
        }

        override suspend fun checkStorageCapacity(): CapacityStatus {
            return CapacityStatus(true, false, 0, 100 * 1024 * 1024, "")
        }

        override fun getAllFoodTemplates(): Flow<List<FoodTemplate>> = flowOf(foodTemplates.toList())

        override suspend fun saveFoodTemplate(template: FoodTemplate): Resource<Unit> {
            val index = foodTemplates.indexOfFirst { it.id == template.id }
            if (index != -1) {
                foodTemplates[index] = template
            } else {
                foodTemplates.add(template)
            }
            return Resource.Success(Unit)
        }

        override suspend fun deleteFoodTemplate(id: String): Resource<Unit> {
            return if (foodTemplates.removeIf { it.id == id }) Resource.Success(Unit)
            else Resource.Error("模板不存在或已被删除")
        }

        override fun getAllBodyRecords(): Flow<List<BodyRecord>> {
            return flowOf(bodyRecords.values.sortedBy { it.dateStr })
        }

        override fun getBodyRecord(dateStr: String): Flow<BodyRecord?> {
            return flowOf(bodyRecords[dateStr])
        }

        override suspend fun saveBodyRecord(record: BodyRecord): Resource<Unit> {
            bodyRecords[record.dateStr] = record
            return Resource.Success(Unit)
        }

        override suspend fun deleteBodyRecord(dateStr: String): Resource<Unit> {
            return if (bodyRecords.remove(dateStr) != null) Resource.Success(Unit)
            else Resource.Error("记录不存在或已被删除")
        }
    }
}
