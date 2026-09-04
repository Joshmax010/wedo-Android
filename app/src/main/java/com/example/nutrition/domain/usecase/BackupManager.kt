package com.example.nutrition.domain.usecase

import com.example.nutrition.domain.constants.NutrientConstants
import com.example.nutrition.domain.model.ActivityLevel
import com.example.nutrition.domain.model.AppMeta
import com.example.nutrition.domain.model.BodyProfile
import com.example.nutrition.domain.model.BodyRecord
import com.example.nutrition.domain.model.DayRecords
import com.example.nutrition.domain.model.FoodTemplate
import com.example.nutrition.domain.model.Gender
import com.example.nutrition.domain.model.MealKey
import com.example.nutrition.domain.model.MealMicro
import com.example.nutrition.domain.model.MealRecord
import com.example.nutrition.domain.model.MicronutrientTarget
import com.example.nutrition.domain.model.NutritionTargets
import com.example.nutrition.domain.model.Resource
import com.example.nutrition.domain.repository.LocalStorageRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.time.Instant
import java.util.UUID

/**
 * 备份管理器 —— 移植自小程序 utils/backup.js
 *
 * 支持导出/导入/校验/预览，备份 JSON Schema 与小程序完全一致：
 * { app: "nutrition-tracker", schemaVersion: 1, exportedAt, data: { targets, records, meta } }
 */
