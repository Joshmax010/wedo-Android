package com.example.nutrition.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nutrition.domain.constants.NutrientConstants
import com.example.nutrition.domain.model.MicronutrientTarget
import com.example.nutrition.ui.theme.Info
import com.example.nutrition.ui.theme.Primary
import com.example.nutrition.ui.theme.TextPrimary
import com.example.nutrition.ui.theme.TextSecondary

/**
 * 营养素选择器 —— 对应小程序 record.js 中的 nutrient picker 逻辑
 *
 * AlertDialog + LazyColumn，显示可选营养素列表
 * 选择后添加到已选列表（去重），关闭弹窗
 *
 * @param visible 是否显示
 * @param existingKeys 已添加的营养素 key 列表（用于过滤）
 * @param availableNutrients 可选营养素列表（从 NutrientConstants.micro 过滤）
 * @param onPick 选中后的回调，返回选中的营养素定义
 * @param onDismiss 关闭弹窗
 */
@Composable
fun NutrientPicker(
    visible: Boolean,
    existingKeys: List<String>,
    onPick: (NutrientConstantItem) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    // 过滤已添加的营养素
    val available = NutrientConstants.MICROS.filter { micro ->
        micro.key !in existingKeys
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "添加微量营养素",
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            if (available.isEmpty()) {
                Text(
                    text = "所有营养素已添加",
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyColumn {
                    items(available) { nutrient ->
                        NutrientPickerItem(
                            nutrient = nutrient,
                            onClick = {
                                onPick(
                                    NutrientConstantItem(
                                        key = nutrient.key,
                                        name = nutrient.name,
                                        unit = nutrient.unit,
                                        target = nutrient.target
                                    )
                                )
                            }
                        )
                        if (nutrient != available.last()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 44.dp),
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = TextSecondary)
            }
        }
    )
}

/**
 * 营养素选择器列表项
 */
@Composable
private fun NutrientPickerItem(
    nutrient: NutrientConstants.MicroDef,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = nutrient.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Text(
                text = "目标 ${nutrient.target}${nutrient.unit}",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
        Text(
            text = "+",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Primary
        )
    }
}

/**
 * 营养素常量项（选择器回调数据类）
 */
data class NutrientConstantItem(
    val key: String,
    val name: String,
    val unit: String,
    val target: Double
)
