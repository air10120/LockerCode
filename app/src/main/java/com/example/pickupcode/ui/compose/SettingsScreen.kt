package com.example.pickupcode.ui.compose

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.pickupcode.BuildConfig
import kotlin.math.roundToInt

/** 应用设置项：跳帧间隔（自动/手动）、分析分辨率、识别震动开关、标注样式 */
data class AppSettings(
    val analyzeMode: String = "auto",        // "auto" 自动自适应 / "manual" 手动固定
    val manualIntervalMs: Long = 300L,       // 手动跳帧间隔（ms）
    val analyzeResolution: String = "1080p", // "720p" / "1080p"
    val vibrateEnabled: Boolean = true,      // 识别到取件码是否震动
    // 标签文字样式
    val labelSize: Float = 22f,              // 标签文字字号（sp），滑块范围 10~36
    val labelPosition: String = "above",     // "above" 上方 / "inside" 框内 / "below" 下方
    val labelColor: String = "white",        // 标签文字颜色（预设 key，见 LABEL_TEXT_COLORS）
    // 识别框样式
    val boxColor: String = "red",            // 识别框颜色（预设 key，见 BOX_COLORS）
    val boxWidth: Float = 6f                 // 识别框线宽（dp），滑块范围 1~8
)

private const val PREFS_NAME = "app_settings"

/** 从 SharedPreferences 读取设置（缺失项用默认值兜底） */
fun loadSettings(context: Context): AppSettings {
    val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return AppSettings(
        analyzeMode = sp.getString("analyze_mode", "auto") ?: "auto",
        manualIntervalMs = sp.getLong("manual_interval_ms", 300L),
        analyzeResolution = sp.getString("analyze_resolution", "1080p") ?: "1080p",
        vibrateEnabled = sp.getBoolean("vibrate_enabled", true),
        labelSize = sp.getFloat("label_size", 22f),
        labelPosition = sp.getString("label_position", "above") ?: "above",
        labelColor = sp.getString("label_color", "white") ?: "white",
        boxColor = sp.getString("box_color", "red") ?: "red",
        boxWidth = sp.getFloat("box_width", 6f)
    )
}

/** 保存设置到 SharedPreferences */
fun saveSettings(context: Context, settings: AppSettings) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString("analyze_mode", settings.analyzeMode)
        .putLong("manual_interval_ms", settings.manualIntervalMs)
        .putString("analyze_resolution", settings.analyzeResolution)
        .putBoolean("vibrate_enabled", settings.vibrateEnabled)
        .putFloat("label_size", settings.labelSize)
        .putString("label_position", settings.labelPosition)
        .putString("label_color", settings.labelColor)
        .putString("box_color", settings.boxColor)
        .putFloat("box_width", settings.boxWidth)
        .apply()
}

private val MANUAL_INTERVALS = listOf(150L, 200L, 250L, 300L, 400L)

// ===== 标注样式选项 =====
/** 标签文字相对识别框的位置 */
private val LABEL_POSITIONS = listOf(
    "above" to "上方",
    "inside" to "框内",
    "below" to "下方"
)

/** 标签文字颜色预设（key → 显示名） */
private val LABEL_TEXT_COLORS = listOf(
    "white" to "白",
    "black" to "黑",
    "red" to "红",
    "green" to "绿",
    "blue" to "蓝",
    "orange" to "橙",
    "purple" to "紫"
)

/** 识别框颜色预设（key → 显示名） */
private val BOX_COLORS = listOf(
    "red" to "红",
    "green" to "绿",
    "blue" to "蓝",
    "orange" to "橙",
    "purple" to "紫",
    "white" to "白"
)

/** 单条更新日志：版本号 + 更新内容列表 */
private data class ChangelogEntry(val version: String, val items: List<String>)

/** 历史更新日志（按版本倒序） */
private val CHANGELOG = listOf(
    ChangelogEntry(
        "v1.0.11", listOf(
            "修复：识别框支持同时标注多个匹配目标",
            "新增：设置页个性化（标签文字大小滑块、标签位置、标签颜色、识别框颜色、识别框线宽滑块）"
        )
    ),
    ChangelogEntry(
        "v1.0.8", listOf(
            "修复多个取件码同时识别漏检：识别区域扩大至中央 90%",
            "同一文本块内的多个取件码全部收集，不再只取第一个",
            "命中标签只显示在最上方框，避免遮挡其他框"
        )
    ),
    ChangelogEntry(
        "v1.0.7", listOf(
            "新增设置页：跳帧间隔支持自动/手动调节",
            "分析分辨率 720p/1080p 可切换",
            "新增识别震动开关，设置自动保存"
        )
    ),
    ChangelogEntry(
        "v1.0.6", listOf(
            "相机分析流提升至 1080p，识别源图更清晰",
            "优化跳帧间隔，降低高分辨率下 OCR 积压"
        )
    ),
    ChangelogEntry(
        "v1.0.5", listOf(
            "相机像素优化：预览提升至 1080p",
            "分析流 720p 保障扫描流畅"
        )
    ),
    ChangelogEntry(
        "v1.0.4", listOf(
            "取件码列表化：加号逐个加入待取件列表，可删除",
            "识别到自动标记已取件，其余显示未取件",
            "相册识别空结果自动放大 2 倍重试"
        )
    ),
    ChangelogEntry(
        "v1.0.3", listOf(
            "支持多个取件码同时识别",
            "点击确定开始识别",
            "修复输入法遮挡输入框问题"
        )
    ),
    ChangelogEntry(
        "v1.0.2", listOf(
            "标注优化：只框选匹配到的取件码，画面更干净"
        )
    ),
    ChangelogEntry(
        "v1.0.1", listOf(
            "修复部分设备相册无法打开问题",
            "APK 命名规范为 app-版本号"
        )
    )
)

