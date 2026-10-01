package com.example.nutrition

import android.app.Application
import android.util.Log
import com.example.nutrition.data.local.db.NutritionDatabase
import com.example.nutrition.data.repository.RoomLocalStorageRepository
import com.example.nutrition.domain.constants.NutrientConstants
import com.example.nutrition.domain.repository.LocalStorageRepository
import com.example.nutrition.domain.usecase.BackupManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NutritionApp : Application() {

    val database: NutritionDatabase by lazy { NutritionDatabase.getInstance(this) }
    val repository: LocalStorageRepository by lazy { RoomLocalStorageRepository(database) }
    val backupManager: BackupManager by lazy { BackupManager(repository) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 启动时迁移 schema 版本
        appScope.launch {
            try {
                migrateSchemaVersion()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("NutritionApp", "Unable to read application metadata", e)
            }
        }
    }

    /**
     * 检查并迁移 AppMeta 的 schemaVersion 到最新版本
     */
    private suspend fun migrateSchemaVersion() {
        val meta = repository.getMeta().first() ?: return
        val latest = NutrientConstants.SCHEMA_VERSION
        if (meta.schemaVersion < latest) {
            repository.setMeta(meta.copy(schemaVersion = latest))
        }
    }

    companion object {
        lateinit var instance: NutritionApp
            private set
    }
}