class BackupManager(
    private val repository: LocalStorageRepository
) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    // ==================== 结果数据类 ====================

    data class ExportResult(val success: Boolean, val json: String, val error: String)
    data class ValidationResult(val valid: Boolean, val error: String)
    data class PreviewResult(val valid: Boolean, val error: String, val preview: BackupPreview?)
    data class ImportResult(val success: Boolean, val error: String, val summary: String)

    // ==================== 导出 DTO ====================

    @Serializable
    data class BackupExport(
        val app: String,
        val schemaVersion: Int,
        val exportedAt: String,
        val data: BackupData
    )

    @Serializable
    data class BackupData(
        val targets: NutritionTargets? = null,
        val records: Map<String, DayRecords> = emptyMap(),
        val meta: AppMeta? = null,
        val foodTemplates: List<FoodTemplate> = emptyList(),
        val bodyRecords: List<BodyRecord> = emptyList()
    )

    // ==================== 预览 DTO ====================

    data class BackupPreview(
        val dayCount: Int,
        val recordCount: Int,
        val earliestDate: String,
        val latestDate: String,
        val hasTargets: Boolean,
        val targetCalories: Double?,
        val targetProtein: Double?,
        val exportedAt: String,
        val schemaVersion: Int,
        val templateCount: Int = 0,
        val bodyRecordCount: Int = 0
    )

    // ==================== S4.2: 导出 ====================

    /**
     * 导出全部本地数据为 JSON 字符串
     * 含导出后校验，确保数据完整性
     */
    suspend fun exportData(): ExportResult {
        val targets = repository.getTargets().first()
        val records = repository.getAllRecords().first()
        val meta = repository.getMeta().first()
        val foodTemplates = repository.getAllFoodTemplates().first()
            .filter { !it.isPreset }
        val bodyRecords = repository.getAllBodyRecords().first()

        val exportObj = BackupExport(
            app = NutrientConstants.APP_NAME,
            schemaVersion = NutrientConstants.SCHEMA_VERSION,
            exportedAt = Instant.now().toString(),
            data = BackupData(
                targets = targets,
                records = records,
                meta = meta,
                foodTemplates = foodTemplates,
                bodyRecords = bodyRecords
            )
        )

        val jsonStr = json.encodeToString(exportObj)

        // 导出后校验
        val verification = verifyExportIntegrity(jsonStr)
        if (!verification.valid) {
            return ExportResult(false, "", verification.error)
        }

        // 更新元信息的上次导出时间
        if (meta != null) {
            repository.setMeta(meta.copy(lastExportDate = Instant.now().toString()))
        }

        return ExportResult(true, jsonStr, "")
    }

    // ==================== S4.6: 导出完整性校验 ====================

    /**
     * 校验导出的 JSON 字符串完整性
     */
    fun verifyExportIntegrity(jsonStr: String): ValidationResult {
        val parsed: JsonObject = try {
            Json.parseToJsonElement(jsonStr).jsonObject
        } catch (e: Exception) {
            return ValidationResult(false, "导出数据 JSON 解析失败")
        }

        val app = parsed["app"]?.jsonPrimitive?.contentOrNull
        if (app.isNullOrEmpty() || app != NutrientConstants.APP_NAME) {
            return ValidationResult(false, "导出数据缺少应用标识")
        }

        val schemaVersion = parsed["schemaVersion"]?.jsonPrimitive?.intOrNull
        if (schemaVersion == null) {
            return ValidationResult(false, "导出数据缺少版本号")
        }

        val exportedAt = parsed["exportedAt"]?.jsonPrimitive?.contentOrNull
        if (exportedAt.isNullOrEmpty()) {
            return ValidationResult(false, "导出数据缺少导出时间")
        }

        val data = parsed["data"]?.jsonObject
        if (data == null) {
            return ValidationResult(false, "导出数据缺少 data 字段")
        }

        if (!data.containsKey("targets") || data["targets"] is JsonNull) {
            return ValidationResult(false, "导出数据缺少目标配置")
        }

        if (!data.containsKey("records")) {
            return ValidationResult(false, "导出数据缺少记录数据")
        }

        return ValidationResult(true, "")
    }

    // ==================== S4.3: 校验 ====================

    /**
     * 校验备份数据的结构合法性
     */
    fun validateBackup(data: JsonObject): ValidationResult {
        // 校验 app 字段
        val app = data["app"]?.jsonPrimitive?.contentOrNull
        if (app != NutrientConstants.APP_NAME) {
            return ValidationResult(false, "应用标识不匹配：不是营养记录的备份数据")
        }

        // 校验 schemaVersion
        val schemaVersion = data["schemaVersion"]?.jsonPrimitive?.intOrNull ?: 0
        if (schemaVersion == 0) {
            return ValidationResult(false, "缺少数据版本号")
        }
        if (schemaVersion > NutrientConstants.SCHEMA_VERSION) {
            return ValidationResult(
                false,
                "数据版本不兼容：备份版本($schemaVersion)高于当前支持版本(${NutrientConstants.SCHEMA_VERSION})"
            )
        }

        // 校验 data 子对象
        val dataObj = data["data"]?.jsonObject
            ?: return ValidationResult(false, "数据结构无效：缺少 data 字段")

        // 校验 targets 结构（存在时才校验）
        dataObj["targets"]?.let { targetElement ->
            if (targetElement !is JsonNull) {
                val targetObj = targetElement as? JsonObject
                    ?: return ValidationResult(false, "目标配置格式错误：不是对象")
                val calories = targetObj["calories"]?.jsonPrimitive?.doubleOrNull
                if (calories == null) {
                    return ValidationResult(false, "目标配置缺少热量字段或类型错误")
                }
            }
        }

        // 校验 records 结构
        dataObj["records"]?.let { recordsElement ->
            if (recordsElement !is JsonNull) {
                val recordsObj = recordsElement as? JsonObject
                    ?: return ValidationResult(false, "记录数据格式错误：不是对象")

                val mealKeys = listOf("breakfast", "lunch", "dinner", "snack")
                for ((dateKey, dayElement) in recordsObj) {
                    if (dayElement is JsonNull) continue
                    val dayObj = dayElement as? JsonObject
                        ?: return ValidationResult(false, "记录数据格式错误：$dateKey 不是对象")

                    for (mk in mealKeys) {
                        val mealValue = dayObj[mk]
                        if (mealValue != null && mealValue !is JsonNull && mealValue !is JsonArray) {
                            return ValidationResult(
                                false,
                                "餐次数据格式错误：$dateKey $mk 不是数组"
                            )
                        }
                    }
                }
            }
        }

        // 校验 meta 结构
        dataObj["meta"]?.let { metaElement ->
            if (metaElement !is JsonNull && metaElement !is JsonObject) {
                return ValidationResult(false, "元信息格式错误：不是对象")
            }
        }

        return ValidationResult(true, "")
    }

    // ==================== S4.4: 预览 ====================

    /**
     * 预览备份数据：解析+校验+生成概览，不写入本地
     */
    fun previewData(jsonStr: String): PreviewResult {
        // 1. JSON 解析
        val parsed = try {
            Json.parseToJsonElement(jsonStr).jsonObject
        } catch (e: Exception) {
            return PreviewResult(false, "JSON 解析失败：${e.message}", null)
        }

        // 2. 结构校验
        val validation = validateBackup(parsed)
        if (!validation.valid) {
            return PreviewResult(false, validation.error, null)
        }

        // 3. 生成预览概览（as? 安全转换，容错 null 值字段）
        val dataObj = parsed["data"] as? JsonObject
            ?: return PreviewResult(false, "数据为空", null)
        val recordsObj = dataObj["records"] as? JsonObject ?: JsonObject(emptyMap())
        val targetsObj = dataObj["targets"] as? JsonObject

        var dayCount = 0
        var recordCount = 0
        var earliestDate = ""
        var latestDate = ""
        val dates = mutableListOf<String>()

        for ((dateKey, dayElement) in recordsObj) {
            if (dayElement is JsonNull) continue
            val dayObj = dayElement as? JsonObject ?: continue
            dayCount++
            dates.add(dateKey)

            for (mk in listOf("breakfast", "lunch", "dinner", "snack")) {
                val mealArr = dayObj[mk] as? JsonArray
                if (mealArr != null) {
                    recordCount += mealArr.size
                }
            }
        }

        if (dates.isNotEmpty()) {
            dates.sort()
            earliestDate = dates.first()
            latestDate = dates.last()
        }

        val foodTemplatesArr = dataObj["foodTemplates"] as? JsonArray
        val bodyRecordsArr = dataObj["bodyRecords"] as? JsonArray

        val preview = BackupPreview(
            dayCount = dayCount,
            recordCount = recordCount,
            earliestDate = earliestDate,
            latestDate = latestDate,
            hasTargets = targetsObj != null,
            targetCalories = targetsObj?.get("calories")?.jsonPrimitive?.doubleOrNull,
            targetProtein = targetsObj?.get("protein")?.jsonPrimitive?.doubleOrNull,
            exportedAt = parsed["exportedAt"]?.jsonPrimitive?.contentOrNull ?: "",
            schemaVersion = parsed["schemaVersion"]?.jsonPrimitive?.intOrNull ?: 1,
            templateCount = foodTemplatesArr?.size ?: 0,
            bodyRecordCount = bodyRecordsArr?.size ?: 0
        )

        return PreviewResult(true, "", preview)
    }

    // ==================== S4.5 + S4.7: 导入（含容错） ====================

    /**
     * 导入 JSON 文本，校验后覆盖写入本地
     * 增强容错：部分字段缺失时使用默认值填充
     */
    suspend fun importData(jsonStr: String): ImportResult {
        // 1. JSON 解析
        val parsed = try {
            Json.parseToJsonElement(jsonStr).jsonObject
        } catch (e: Exception) {
            return ImportResult(false, "JSON 解析失败：${e.message}", "")
        }

        // 2. 结构校验
        val validation = validateBackup(parsed)
        if (!validation.valid) {
            return ImportResult(false, validation.error, "")
        }

        // 3. 提取数据，缺失字段用默认值/现有值填充
        val dataObj = parsed["data"]?.jsonObject
            ?: return ImportResult(false, "数据为空", "")

        // targets 容错（S4.7）
        val targets = parseTargets(dataObj["targets"])

        // records 容错（S4.7）
        val records = parseRecords(dataObj["records"])

        // meta 容错
        val meta = parseMeta(dataObj["meta"])

        // 食物模板 + 身体记录（新 schema，旧备份不存在这些字段时不覆盖现有数据）
        val foodTemplates = if (dataObj.containsKey("foodTemplates")) {
            parseFoodTemplates(dataObj["foodTemplates"])
        } else null
        val bodyRecords = if (dataObj.containsKey("bodyRecords")) {
            parseBodyRecords(dataObj["bodyRecords"])
        } else null

        // 4. 写入 Storage
        val writeResult = repository.bulkSet(
            targets = targets,
            records = records,
            meta = meta,
            foodTemplates = foodTemplates,
            bodyRecords = bodyRecords
        )
        if (writeResult is Resource.Error) {
            return ImportResult(false, writeResult.message, "")
        }

        // 5. 生成摘要
        var totalCount = 0
        for ((_, day) in records) {
            totalCount += day.breakfast.size + day.lunch.size + day.dinner.size + day.snack.size
        }

        val summary = buildString {
            append("导入成功：共 ${records.size} 天记录，$totalCount 条数据")
            if (targets != null && targets.calories > 0) {
                append("，热量目标 ${targets.calories.toInt()} kcal")
            }
        }

        return ImportResult(true, "", summary)
    }

    // ==================== 内部解析工具（容错） ====================

    /**
     * 解析 targets，缺失字段补零/补默认值
     * S4.7: 缺失 protein/fat/carbs 补 0，缺失 micronutrients 补空数组
     */
    private fun parseTargets(element: JsonElement?): NutritionTargets {
        if (element == null || element is JsonNull) {
            return NutrientConstants.getDefaultTargets()
        }
        val obj = element as? JsonObject ?: return NutrientConstants.getDefaultTargets()

        val calories = safeDouble(obj["calories"])
        val protein = safeDouble(obj["protein"])
        val fat = safeDouble(obj["fat"])
        val carbs = safeDouble(obj["carbs"])

        val micronutrients = parseMicronutrientTargets(obj["micronutrients"])
        val updatedAt = obj["updatedAt"]?.jsonPrimitive?.contentOrNull ?: Instant.now().toString()
        val bodyProfile = parseBodyProfile(obj["bodyProfile"])
        val isAutoCalculated = obj["isAutoCalculated"]?.jsonPrimitive?.booleanOrNull ?: false

        return NutritionTargets(
            calories = calories,
            protein = protein,
            fat = fat,
            carbs = carbs,
            micronutrients = micronutrients,
            updatedAt = updatedAt,
            bodyProfile = bodyProfile,
            isAutoCalculated = isAutoCalculated
        )
    }

    /**
     * 解析身体档案（二期新增），缺失返回 null
     */
    private fun parseBodyProfile(element: JsonElement?): BodyProfile? {
        if (element == null || element is JsonNull) return null
        val obj = element as? JsonObject ?: return null

        val genderKey = obj["gender"]?.jsonPrimitive?.contentOrNull ?: return null
        val gender = Gender.fromKey(genderKey) ?: return null
        val age = obj["age"]?.jsonPrimitive?.intOrNull ?: return null
        val heightCm = obj["heightCm"]?.jsonPrimitive?.intOrNull ?: return null
        val weightKg = obj["weightKg"]?.jsonPrimitive?.doubleOrNull ?: return null
        val activityKey = obj["activityLevel"]?.jsonPrimitive?.contentOrNull ?: return null
        val activityLevel = ActivityLevel.fromKey(activityKey) ?: return null

        return BodyProfile(
            gender = gender,
            age = age,
            heightCm = heightCm,
            weightKg = weightKg,
            activityLevel = activityLevel
        )
    }

    /**
     * 解析微量营养素目标列表，缺失时补默认列表
     */
    private fun parseMicronutrientTargets(element: JsonElement?): List<MicronutrientTarget> {
        if (element == null || element !is JsonArray) {
            return NutrientConstants.getDefaultTargets().micronutrients
        }
        return element.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val key = obj["key"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: key
            val unit = obj["unit"]?.jsonPrimitive?.contentOrNull ?: ""
            val target = safeDouble(obj["target"])
            MicronutrientTarget(key, name, unit, target)
        }
    }

    /**
     * 解析 records，每天的餐次数组缺失时补空列表
     */
    private fun parseRecords(element: JsonElement?): Map<String, DayRecords> {
        if (element == null || element !is JsonObject) return emptyMap()

        val result = mutableMapOf<String, DayRecords>()
        for ((dateKey, dayElement) in element) {
            if (dayElement is JsonNull) {
                result[dateKey] = DayRecords(dateStr = dateKey)
                continue
            }
            val dayObj = dayElement as? JsonObject
            if (dayObj == null) {
                result[dateKey] = DayRecords(dateStr = dateKey)
                continue
            }
            result[dateKey] = parseDayRecords(dateKey, dayObj)
        }
        return result
    }

    /**
     * 解析单天记录，缺失的餐次补空列表
     */
    private fun parseDayRecords(dateStr: String, obj: JsonObject): DayRecords {
        val breakfast = parseMealRecords(obj["breakfast"])
        val lunch = parseMealRecords(obj["lunch"])
        val dinner = parseMealRecords(obj["dinner"])
        val snack = parseMealRecords(obj["snack"])

        return DayRecords(
            dateStr = dateStr,
            breakfast = breakfast,
            lunch = lunch,
            dinner = dinner,
            snack = snack
        )
    }

    /**
     * 解析单餐记录列表
     */
    private fun parseMealRecords(element: JsonElement?): List<MealRecord> {
        if (element == null || element !is JsonArray) return emptyList()
        return element.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            parseMealRecord(obj)
        }
    }

    /**
     * 解析单条记录，缺失字段补默认值
     * S4.7: 缺失 protein/fat/carbs 补 0，缺失 micronutrients 补空数组
     */
    private fun parseMealRecord(obj: JsonObject): MealRecord {
        val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: UUID.randomUUID().toString()
        val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: ""
        val calories = safeDouble(obj["calories"])
        val protein = safeDouble(obj["protein"])
        val fat = safeDouble(obj["fat"])
        val carbs = safeDouble(obj["carbs"])
        val weightGrams = obj["weightGrams"]?.jsonPrimitive?.doubleOrNull ?: 100.0

        val micronutrients = when (val mn = obj["micronutrients"]) {
            is JsonArray -> mn.mapNotNull { microElement ->
                val microObj = microElement as? JsonObject ?: return@mapNotNull null
                val key = microObj["key"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val value = safeDouble(microObj["value"])
                MealMicro(key, value)
            }
            else -> emptyList()
        }

        val createdAt = obj["createdAt"]?.jsonPrimitive?.contentOrNull ?: Instant.now().toString()
        val updatedAt = obj["updatedAt"]?.jsonPrimitive?.contentOrNull

        return MealRecord(
            id = id,
            name = name,
            calories = calories,
            protein = protein,
            fat = fat,
            carbs = carbs,
            micronutrients = micronutrients,
            weightGrams = weightGrams,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    /**
     * 解析 meta，缺失字段补默认值
     */
    private fun parseMeta(element: JsonElement?): AppMeta {
        if (element == null || element is JsonNull) {
            return NutrientConstants.getDefaultMeta()
        }
        val obj = element as? JsonObject ?: return NutrientConstants.getDefaultMeta()

        val version = obj["version"]?.jsonPrimitive?.contentOrNull ?: NutrientConstants.APP_VERSION
        val firstUseDate = obj["firstUseDate"]?.jsonPrimitive?.contentOrNull
            ?: NutrientConstants.getDefaultMeta().firstUseDate
        val lastExportDate = obj["lastExportDate"]?.jsonPrimitive?.contentOrNull
        val schemaVersion = obj["schemaVersion"]?.jsonPrimitive?.intOrNull ?: NutrientConstants.SCHEMA_VERSION
        val hasSeenGuide = obj["hasSeenGuide"]?.jsonPrimitive?.booleanOrNull ?: false

        return AppMeta(
            version = version,
            firstUseDate = firstUseDate,
            lastExportDate = lastExportDate,
            schemaVersion = schemaVersion,
            hasSeenGuide = hasSeenGuide
        )
    }

    /**
     * 解析食物模板列表
     */
    private fun parseFoodTemplates(element: JsonElement?): List<FoodTemplate> {
        if (element == null || element !is JsonArray) return emptyList()
        return element.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: UUID.randomUUID().toString()
            val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val calories = safeDouble(obj["calories"])
            val protein = safeDouble(obj["protein"])
            val fat = safeDouble(obj["fat"])
            val carbs = safeDouble(obj["carbs"])
            val micronutrients = when (val mn = obj["micronutrients"]) {
                is JsonArray -> mn.mapNotNull { microElement ->
                    val microObj = microElement as? JsonObject ?: return@mapNotNull null
                    val key = microObj["key"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                    val value = safeDouble(microObj["value"])
                    MealMicro(key, value)
                }
                else -> emptyList()
            }
            val tags = when (val t = obj["tags"]) {
                is JsonArray -> t.mapNotNull { it.jsonPrimitive.contentOrNull }
                else -> emptyList()
            }
            val source = obj["source"]?.jsonPrimitive?.contentOrNull ?: ""
            val createdAt = obj["createdAt"]?.jsonPrimitive?.contentOrNull ?: Instant.now().toString()

            FoodTemplate(
                id = id,
                name = name,
                calories = calories,
                protein = protein,
                fat = fat,
                carbs = carbs,
                micronutrients = micronutrients,
                tags = tags,
                isPreset = false,
                source = source,
                createdAt = createdAt
            )
        }
    }

    /**
     * 解析身体记录列表
     */
    private fun parseBodyRecords(element: JsonElement?): List<BodyRecord> {
        if (element == null || element !is JsonArray) return emptyList()
        return element.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val dateStr = obj["dateStr"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val weightKg = safeDouble(obj["weightKg"])
            val bodyFatPercent = obj["bodyFatPercent"]?.jsonPrimitive?.doubleOrNull
            val muscleKg = obj["muscleKg"]?.jsonPrimitive?.doubleOrNull
            val note = obj["note"]?.jsonPrimitive?.contentOrNull
            val createdAt = obj["createdAt"]?.jsonPrimitive?.contentOrNull ?: Instant.now().toString()

            BodyRecord(
                dateStr = dateStr,
                weightKg = weightKg,
                bodyFatPercent = bodyFatPercent,
                muscleKg = muscleKg,
                note = note,
                createdAt = createdAt
            )
        }
    }

    // ==================== 通用工具 ====================

    private fun safeDouble(element: JsonElement?): Double {
        if (element == null || element is JsonNull) return 0.0
        val primitive = (element as? JsonPrimitive) ?: return 0.0
        return primitive.doubleOrNull
            ?: primitive.intOrNull?.toDouble()
            ?: primitive.longOrNull?.toDouble()
            ?: primitive.floatOrNull?.toDouble()
            ?: primitive.contentOrNull?.toDoubleOrNull()
            ?: 0.0
    }
}
