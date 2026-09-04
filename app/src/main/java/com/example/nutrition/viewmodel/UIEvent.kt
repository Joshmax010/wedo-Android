package com.example.nutrition.viewmodel

/**
 * 一次性 UI 事件 —— 不属于页面状态的动作（Toast 提示等）
 *
 * ViewModel 通过 Channel 发送，界面收集后执行并消费；
 * 替代原来用 mutableStateOf 承载 toastMessage 导致的重复触发/消费竞态问题
 */
sealed interface UIEvent {
    data class ShowToast(val message: String) : UIEvent
}
