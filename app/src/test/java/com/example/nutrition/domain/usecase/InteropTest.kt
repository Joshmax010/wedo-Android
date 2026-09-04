package com.example.nutrition.domain.usecase

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
import com.example.nutrition.domain.repository.LocalStorageRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * 数据互通测试 —— 验证小程序 ↔ Android JSON 格式兼容性
 *
 * 测试场景：
 * 1. 小程序导出的 JSON → Android 导入 → 数据一致
 * 2. Android 导出的 JSON → 重新导入 → 数据一致（roundtrip）
 * 3. Calculator 数值一致性（同一份数据三端计算结果一致）
 */
class InteropTest {

    private lateinit var repo: TestRepo
    private lateinit var backupManager: BackupManager

    @Before
    fun setUp() {
        repo = TestRepo()
        backupManager = BackupManager(repo)
    }

    // ==================== 小程序→Android 导入测试 ====================

    /**
     * 测试小程序格式的 JSON 能被 Android 正确导入
     * JSON 结构与小程序 backup.js exportData() 输出完全一致
     */
    @Test
    fun `小程序格式 JSON 导入后数据一致`() = runTest {
        val wechatJson = """
        {
            "app": "nutrition-tracker",
            "schemaVersion": 1,
            "exportedAt": "2026-06-15T10:30:00.000Z",
            "data": {
                "targets": {
                    "calories": 2000,
                    "protein": 120,
                    "fat": 65,
                    "carbs": 250,
                    "micronutrients": [
                        {"key": "vitC", "name": "维生素C", "unit": "mg", "target": 100},
                        {"key": "calcium", "name": "钙", "unit": "mg", "target": 800}
                    ],
                    "updatedAt": "2026-06-01T08:00:00.000Z"
                },
                "records": {
                    "2026-06-14": {
                        "breakfast": [
                            {
                                "id": "r_1718345600000",
                                "name": "燕麦牛奶",
                                "calories": 350,
                                "protein": 15.5,
                                "fat": 8.2,
                                "carbs": 52.0,
                                "micronutrients": [
                                    {"key": "calcium", "value": 300}
                                ],
                                "createdAt": "2026-06-14T07:30:00.000Z"
                            }
                        ],
                        "lunch": [
                            {
                                "id": "r_1718366400000",
                                "name": "鸡胸肉沙拉",
                                "calories": 420,
                                "protein": 35.0,
                                "fat": 12.0,
                                "carbs": 25.0,
                                "micronutrients": [
                                    {"key": "vitC", "value": 45}
                                ],
                                "createdAt": "2026-06-14T12:00:00.000Z"
                            }
                        ],
                        "dinner": [],
                        "snack": []
                    },
                    "2026-06-15": {
                        "breakfast": [
                            {
                                "id": "r_1718432000000",
                                "name": "全麦面包",
                                "calories": 280,
                                "protein": 10.0,
                                "fat": 5.0,
                                "carbs": 45.0,
                                "micronutrients": [],
                                "createdAt": "2026-06-15T08:00:00.000Z"
                            }
                        ],
                        "lunch": [],
                        "dinner": [],
                        "snack": []
                    }
                },
                "meta": {
                    "version": "1.0.0",
                    "firstUseDate": "2026-06-01",
                    "schemaVersion": 1,
                    "hasSeenGuide": true,
                    "lastExportDate": "2026-06-15T10:30:00.000Z"
                }
            }
        }
        """.trimIndent()

        // 1. 预览校验
        val preview = backupManager.previewData(wechatJson)
        assertTrue("预览应通过", preview.valid)
        assertNotNull("预览数据不应为空", preview.preview)
        assertEquals(2, preview.preview!!.dayCount)
        assertEquals(3, preview.preview!!.recordCount)
        assertEquals("2026-06-14", preview.preview!!.earliestDate)
        assertEquals("2026-06-15", preview.preview!!.latestDate)
        assertTrue(preview.preview!!.hasTargets)
        assertEquals(2000.0, preview.preview!!.targetCalories!!, 0.01)
        assertEquals(120.0, preview.preview!!.targetProtein!!, 0.01)

        // 2. 导入
        val importResult = backupManager.importData(wechatJson)
        assertTrue("导入应成功: ${importResult.error}", importResult.success)

        // 3. 验证目标配置
        val targets = repo.getTargets().first()
        assertNotNull("目标配置应存在", targets)
        assertEquals(2000.0, targets!!.calories, 0.01)
        assertEquals(120.0, targets.protein, 0.01)
        assertEquals(65.0, targets.fat, 0.01)
        assertEquals(250.0, targets.carbs, 0.01)
        assertEquals(2, targets.micronutrients.size)
        assertEquals("vitC", targets.micronutrients[0].key)
        assertEquals(100.0, targets.micronutrients[0].target, 0.01)

        // 4. 验证记录数据
        val allRecords = repo.getAllRecords().first()
        assertEquals(2, allRecords.size)

        val day1 = allRecords["2026-06-14"]
        assertNotNull("2026-06-14 记录应存在", day1)
        assertEquals(1, day1!!.breakfast.size)
        assertEquals("燕麦牛奶", day1.breakfast[0].name)
        assertEquals(350.0, day1.breakfast[0].calories, 0.01)
        assertEquals(15.5, day1.breakfast[0].protein, 0.01)
        assertEquals(1, day1.breakfast[0].micronutrients.size)
        assertEquals("calcium", day1.breakfast[0].micronutrients[0].key)
        assertEquals(300.0, day1.breakfast[0].micronutrients[0].value, 0.01)

        assertEquals(1, day1.lunch.size)
        assertEquals("鸡胸肉沙拉", day1.lunch[0].name)

        val day2 = allRecords["2026-06-15"]
        assertNotNull("2026-06-15 记录应存在", day2)
        assertEquals(1, day2!!.breakfast.size)
        assertEquals("全麦面包", day2.breakfast[0].name)

        // 5. 验证元信息
        val meta = repo.getMeta().first()
        assertNotNull("元信息应存在", meta)
        assertEquals("2026-06-01", meta!!.firstUseDate)
        assertTrue(meta.hasSeenGuide)
    }

