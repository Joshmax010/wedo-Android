package com.example.nutrition.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nutrition.domain.constants.NutrientConstants
import com.example.nutrition.domain.model.MealMicro
import com.example.nutrition.domain.model.MealRecord
import com.example.nutrition.ui.theme.BgCard
import com.example.nutrition.ui.theme.BgMain
import com.example.nutrition.ui.theme.Error
import com.example.nutrition.ui.theme.Info
import com.example.nutrition.ui.theme.Primary
import com.example.nutrition.ui.theme.TextInverse
import com.example.nutrition.ui.theme.TextPrimary
import com.example.nutrition.ui.theme.TextSecondary
import com.example.nutrition.ui.theme.TextPlaceholder

/**
 * 记录条目组件 —— 对应小程序 record-item 组件
 *
 * 显示食物名称/热量/宏量标签/微量标签
 * 左滑删除（Material3 SwipeToDismissBox）
 * 微量营养素 key → 中文名解析（NutrientConstants）
 *
 * @param record 记录数据
 * @param onEdit 点击编辑回调
 * @param onDelete 删除回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordItem(
    record: MealRecord,
    modifier: Modifier = Modifier,
    isDismissed: Boolean = false,
    onEdit: (String) -> Unit = {},
    onDelete: (String) -> Unit = {}
) {
    val cardShape = RoundedCornerShape(16.dp)
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete(record.id)
                false // 不自动移除，等外部确认后再通过列表更新移除，或者取消后重置
            } else {
                false
            }
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.35f }
    )

    // 当外部状态显示未被删除（比如取消了确认）时，如果当前处于 dismissed 状态，则重置
    LaunchedEffect(isDismissed) {
        if (!isDismissed && dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            dismissState.reset()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            // 红色删除背景
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(cardShape)
                    .background(if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) Error else BgCard),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) Row(
                    modifier = Modifier.padding(end = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "删除",
                        tint = TextInverse
                    )
                    Text(
                        text = "删除",
                        color = TextInverse,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        enableDismissFromStartToEnd = false,
        modifier = modifier.clip(cardShape)
    ) {
        // 卡片内容
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEdit(record.id) },
            shape = cardShape,
            colors = CardDefaults.cardColors(containerColor = BgCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(modifier = Modifier.padding(12.dp)) {
                RecordItemContent(record = record)
            }
        }
    }
}

/**
 * 记录卡片内部内容
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecordItemContent(record: MealRecord) {
    androidx.compose.foundation.layout.Column {
        // 头部：名称 + 克重 + 热量
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = record.name.ifEmpty { "未命名" },
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            val weightText = if (record.weightGrams == 100.0) "" else "${formatNutrient(record.weightGrams)}g · "
            Text(
                text = "${weightText}${record.calories.toInt()} kcal",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
        }

        // 宏量营养素标签
        val macroTags = buildList {
            if (record.protein > 0) add("蛋白质 ${formatNutrient(record.protein)}g")
            if (record.fat > 0) add("脂肪 ${formatNutrient(record.fat)}g")
            if (record.carbs > 0) add("碳水 ${formatNutrient(record.carbs)}g")
        }

        if (macroTags.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                macroTags.forEach { tag ->
                    NutrientTag(text = tag, isMicro = false)
                }
            }
        }

        // 微量营养素标签（解析中文名称）
        val displayMicros = record.micronutrients.map { micro ->
            val name = NutrientConstants.getMicroName(micro.key)
            val unit = NutrientConstants.getMicroUnit(micro.key)
            Triple(micro.key, name, "${formatNutrient(micro.value)}$unit")
        }

        if (displayMicros.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                displayMicros.forEach { (_, name, valueStr) ->
                    NutrientTag(text = "$name $valueStr", isMicro = true)
                }
            }
        }
    }
}

/**
 * 营养素标签（宏量灰色底 / 微量蓝色底）
 */
@Composable
private fun NutrientTag(text: String, isMicro: Boolean) {
    val bgColor = if (isMicro) Info.copy(alpha = 0.08f) else BgMain
    val textColor = if (isMicro) Info else TextSecondary

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            color = textColor
        )
    }
}

private fun formatNutrient(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        String.format("%.1f", value)
    }
}
