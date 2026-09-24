package com.example.pickupcode.ui.compose.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable

/**
 * 全局 Motion Scheme：Material 3 Expressive 动效。
 * 页面过渡、命中框出现/缩放、按钮按压反馈等统一使用该 scheme 的动画规格。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val AppMotionScheme: MotionScheme = MotionScheme.expressive()

/** 应用主题：M3 Expressive Purple 浅色方案，仅浅色模式 */
@Composable
fun PickupCodeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = PickupCodeTypography,
        shapes = PickupCodeShapes,
        content = content
    )
}