    // ==================== Android roundtrip 测试 ====================

    /**
     * Android 导出数据再导入，数据应完全一致
     */
    @Test
    fun `Android 导出再导入 roundtrip 数据一致`() = runTest {
        // 准备数据
        val targets = NutritionTargets(
            calories = 1800.0, protein = 100.0, fat = 55.0, carbs = 220.0,
            micronutrients = listOf(
                com.example.nutrition.domain.model.MicronutrientTarget("iron", "铁", "mg", 12.0)
            )
        )
        repo.setTargets(targets)

        val record = MealRecord(
            id = "test_001",
            name = "测试记录",
            calories = 500.0,
            protein = 30.0,
            fat = 20.0,
            carbs = 45.0,
            micronutrients = listOf(
                com.example.nutrition.domain.model.MealMicro("iron", 6.0)
            )
        )
        repo.addRecord("2026-06-20", MealKey.DINNER, record)

        // 导出
        val exportResult = backupManager.exportData()
        assertTrue("导出应成功", exportResult.success)
        assertTrue("导出 JSON 应包含 app 标识", exportResult.json.contains("nutrition-tracker"))

        // 清空
        repo.clearRecords()
        repo.setTargets(NutritionTargets(0.0, 0.0, 0.0, 0.0, emptyList()))
        assertNull(repo.getAllRecords().first()["2026-06-20"])

        // 重新导入
        val importResult = backupManager.importData(exportResult.json)
        assertTrue("导入应成功: ${importResult.error}", importResult.success)

        // 验证数据恢复
        val restoredTargets = repo.getTargets().first()
        assertNotNull(restoredTargets)
        assertEquals(1800.0, restoredTargets!!.calories, 0.01)
        assertEquals(100.0, restoredTargets.protein, 0.01)

        val restoredRecords = repo.getAllRecords().first()
        val day = restoredRecords["2026-06-20"]
        assertNotNull(day)
        assertEquals(1, day!!.dinner.size)
        assertEquals("测试记录", day.dinner[0].name)
        assertEquals(500.0, day.dinner[0].calories, 0.01)
        assertEquals(1, day.dinner[0].micronutrients.size)
        assertEquals("iron", day.dinner[0].micronutrients[0].key)
        assertEquals(6.0, day.dinner[0].micronutrients[0].value, 0.01)
    }

