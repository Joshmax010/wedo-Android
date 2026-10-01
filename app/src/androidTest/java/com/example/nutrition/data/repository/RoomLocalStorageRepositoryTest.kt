package com.example.nutrition.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nutrition.data.local.db.NutritionDatabase
import com.example.nutrition.data.local.entity.BodyRecordEntity
import com.example.nutrition.data.local.entity.DayRecordEntity
import com.example.nutrition.data.local.entity.FoodTemplateEntity
import com.example.nutrition.data.local.entity.MetaEntity
import com.example.nutrition.data.local.entity.TargetEntity
import com.example.nutrition.domain.constants.PresetFoodTemplates
import com.example.nutrition.domain.model.AppMeta
import com.example.nutrition.domain.model.BodyRecord
import com.example.nutrition.domain.model.DayRecords
import com.example.nutrition.domain.model.FoodTemplate
import com.example.nutrition.domain.model.MealKey
import com.example.nutrition.domain.model.MealMicro
import com.example.nutrition.domain.model.MealRecord
import com.example.nutrition.domain.model.MicronutrientTarget
import com.example.nutrition.domain.model.NutritionTargets
import com.example.nutrition.domain.model.Resource
import kotlinx.serialization.SerializationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

    private data class DatabaseSnapshot(
        val targets: TargetEntity?,
        val records: List<DayRecordEntity>,
        val meta: MetaEntity?,
        val templates: List<FoodTemplateEntity>,
        val bodyRecords: List<BodyRecordEntity>
    )

    // 直接比较五张表，避免读取模板的 Flow 时触发预设初始化。
    private suspend fun snapshotDatabase() = DatabaseSnapshot(
        targets = db.targetDao().get(),
        records = db.recordDao().getAll(),
        meta = db.metaDao().get(),
        templates = db.foodTemplateDao().getAll(),
        bodyRecords = db.bodyRecordDao().getAll()
    )

    private suspend fun seedImportData(): DatabaseSnapshot {
        assertTrue(
            repo.bulkSet(
                targets = sampleTargets(),
                records = mapOf(
                    "2026-06-29" to DayRecords(
                        dateStr = "2026-06-29", lunch = listOf(sampleRecord(name = "保留的记录"))
                    ),
                    "2026-06-30" to DayRecords(
                        dateStr = "2026-06-30", breakfast = listOf(sampleRecord(name = "原记录"))
                    )
                ),
                meta = sampleMeta(),
                foodTemplates = listOf(
                    FoodTemplate(id = "existing-preset", name = "已编辑预设", calories = 123.0, isPreset = true),
                    FoodTemplate(id = "existing-custom", name = "原自定义模板", calories = 456.0)
                ),
                bodyRecords = listOf(BodyRecord(dateStr = "2026-06-30", weightKg = 70.0))
            ) is Resource.Success
        )
        return snapshotDatabase()
    }

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

    private suspend fun runConcurrentWrites(actions: List<suspend () -> Resource<Unit>>) = coroutineScope {
        val gate = CompletableDeferred<Unit>()
        val jobs = actions.map { action ->
            async(Dispatchers.Default) { gate.await(); action() }
        }
        gate.complete(Unit)
        jobs.awaitAll().forEach { assertTrue(it is Resource.Success) }
    }

    @Test
    fun addRecord_并发新增保留全部记录() = runTest {
        val records = (1..40).map { sampleRecord(name = "并发记录$it") }
        runConcurrentWrites(records.map { record ->
            suspend { repo.addRecord("2026-06-30", MealKey.LUNCH, record) }
        })
        assertEquals(records.map { it.id }.toSet(), repo.getDayRecords("2026-06-30").first().lunch.map { it.id }.toSet())
    }

    @Test
    fun updateRecord_并发编辑不同记录不覆盖彼此() = runTest {
        val records = (1..10).map { sampleRecord(name = "记录$it") }
        assertTrue(repo.setDayRecords("2026-06-30", DayRecords("2026-06-30", lunch = records)) is Resource.Success)
        runConcurrentWrites(records.map { record ->
            suspend { repo.updateRecord("2026-06-30", MealKey.LUNCH, record.id, record.copy(name = "已编辑${record.name}")) }
        })
        val loaded = repo.getDayRecords("2026-06-30").first().lunch
        assertEquals(records.map { it.id }.toSet(), loaded.map { it.id }.toSet())
        assertEquals(records.map { "已编辑${it.name}" }.toSet(), loaded.map { it.name }.toSet())
        records.forEach { original -> assertEquals(original.createdAt, loaded.single { it.id == original.id }.createdAt) }
    }

    @Test
    fun deleteRecord_并发删除不同记录不恢复已删除项() = runTest {
        val records = (1..10).map { sampleRecord(name = "记录$it") }
        assertTrue(repo.setDayRecords("2026-06-30", DayRecords("2026-06-30", lunch = records)) is Resource.Success)
        runConcurrentWrites(records.dropLast(1).map { record ->
            suspend { repo.deleteRecord("2026-06-30", MealKey.LUNCH, record.id) }
        })
        assertEquals(listOf(records.last()), repo.getDayRecords("2026-06-30").first().lunch)
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

    @Test
    fun bulkSet_成功导入五张表_保留预设和未导入日期() = runTest {
        val before = seedImportData()
        val targets = sampleTargets().copy(calories = 2500.0)
        val records = mapOf(
            "2026-06-30" to DayRecords(
                dateStr = "2026-06-30", dinner = listOf(sampleRecord(name = "替换记录"))
            ),
            "2026-07-01" to DayRecords(
                dateStr = "2026-07-01", lunch = listOf(sampleRecord(name = "新增记录"))
            )
        )
        val meta = sampleMeta().copy(hasSeenGuide = true)
        val template = FoodTemplate(id = "imported-custom", name = "导入模板", calories = 300.0)
        val bodyRecord = BodyRecord(dateStr = "2026-07-01", weightKg = 68.0)

        assertTrue(
            repo.bulkSet(
                targets = targets, records = records, meta = meta,
                foodTemplates = listOf(template), bodyRecords = listOf(bodyRecord)
            ) is Resource.Success
        )

        assertEquals(targets, repo.getTargets().first())
        assertEquals(meta, repo.getMeta().first())
        val loadedRecords = repo.getAllRecords().first()
        assertEquals(setOf("2026-06-29", "2026-06-30", "2026-07-01"), loadedRecords.keys)
        records.forEach { (date, day) -> assertEquals(day, loadedRecords[date]) }
        val after = snapshotDatabase()
        assertEquals(
            before.records.single { it.dateStr == "2026-06-29" },
            after.records.single { it.dateStr == "2026-06-29" }
        )
        assertEquals(before.templates.filter { it.isPreset }, after.templates.filter { it.isPreset })
        assertEquals(listOf(template.id), after.templates.filter { !it.isPreset }.map { it.id })
        assertEquals(template.name, after.templates.single { !it.isPreset }.name)
        assertEquals(listOf(bodyRecord), repo.getAllBodyRecords().first())
    }

    @Test
    fun bulkSet_null字段保留已有数据() = runTest {
        val before = seedImportData()
        val targets = sampleTargets().copy(calories = 2500.0)

        assertTrue(repo.bulkSet(targets = targets) is Resource.Success)

        assertEquals(targets, repo.getTargets().first())
        assertEquals(before, snapshotDatabase().copy(targets = before.targets))
    }

    @Test
    fun bulkSet_空列表清空自定义模板和身体记录_保留预设() = runTest {
        val before = seedImportData()

        assertTrue(
            repo.bulkSet(foodTemplates = emptyList(), bodyRecords = emptyList()) is Resource.Success
        )

        assertEquals(
            before.copy(templates = before.templates.filter { it.isPreset }, bodyRecords = emptyList()),
            snapshotDatabase()
        )
    }

    @Test
    fun bulkSet_最后一张表写入失败_回滚五张表的全部变更() = runTest {
        val before = seedImportData()
        // 在最后一个身体记录插入时制造真实 SQLite 写入失败。
        // 前面的覆盖、删除以及第一条身体记录插入都必须一起回滚。
        db.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER fail_body_import BEFORE INSERT ON body_records
            WHEN NEW.dateStr = '2026-07-02'
            BEGIN
                SELECT RAISE(ABORT, 'Simulated import write failure');
            END
            """.trimIndent()
        )

        val result = repo.bulkSet(
            targets = sampleTargets().copy(calories = 2500.0),
            records = mapOf(
                "2026-06-30" to DayRecords(
                    dateStr = "2026-06-30", dinner = listOf(sampleRecord(name = "替换记录"))
                ),
                "2026-07-01" to DayRecords(
                    dateStr = "2026-07-01", lunch = listOf(sampleRecord(name = "新增记录"))
                )
            ),
            meta = sampleMeta().copy(hasSeenGuide = true),
            foodTemplates = listOf(FoodTemplate(id = "imported-custom", name = "导入模板", calories = 300.0)),
            bodyRecords = listOf(
                BodyRecord(dateStr = "2026-07-01", weightKg = 68.0),
                BodyRecord(dateStr = "2026-07-02", weightKg = 69.0)
            )
        )

        assertEquals(Resource.Error("数据导入失败，请重试"), result)
        assertEquals(before, snapshotDatabase())
    }

    @Test
    fun bulkSet_目标回读校验失败_回滚已写入的目标() = runTest {
        val before = seedImportData()
        db.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER alter_imported_target AFTER INSERT ON targets
            BEGIN
                UPDATE targets SET calories = NEW.calories + 1 WHERE id = NEW.id;
            END
            """.trimIndent()
        )

        val result = repo.bulkSet(targets = sampleTargets().copy(calories = 2500.0))

        assertEquals(Resource.Error("数据校验失败，请重试"), result)
        assertEquals(before, snapshotDatabase())
    }

    @Test
    fun bulkSet_元信息回读校验失败_回滚之前的目标和饮食记录() = runTest {
        val before = seedImportData()
        db.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER alter_imported_meta AFTER INSERT ON meta
            BEGIN
                UPDATE meta SET schemaVersion = NEW.schemaVersion + 1 WHERE id = NEW.id;
            END
            """.trimIndent()
        )

        val result = repo.bulkSet(
            targets = sampleTargets().copy(calories = 2500.0),
            records = mapOf(
                "2026-06-30" to DayRecords(
                    dateStr = "2026-06-30", dinner = listOf(sampleRecord(name = "替换记录"))
                )
            ),
            meta = sampleMeta().copy(hasSeenGuide = true)
        )

        assertEquals(Resource.Error("数据校验失败，请重试"), result)
        assertEquals(before, snapshotDatabase())
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

    @Test
    fun getDayRecords_JSON损坏时报告读取失败_不返回空记录() = runTest {
        db.recordDao().insert(DayRecordEntity(dateStr = "2026-06-30", breakfastJson = "invalid-json"))
        try {
            repo.getDayRecords("2026-06-30").first()
            fail("损坏的 JSON 应报告读取失败")
        } catch (_: SerializationException) {
            // Expected; an absent row is already tested separately as an empty day.
        }
    }

    @Test
    fun addRecord_读取损坏数据失败时不覆盖原始行() = runTest {
        val damaged = DayRecordEntity(dateStr = "2026-06-30", breakfastJson = "invalid-json")
        db.recordDao().insert(damaged)
        assertTrue(repo.addRecord("2026-06-30", MealKey.LUNCH, sampleRecord()) is Resource.Error)
        assertEquals(damaged, db.recordDao().getByDate("2026-06-30"))
    }

    @Test
    fun getAllFoodTemplates_并发订阅仅初始化一次() = runTest {
        db.openHelper.writableDatabase.execSQL("CREATE TABLE preset_attempts (id TEXT NOT NULL)")
        db.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER audit_preset_insert BEFORE INSERT ON food_templates
            WHEN NEW.isPreset = 1
            BEGIN
                INSERT INTO preset_attempts (id) VALUES (NEW.id);
            END
            """.trimIndent()
        )
        val gate = CompletableDeferred<Unit>()
        val results = coroutineScope {
            val jobs = (1..4).map {
                async(Dispatchers.Default) { gate.await(); repo.getAllFoodTemplates().first() }
            }
            gate.complete(Unit)
            jobs.awaitAll()
        }
        val expectedIds = PresetFoodTemplates.getAll().map { it.id }.toSet()
        results.forEach { assertEquals(expectedIds, it.map { template -> template.id }.toSet()) }
        repo.getAllFoodTemplates().first()
        db.openHelper.writableDatabase.query("SELECT COUNT(*) FROM preset_attempts").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(expectedIds.size, cursor.getInt(0))
        }
    }

    @Test
    fun getAllFoodTemplates_初始化保留已编辑预设和自定义模板() = runTest {
        val edited = PresetFoodTemplates.getAll().first().copy(calories = 321.0, tags = listOf("我的标签"))
        val custom = FoodTemplate(id = "my-custom", name = "自定义模板", calories = 400.0)
        assertTrue(repo.saveFoodTemplate(edited) is Resource.Success)
        assertTrue(repo.saveFoodTemplate(custom) is Resource.Success)
        val templates = repo.getAllFoodTemplates().first()
        assertEquals(edited, templates.single { it.id == edited.id })
        assertEquals(custom, templates.single { it.id == custom.id })
    }

    @Test
    fun getAllFoodTemplates_初始化失败回滚且允许重试() = runTest {
        val custom = FoodTemplate(id = "my-custom", name = "自定义模板", calories = 400.0)
        assertTrue(repo.saveFoodTemplate(custom) is Resource.Success)
        val before = snapshotDatabase()
        val lastId = PresetFoodTemplates.getAll().last().id.replace("'", "''")
        db.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER fail_preset_init BEFORE INSERT ON food_templates
            WHEN NEW.id = '$lastId'
            BEGIN
                SELECT RAISE(ABORT, 'Simulated preset initialization failure');
            END
            """.trimIndent()
        )
        try {
            repo.getAllFoodTemplates().first()
            fail("Initialization should fail")
        } catch (_: android.database.sqlite.SQLiteException) {
            assertEquals(before, snapshotDatabase())
        }
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_preset_init")
        val templates = repo.getAllFoodTemplates().first()
        assertEquals(PresetFoodTemplates.getAll().size, templates.count { it.isPreset })
        assertEquals(custom, templates.single { it.id == custom.id })
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
