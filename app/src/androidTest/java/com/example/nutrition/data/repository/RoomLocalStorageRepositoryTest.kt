package com.example.nutrition.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nutrition.data.local.db.NutritionDatabase
import com.example.nutrition.domain.model.AppMeta
import com.example.nutrition.domain.model.DayRecords
import com.example.nutrition.domain.model.MealKey
import com.example.nutrition.domain.model.MealMicro
import com.example.nutrition.domain.model.MealRecord
import com.example.nutrition.domain.model.MicronutrientTarget
import com.example.nutrition.domain.model.NutritionTargets
import com.example.nutrition.domain.model.Resource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * RoomLocalStorageRepository 集成测试
 *
 * 使用 Room in-memory 数据库，覆盖 CRUD + 边界场景
 */
@RunWith(AndroidJUnit4::class)
class RoomLocalStorageRepositoryTest {

    private lateinit var db: NutritionDatabase
    private lateinit var repo: RoomLocalStorageRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, NutritionDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = RoomLocalStorageRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ==================== 辅助方法 ====================

    private fun sampleTargets() = NutritionTargets(
        calories = 2000.0,
        protein = 120.0,
        fat = 65.0,
        carbs = 250.0,
        micronutrients = listOf(
            MicronutrientTarget("vitC", "维生素C", "mg", 100.0),
            MicronutrientTarget("calcium", "钙", "mg", 800.0)
        )
    )

    private fun sampleRecord(
        name: String = "鸡蛋",
        calories: Double = 155.0,
        protein: Double = 13.0,
        fat: Double = 11.0,
        carbs: Double = 1.1
    ) = MealRecord(
        name = name,
        calories = calories,
        protein = protein,
        fat = fat,
        carbs = carbs,
        micronutrients = listOf(MealMicro("vitC", 5.0))
    )

    private fun sampleMeta() = AppMeta(
        version = "1.0.0",
        firstUseDate = "2026-06-30",
        schemaVersion = 1,
        hasSeenGuide = false
    )

    // ==================== 营养目标测试 ====================

    @Test
    fun getTargets_初始为空() = runTest {
        assertNull(repo.getTargets().first())
    }

    @Test
    fun setTargets_写入并读取() = runTest {
        val targets = sampleTargets()
        assertTrue(repo.setTargets(targets) is Resource.Success)

        val loaded = repo.getTargets().first()
        assertNotNull(loaded)
        assertEquals(2000.0, loaded!!.calories, 0.001)
        assertEquals(120.0, loaded.protein, 0.001)
        assertEquals(65.0, loaded.fat, 0.001)
        assertEquals(250.0, loaded.carbs, 0.001)
        assertEquals(2, loaded.micronutrients.size)
    }

    @Test
    fun setTargets_覆盖写入() = runTest {
        repo.setTargets(sampleTargets())

        val newTargets = sampleTargets().copy(calories = 2500.0, protein = 150.0)
        assertTrue(repo.setTargets(newTargets) is Resource.Success)

        val loaded = repo.getTargets().first()
        assertEquals(2500.0, loaded!!.calories, 0.001)
        assertEquals(150.0, loaded.protein, 0.001)
    }

    // ==================== 每日记录测试 ====================

    @Test
    fun getAllRecords_初始为空() = runTest {
        val records = repo.getAllRecords().first()
        assertTrue(records.isEmpty())
    }

    @Test
    fun getDayRecords_不存在的日期返回空() = runTest {
        val day = repo.getDayRecords("2026-06-30").first()
        assertEquals("2026-06-30", day.dateStr)
        assertTrue(day.isEmpty())
    }

    @Test
    fun setDayRecords_写入并读取() = runTest {
        val day = DayRecords(
            dateStr = "2026-06-30",
            breakfast = listOf(sampleRecord()),
            lunch = listOf(sampleRecord(name = "米饭", calories = 200.0))
        )
        assertTrue(repo.setDayRecords("2026-06-30", day) is Resource.Success)

        val loaded = repo.getDayRecords("2026-06-30").first()
        assertEquals(1, loaded.breakfast.size)
        assertEquals("鸡蛋", loaded.breakfast[0].name)
        assertEquals(1, loaded.lunch.size)
        assertEquals(0, loaded.dinner.size)
        assertEquals(0, loaded.snack.size)
    }

    @Test
    fun addRecord_添加记录到指定餐次() = runTest {
        val record = sampleRecord()
        assertTrue(repo.addRecord("2026-06-30", MealKey.BREAKFAST, record) is Resource.Success)

        val day = repo.getDayRecords("2026-06-30").first()
        assertEquals(1, day.breakfast.size)
        assertEquals("鸡蛋", day.breakfast[0].name)
        assertEquals(0, day.lunch.size)
    }

