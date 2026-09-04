package com.example.nutrition.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nutrition.ui.theme.Border
import com.example.nutrition.ui.theme.Primary
import com.example.nutrition.ui.theme.TextInverse
import com.example.nutrition.ui.theme.TextPrimary
import com.example.nutrition.ui.theme.TextSecondary
import com.example.nutrition.ui.theme.TextPlaceholder
import com.example.nutrition.ui.theme.BgCard

/**
 * 首次使用引导遮罩 —— 对应小程序 guide-overlay 组件
 *
 * 全屏半透明遮罩 + 居中卡片
 * 4 步引导：📊记录 → 🎯目标 → 📈统计 → 💾导出
 * 上一步/下一步/跳过按钮，进度指示点
 * 最后一步按钮变为"开始使用"
 *
 * @param visible 是否显示
 * @param onFinish 完成或跳过引导时的回调
 */
@Composable
fun GuideOverlay(
    visible: Boolean,
    onFinish: () -> Unit
) {
    if (!visible) return

    val steps = remember { GUIDE_STEPS }
    var currentStep by remember { mutableIntStateOf(0) }

    // 全屏遮罩
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(enabled = false) { /* 点击遮罩不关闭 */ },
        contentAlignment = Alignment.Center
    ) {
        // 引导卡片
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BgCard)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 跳过按钮（右上角）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = {
                    currentStep = 0
                    onFinish()
                }) {
                    Text(
                        text = "跳过",
                        fontSize = 12.sp,
                        color = TextPlaceholder
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 步骤内容（带切换动画）
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith
                        fadeOut(animationSpec = tween(200))
                },
                label = "guideStep"
            ) { step ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 图标
                    Text(
                        text = steps[step].icon,
                        fontSize = 48.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 标题
                    Text(
                        text = steps[step].title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 描述
                    Text(
                        text = steps[step].desc,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 进度指示点
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                steps.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .size(if (index == currentStep) 16.dp else 8.dp, 8.dp)
                            .clip(
                                if (index == currentStep) {
                                    RoundedCornerShape(4.dp)
                                } else {
                                    CircleShape
                                }
                            )
                            .background(
                                if (index == currentStep) Primary else Border
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 导航按钮区
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 上一步（第一步时隐藏）
                if (currentStep > 0) {
                    TextButton(onClick = { currentStep-- }) {
                        Text(
                            text = "上一步",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                // 下一步 / 开始使用
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Primary)
                        .clickable {
                            if (currentStep < steps.lastIndex) {
                                currentStep++
                            } else {
                                currentStep = 0
                                onFinish()
                            }
                        }
                        .padding(horizontal = 32.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = if (currentStep == steps.lastIndex) "开始使用" else "下一步",
                        color = TextInverse,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * 引导步骤定义
 */
private data class GuideStep(
    val icon: String,
    val title: String,
    val desc: String
)

private val GUIDE_STEPS = listOf(
    GuideStep(
        icon = "\uD83D\uDCCA", // 📊
        title = "记录每日饮食",
        desc = "在录入页添加食物和营养数据，随时查看当日摄入总览"
    ),
    GuideStep(
        icon = "\uD83C\uDFAF", // 🎯
        title = "设置营养目标",
        desc = "前往设置页配置每日热量和营养素目标，获取达标进度"
    ),
    GuideStep(
        icon = "\uD83D\uDCC8", // 📈
        title = "每周统计分析",
        desc = "查看周报图表，了解一周营养摄入趋势和达标率"
    ),
    GuideStep(
        icon = "\uD83D\uDCBE", // 💾
        title = "定期导出备份",
        desc = "数据仅保存在本地，卸载或清理缓存会丢失。请定期使用「导出数据」备份"
    )
)