    // ==================== 容错测试 ====================

    /**
     * 小程序 JSON 缺少部分字段时应使用默认值填充
     */
    @Test
    fun `缺少微量营养素的 JSON 仍可导入`() = runTest {
        val json = """
        {
            "app": "nutrition-tracker",
            "schemaVersion": 1,
            "exportedAt": "2026-06-15T10:30:00.000Z",
            "data": {
                "targets": {
                    "calories": 2000,
                    "protein": 120,
                    "fat": 65,
                    "carbs": 250,
                    "micronutrients": []
                },
                "records": {
                    "2026-06-14": {
                        "breakfast": [
                            {
                                "id": "r_1",
                                "name": "简单记录",
                                "calories": 300
                            }
                        ],
                        "lunch": [],
                        "dinner": [],
                        "snack": []
                    }
                }
            }
        }
        """.trimIndent()

        val result = backupManager.importData(json)
        assertTrue("缺少微量营养素的记录应能导入: ${result.error}", result.success)

        val records = repo.getAllRecords().first()
        val day = records["2026-06-14"]
        assertNotNull(day)
        assertEquals(1, day!!.breakfast.size)
        assertEquals("简单记录", day.breakfast[0].name)
        assertEquals(300.0, day.breakfast[0].calories, 0.01)
        // 缺少的宏量应为 0
        assertEquals(0.0, day.breakfast[0].protein, 0.01)
        assertEquals(0.0, day.breakfast[0].fat, 0.01)
        assertEquals(0.0, day.breakfast[0].carbs, 0.01)
    }

    // ==================== Calculator 数值一致性 ====================

    /**
     * 验证同一份数据经 Calculator 聚合后数值与小程序 calc.js 一致
     */
    @Test
    fun `Calculator 聚合结果与小程序数值一致`() {
        val dayData = DayRecords(
            dateStr = "2026-06-14",
            breakfast = listOf(
                MealRecord(calories = 350.0, protein = 15.5, fat = 8.2, carbs = 52.0),
                MealRecord(calories = 200.0, protein = 10.0, fat = 5.0, carbs = 30.0)
            ),
            lunch = listOf(
                MealRecord(calories = 420.0, protein = 35.0, fat = 12.0, carbs = 25.0)
            ),
            dinner = listOf(
                MealRecord(calories = 600.0, protein = 40.0, fat = 20.0, carbs = 55.0)
            ),
            snack = listOf(
                MealRecord(calories = 150.0, protein = 3.0, fat = 8.0, carbs = 18.0)
            )
        )

        val agg = Calculator.aggregateDay(dayData)

        // 热量 = 350 + 200 + 420 + 600 + 150 = 1720
        assertEquals(1720, agg.calories)

        // 蛋白质 = 15.5 + 10.0 + 35.0 + 40.0 + 3.0 = 103.5
        assertEquals(103.5, agg.protein, 0.01)

        // 脂肪 = 8.2 + 5.0 + 12.0 + 20.0 + 8.0 = 53.2
        assertEquals(53.2, agg.fat, 0.01)

        // 碳水 = 52.0 + 30.0 + 25.0 + 55.0 + 18.0 = 180.0
        assertEquals(180.0, agg.carbs, 0.01)

        // 各餐分布
        assertEquals(550, agg.meals[MealKey.BREAKFAST]?.calories)
        assertEquals(420, agg.meals[MealKey.LUNCH]?.calories)
        assertEquals(600, agg.meals[MealKey.DINNER]?.calories)
        assertEquals(150, agg.meals[MealKey.SNACK]?.calories)
    }

    // ==================== 测试用 FakeRepository ====================

    class TestRepo : LocalStorageRepository {
        private var targets: NutritionTargets? = null
        private val records = mutableMapOf<String, DayRecords>()
        private var meta: AppMeta? = null
        private val foodTemplates = mutableListOf<FoodTemplate>()
        private val bodyRecords = mutableMapOf<String, BodyRecord>()