    @Test
    fun addRecord_多条记录累加() = runTest {
        repo.addRecord("2026-06-30", MealKey.BREAKFAST, sampleRecord(name = "鸡蛋"))
        repo.addRecord("2026-06-30", MealKey.BREAKFAST, sampleRecord(name = "牛奶"))

        val day = repo.getDayRecords("2026-06-30").first()
        assertEquals(2, day.breakfast.size)
        assertEquals("鸡蛋", day.breakfast[0].name)
        assertEquals("牛奶", day.breakfast[1].name)
    }

    @Test
    fun updateRecord_更新指定记录() = runTest {
        val record = sampleRecord()
        repo.addRecord("2026-06-30", MealKey.LUNCH, record)

        val day = repo.getDayRecords("2026-06-30").first()
        val recordId = day.lunch[0].id

        val updated = sampleRecord(name = "炒饭", calories = 500.0)
        assertTrue(repo.updateRecord("2026-06-30", MealKey.LUNCH, recordId, updated) is Resource.Success)

        val reloaded = repo.getDayRecords("2026-06-30").first()
        assertEquals("炒饭", reloaded.lunch[0].name)
        assertEquals(500.0, reloaded.lunch[0].calories, 0.001)
        assertEquals(recordId, reloaded.lunch[0].id)
        // createdAt 应保留
        assertEquals(day.lunch[0].createdAt, reloaded.lunch[0].createdAt)
        // updatedAt 应被设置
        assertNotNull(reloaded.lunch[0].updatedAt)
    }

    @Test
    fun updateRecord_不存在的_ID_返回_Error() = runTest {
        repo.addRecord("2026-06-30", MealKey.LUNCH, sampleRecord())
        val result = repo.updateRecord(
            "2026-06-30", MealKey.LUNCH, "non-existent-id", sampleRecord()
        )
        assertTrue(result is Resource.Error)
    }

    @Test
    fun deleteRecord_删除指定记录() = runTest {
        val record = sampleRecord()
        repo.addRecord("2026-06-30", MealKey.DINNER, record)

        val day = repo.getDayRecords("2026-06-30").first()
        val recordId = day.dinner[0].id

        assertTrue(repo.deleteRecord("2026-06-30", MealKey.DINNER, recordId) is Resource.Success)

        val reloaded = repo.getDayRecords("2026-06-30").first()
        assertEquals(0, reloaded.dinner.size)
    }

    @Test
    fun deleteRecord_不存在的_ID_返回_Error() = runTest {
        repo.addRecord("2026-06-30", MealKey.DINNER, sampleRecord())
        assertTrue(repo.deleteRecord("2026-06-30", MealKey.DINNER, "non-existent-id") is Resource.Error)
    }

    @Test
    fun clearRecords_清空全部记录() = runTest {
        repo.addRecord("2026-06-29", MealKey.BREAKFAST, sampleRecord())
        repo.addRecord("2026-06-30", MealKey.LUNCH, sampleRecord())

        assertTrue(repo.clearRecords() is Resource.Success)

        val allRecords = repo.getAllRecords().first()
        assertTrue(allRecords.isEmpty())
    }

    @Test
    fun getAllRecords_多日期记录() = runTest {
        repo.addRecord("2026-06-29", MealKey.BREAKFAST, sampleRecord())
        repo.addRecord("2026-06-30", MealKey.LUNCH, sampleRecord())
        repo.addRecord("2026-07-01", MealKey.DINNER, sampleRecord())

        val allRecords = repo.getAllRecords().first()
        assertEquals(3, allRecords.size)
        assertTrue(allRecords.containsKey("2026-06-29"))
        assertTrue(allRecords.containsKey("2026-06-30"))
        assertTrue(allRecords.containsKey("2026-07-01"))
    }

    // ==================== 元信息测试 ====================

    @Test
    fun getMeta_初始为空() = runTest {
        assertNull(repo.getMeta().first())
    }

    @Test
    fun setMeta_写入并读取() = runTest {
        val meta = sampleMeta()
        assertTrue(repo.setMeta(meta) is Resource.Success)

        val loaded = repo.getMeta().first()
        assertNotNull(loaded)
        assertEquals("1.0.0", loaded!!.version)
        assertEquals("2026-06-30", loaded.firstUseDate)
        assertEquals(1, loaded.schemaVersion)
        assertFalse(loaded.hasSeenGuide)
    }

    @Test
    fun setMeta_覆盖写入() = runTest {
        repo.setMeta(sampleMeta())
        val updated = sampleMeta().copy(hasSeenGuide = true, lastExportDate = "2026-07-01")
        assertTrue(repo.setMeta(updated) is Resource.Success)

        val loaded = repo.getMeta().first()
        assertTrue(loaded!!.hasSeenGuide)
        assertEquals("2026-07-01", loaded.lastExportDate)
    }