/**
 * 设置页：跳帧间隔（自动/手动 + 固定档位）、分析分辨率（720p/1080p）、
 * 识别震动开关、更新日志入口、作者信息。修改即时回传上层并持久化。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onChange: (AppSettings) -> Unit,
    onBack: () -> Unit
) {
    var showChangelog by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .imePadding()
            .padding(16.dp)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 顶部：返回 + 标题
            Row(verticalAlignment = Alignment.CenterVertically) {
                PressableIconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    "设置",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(20.dp))

            // ===== 跳帧间隔 =====
            Text(
                "跳帧间隔",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "控制实时识别的处理频率：自动模式会根据手机性能动态调节，手动模式固定为指定间隔",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = settings.analyzeMode == "auto",
                    onClick = { onChange(settings.copy(analyzeMode = "auto")) },
                    label = { Text("自动") }
                )
                FilterChip(
                    selected = settings.analyzeMode == "manual",
                    onClick = { onChange(settings.copy(analyzeMode = "manual")) },
                    label = { Text("手动") }
                )
            }
            if (settings.analyzeMode == "manual") {
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (ms in MANUAL_INTERVALS) {
                        FilterChip(
                            selected = settings.manualIntervalMs == ms,
                            onClick = { onChange(settings.copy(manualIntervalMs = ms)) },
                            label = { Text("${ms}ms") }
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            // ===== 分析分辨率 =====
            Text(
                "分析分辨率",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "720p 更流畅省电，1080p 画面更清晰（对识别精度提升有限）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = settings.analyzeResolution == "720p",
                    onClick = { onChange(settings.copy(analyzeResolution = "720p")) },
                    label = { Text("720p") }
                )
                FilterChip(
                    selected = settings.analyzeResolution == "1080p",
                    onClick = { onChange(settings.copy(analyzeResolution = "1080p")) },
                    label = { Text("1080p") }
                )
            }
            Spacer(Modifier.height(20.dp))

            // ===== 识别震动 =====
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "识别到取件码时震动",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "命中目标时震动提醒一次",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.vibrateEnabled,
                        onCheckedChange = { onChange(settings.copy(vibrateEnabled = it)) }
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            // ===== 标签文字样式 =====
            Text(
                "标签文字",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "自定义识别框上「已找到」标签的文字大小、位置与颜色",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "文字大小",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = settings.labelSize,
                    onValueChange = { onChange(settings.copy(labelSize = it)) },
                    valueRange = 10f..36f,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "${settings.labelSize.roundToInt()}sp",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "文字位置",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((key, name) in LABEL_POSITIONS) {
                    FilterChip(
                        selected = settings.labelPosition == key,
                        onClick = { onChange(settings.copy(labelPosition = key)) },
                        label = { Text(name) }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "文字颜色",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((key, name) in LABEL_TEXT_COLORS) {
                    FilterChip(
                        selected = settings.labelColor == key,
                        onClick = { onChange(settings.copy(labelColor = key)) },
                        label = { Text(name) }
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            // ===== 识别框样式 =====
            Text(
                "识别框",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "自定义匹配目标的识别框颜色与线宽",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "框颜色",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((key, name) in BOX_COLORS) {
                    FilterChip(
                        selected = settings.boxColor == key,
                        onClick = { onChange(settings.copy(boxColor = key)) },
                        label = { Text(name) }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "框线宽",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = settings.boxWidth,
                    onValueChange = { onChange(settings.copy(boxWidth = it)) },
                    valueRange = 1f..8f,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "${settings.boxWidth.roundToInt()}dp",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(20.dp))

            // ===== 更新日志入口 =====
            Surface(
                onClick = { showChangelog = true },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "更新日志",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "查看各版本更新内容",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            // ===== 作者信息 =====
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "关于",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "取件码识别 v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(8.dp))
                    // 作者信息行：点击复制到剪贴板
                    AuthorInfoRow(
                        label = "作者 QQ",
                        value = "1986457904",
                        onClick = {
                            clipboard.setText(AnnotatedString("1986457904"))
                            Toast.makeText(context, "QQ 号已复制", Toast.LENGTH_SHORT).show()
                        }
                    )
                    Spacer(Modifier.height(4.dp))
                    AuthorInfoRow(
                        label = "抖音号",
                        value = "ygqpdr",
                        onClick = {
                            clipboard.setText(AnnotatedString("ygqpdr"))
                            Toast.makeText(context, "抖音号已复制", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "设置即时生效，返回首页后自动应用",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // ===== 更新日志弹窗 =====
    if (showChangelog) {
        AlertDialog(
            onDismissRequest = { showChangelog = false },
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("更新日志", style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    CHANGELOG.forEachIndexed { index, entry ->
                        if (index > 0) {
                            Spacer(Modifier.height(12.dp))
                        }
                        Text(
                            entry.version,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        entry.items.forEach { item ->
                            Text(
                                "· $item",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showChangelog = false }) {
                    Text("关闭")
                }
            }
        )
    }
}

/** 作者信息行：左侧标签 + 右侧值，整行可点击复制 */
@Composable
private fun AuthorInfoRow(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
