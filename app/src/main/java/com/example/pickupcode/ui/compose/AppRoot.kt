package com.example.pickupcode.ui.compose

import android.graphics.Rect
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/** 应用页面：Home（实时扫描主界面） / Settings（设置页） / Result（拍照·相册识别结果页） */
sealed interface Screen {
    data object Home : Screen

    data object Settings : Screen

    data class Result(
        val targetCodes: List<String>,
        val allCodes: List<String>,
        val matchedCodes: List<String>,
        val imageUri: String,
        val allRects: List<Rect>
    ) : Screen
}

/**
 * 应用根路由：状态切换驱动页面过渡。
 * 前进（Home → Result/Settings）：新页右滑淡入；返回：旧页右滑淡出，
 * 系统返回手势（BackHandler）同样触发该过渡动画。
 * 设置状态提升到根层，Home 与 Settings 共享，修改即时持久化。
 */
@Composable
fun AppRoot() {
    val context = LocalContext.current
    var settings by remember { mutableStateOf(loadSettings(context)) }
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }

    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            if (targetState is Screen.Result) {
                (fadeIn(tween(260)) + slideInHorizontally { it / 4 }) togetherWith
                    (fadeOut(tween(200)) + slideOutHorizontally { -it / 8 })
            } else {
                (fadeIn(tween(260)) + slideInHorizontally { -it / 8 }) togetherWith
                    (fadeOut(tween(200)) + slideOutHorizontally { it / 4 })
            }
        },
        label = "screenTransition"
    ) { target ->
        when (target) {
            is Screen.Home -> HomeScreen(
                settings = settings,
                onOpenSettings = { screen = Screen.Settings },
                onOpenResult = { screen = it }
            )

            is Screen.Settings -> {
                // 系统返回手势 / 返回键：回首页并播放返回过渡动画
                BackHandler { screen = Screen.Home }
                SettingsScreen(
                    settings = settings,
                    onChange = { new ->
                        settings = new
                        saveSettings(context, new)
                    },
                    onBack = { screen = Screen.Home }
                )
            }

            is Screen.Result -> {
                // 系统返回手势 / 返回键：回首页并播放返回过渡动画
                BackHandler { screen = Screen.Home }
                ResultScreen(
                    targetCodes = target.targetCodes,
                    allCodes = target.allCodes,
                    matchedCodes = target.matchedCodes,
                    imageUri = target.imageUri,
                    allRects = target.allRects,
                    onRescan = { screen = Screen.Home },
                    onBack = { screen = Screen.Home }
                )
            }
        }
    }
}