        override fun getTargets(): Flow<NutritionTargets?> = flowOf(targets)
        override suspend fun setTargets(targets: NutritionTargets): Resource<Unit> {
            this.targets = targets; return Resource.Success(Unit)
        }
        override fun getAllRecords(): Flow<Map<String, DayRecords>> = flowOf(records.toMap())
        override fun getDayRecords(dateStr: String): Flow<DayRecords> =
            flowOf(records[dateStr] ?: DayRecords(dateStr = dateStr))
        override suspend fun setDayRecords(dateStr: String, dayData: DayRecords): Resource<Unit> {
            records[dateStr] = dayData; return Resource.Success(Unit)
        }
        override suspend fun addRecord(dateStr: String, mealKey: MealKey, record: MealRecord): Resource<Unit> {
            val day = records[dateStr] ?: DayRecords(dateStr = dateStr)
            records[dateStr] = day.withMeal(mealKey, day.getMeal(mealKey) + record)
            return Resource.Success(Unit)
        }
        override suspend fun updateRecord(dateStr: String, mealKey: MealKey, recordId: String, newData: MealRecord): Resource<Unit> {
            val day = records[dateStr] ?: DayRecords(dateStr = dateStr)
            val meal = day.getMeal(mealKey).toMutableList()
            val idx = meal.indexOfFirst { it.id == recordId }
            if (idx >= 0) meal[idx] = newData
            records[dateStr] = day.withMeal(mealKey, meal)
            return Resource.Success(Unit)
        }
        override suspend fun deleteRecord(dateStr: String, mealKey: MealKey, recordId: String): Resource<Unit> {
            val day = records[dateStr] ?: DayRecords(dateStr = dateStr)
            val meal = day.getMeal(mealKey).filter { it.id != recordId }
            records[dateStr] = day.withMeal(mealKey, meal)
            return Resource.Success(Unit)
        }
        override suspend fun clearRecords(): Resource<Unit> {
            records.clear(); return Resource.Success(Unit)
        }
        override fun getMeta(): Flow<AppMeta?> = flowOf(meta)
        override suspend fun setMeta(meta: AppMeta): Resource<Unit> {
            this.meta = meta; return Resource.Success(Unit)
        }
        override suspend fun bulkSet(
            targets: NutritionTargets?,
            records: Map<String, DayRecords>?,
            meta: AppMeta?,
            foodTemplates: List<FoodTemplate>?,
            bodyRecords: List<BodyRecord>?
        ): Resource<Unit> {
            targets?.let { this.targets = it }
            records?.let { this.records.clear(); this.records.putAll(it) }
            meta?.let { this.meta = it }
            foodTemplates?.let { this.foodTemplates.clear(); this.foodTemplates.addAll(it) }
            bodyRecords?.let {
                this.bodyRecords.clear()
                it.forEach { record -> this.bodyRecords[record.dateStr] = record }
            }
            return Resource.Success(Unit)
        }
        override suspend fun getStorageStatus(): StorageStatus = StorageStatus(100, 102400, 3)
        override suspend fun checkStorageCapacity(): CapacityStatus = CapacityStatus(true, false, 100, 102400, "正常")

        override fun getAllFoodTemplates(): Flow<List<FoodTemplate>> = flowOf(foodTemplates.toList())
        override suspend fun saveFoodTemplate(template: FoodTemplate): Resource<Unit> {
            val idx = foodTemplates.indexOfFirst { it.id == template.id }
            if (idx >= 0) foodTemplates[idx] = template else foodTemplates.add(template)
            return Resource.Success(Unit)
        }
        override suspend fun deleteFoodTemplate(id: String): Resource<Unit> =
            if (foodTemplates.removeIf { it.id == id }) Resource.Success(Unit)
            else Resource.Error("模板不存在或已被删除")

        override fun getAllBodyRecords(): Flow<List<BodyRecord>> = flowOf(bodyRecords.values.sortedBy { it.dateStr })
        override fun getBodyRecord(dateStr: String): Flow<BodyRecord?> = flowOf(bodyRecords[dateStr])
        override suspend fun saveBodyRecord(record: BodyRecord): Resource<Unit> {
            bodyRecords[record.dateStr] = record; return Resource.Success(Unit)
        }
        override suspend fun deleteBodyRecord(dateStr: String): Resource<Unit> =
            if (bodyRecords.remove(dateStr) != null) Resource.Success(Unit)
            else Resource.Error("记录不存在或已被删除")
    }
}