    // ==================== 批量操作测试 ====================

    @Test
    fun bulkSet_同时写入目标_记录_元信息() = runTest {
        val targets = sampleTargets()
        val records = mapOf(
            "2026-06-30" to DayRecords(
                dateStr = "2026-06-30",
                breakfast = listOf(sampleRecord())
            )
        )
        val meta = sampleMeta()

        assertTrue(repo.bulkSet(targets = targets, records = records, meta = meta) is Resource.Success)

        val loadedTargets = repo.getTargets().first()
        assertNotNull(loadedTargets)
        assertEquals(2000.0, loadedTargets!!.calories, 0.001)

        val loadedRecords = repo.getAllRecords().first()
        assertEquals(1, loadedRecords.size)

        val loadedMeta = repo.getMeta().first()
        assertNotNull(loadedMeta)
    }

    @Test
    fun bulkSet_部分写入() = runTest {
        // 只写目标，不写记录和元信息
        assertTrue(repo.bulkSet(targets = sampleTargets()) is Resource.Success)
        assertNotNull(repo.getTargets().first())
        assertNull(repo.getMeta().first())
        assertTrue(repo.getAllRecords().first().isEmpty())
    }

    // ==================== 存储状态测试 ====================

    @Test
    fun getStorageStatus_返回合理值() = runTest {
        val status = repo.getStorageStatus()
        assertNotNull(status)
        assertTrue(status.currentSize >= 0)
        assertTrue(status.limitSize > 0)
    }

    @Test
    fun checkStorageCapacity_初始状态正常() = runTest {
        val capacity = repo.checkStorageCapacity()
        assertTrue(capacity.ok)
        assertFalse(capacity.warn)
        assertTrue(capacity.message.isEmpty())
    }

    // ==================== 边界场景测试 ====================

    @Test
    fun 微量营养素正确序列化反序列化() = runTest {
        val record = MealRecord(
            name = "综合维生素",
            calories = 10.0,
            micronutrients = listOf(
                MealMicro("vitA", 800.0),
                MealMicro("vitC", 100.0),
                MealMicro("calcium", 500.0),
                MealMicro("iron", 12.0)
            )
        )
        repo.addRecord("2026-06-30", MealKey.SNACK, record)

        val loaded = repo.getDayRecords("2026-06-30").first()
        val micros = loaded.snack[0].micronutrients
        assertEquals(4, micros.size)
        assertEquals("vitA", micros[0].key)
        assertEquals(800.0, micros[0].value, 0.001)
    }

    @Test
    fun 同一天多次覆盖写入() = runTest {
        val day1 = DayRecords(
            dateStr = "2026-06-30",
            breakfast = listOf(sampleRecord(name = "面包"))
        )
        repo.setDayRecords("2026-06-30", day1)

        val day2 = DayRecords(
            dateStr = "2026-06-30",
            breakfast = listOf(sampleRecord(name = "粥")),
            lunch = listOf(sampleRecord(name = "面条"))
        )
        repo.setDayRecords("2026-06-30", day2)

        val loaded = repo.getDayRecords("2026-06-30").first()
        assertEquals(1, loaded.breakfast.size)
        assertEquals("粥", loaded.breakfast[0].name)
        assertEquals(1, loaded.lunch.size)
    }

    @Test
    fun 跨餐次操作互不影响() = runTest {
        repo.addRecord("2026-06-30", MealKey.BREAKFAST, sampleRecord(name = "早餐"))
        repo.addRecord("2026-06-30", MealKey.LUNCH, sampleRecord(name = "午餐"))
        repo.addRecord("2026-06-30", MealKey.DINNER, sampleRecord(name = "晚餐"))
        repo.addRecord("2026-06-30", MealKey.SNACK, sampleRecord(name = "加餐"))

        val day = repo.getDayRecords("2026-06-30").first()
        assertEquals(1, day.breakfast.size)
        assertEquals(1, day.lunch.size)
        assertEquals(1, day.dinner.size)
        assertEquals(1, day.snack.size)
        assertEquals(4, day.totalCount())

        // 删除午餐记录，其他不受影响
        repo.deleteRecord("2026-06-30", MealKey.LUNCH, day.lunch[0].id)

        val reloaded = repo.getDayRecords("2026-06-30").first()
        assertEquals(1, reloaded.breakfast.size)
        assertEquals(0, reloaded.lunch.size)
        assertEquals(1, reloaded.dinner.size)
        assertEquals(1, reloaded.snack.size)
    }
}
