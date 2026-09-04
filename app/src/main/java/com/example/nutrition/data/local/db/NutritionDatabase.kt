package com.example.nutrition.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.nutrition.data.local.entity.BodyRecordEntity
import com.example.nutrition.data.local.entity.DayRecordEntity
import com.example.nutrition.data.local.entity.FoodTemplateEntity
import com.example.nutrition.data.local.entity.MetaEntity
import com.example.nutrition.data.local.entity.TargetEntity

/**
 * Room 数据库
 *
 * 包含五张表：targets（营养目标）、day_records（每日记录）、meta（元信息）、
 * food_templates（食物模板）、body_records（身体记录）
 * 版本号为 3，与三期 schemaVersion 对齐
 */
@Database(
    entities = [
        TargetEntity::class,
        DayRecordEntity::class,
        MetaEntity::class,
        FoodTemplateEntity::class,
        BodyRecordEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class NutritionDatabase : RoomDatabase() {

    abstract fun targetDao(): TargetDao
    abstract fun recordDao(): RecordDao
    abstract fun metaDao(): MetaDao
    abstract fun foodTemplateDao(): FoodTemplateDao
    abstract fun bodyRecordDao(): BodyRecordDao

    companion object {
        private const val DB_NAME = "nutrition.db"

        @Volatile
        private var INSTANCE: NutritionDatabase? = null

        fun getInstance(context: Context): NutritionDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NutritionDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        /**
         * 3 → 4：食物模板新增 tagsJson 字段
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE food_templates ADD COLUMN tagsJson TEXT NOT NULL DEFAULT '[]'")
            }
        }

        /**
         * 1 → 2：新增 targets.bodyProfileJson 和 isAutoCalculated 字段
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE targets ADD COLUMN bodyProfileJson TEXT")
                db.execSQL("ALTER TABLE targets ADD COLUMN isAutoCalculated INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * 2 → 3：新增 food_templates（食物模板）和 body_records（身体记录）表
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS food_templates (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        calories REAL NOT NULL,
                        protein REAL NOT NULL,
                        fat REAL NOT NULL,
                        carbs REAL NOT NULL,
                        micronutrientsJson TEXT NOT NULL,
                        isPreset INTEGER NOT NULL DEFAULT 0,
                        source TEXT NOT NULL DEFAULT '',
                        createdAt TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS body_records (
                        dateStr TEXT PRIMARY KEY NOT NULL,
                        weightKg REAL NOT NULL,
                        bodyFatPercent REAL,
                        muscleKg REAL,
                        note TEXT,
                        createdAt TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
