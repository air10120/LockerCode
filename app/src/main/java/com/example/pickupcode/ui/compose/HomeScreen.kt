package com.example.pickupcode.ui.compose

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as ComposeSize
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.pickupcode.CodeMatcher
import com.example.pickupcode.PickupCodeExtractor
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlin.math.min

private const val DEFAULT_ANALYZE_INTERVAL_MS = 300L // 手动模式默认跳帧间隔
private const val AUTO_INTERVAL_MARGIN_MS = 120L      // 自动模式：间隔 = 实测 OCR 耗时 + 余量
private const val AUTO_INTERVAL_MIN_MS = 150L         // 自动模式间隔下限
private const val AUTO_INTERVAL_MAX_MS = 800L         // 自动模式间隔上限
private const val CROP_RATIO = 0.05f           // 中央 90% ROI 裁剪（上下左右各去 5%）：扩大识别区域，多个快递分布开也能命中
private const val HIT_HOLD_MS = 800L           // 命中后短暂保持标注

/** 默认命中框颜色（红） */
private const val DEFAULT_HIT_COLOR = 0xFFE53935.toInt()

/** 识别框颜色表：key 与 SettingsScreen 中 BOX_COLORS 一致 */
private val BOX_COLORS: Map<String, Int> = mapOf(
    "red" to 0xFFE53935.toInt(),
    "green" to 0xFF43A047.toInt(),
    "blue" to 0xFF1E88E5.toInt(),
    "orange" to 0xFFFB8C00.toInt(),
    "purple" to 0xFF8E24AA.toInt(),
    "white" to Color.WHITE
)

/** 标签文字颜色表：key 与 SettingsScreen 中 LABEL_TEXT_COLORS 一致 */
private val LABEL_TEXT_COLORS: Map<String, Int> = mapOf(
    "white" to Color.WHITE,
    "black" to Color.BLACK,
    "red" to 0xFFE53935.toInt(),
    "green" to 0xFF43A047.toInt(),
    "blue" to 0xFF1E88E5.toInt(),
    "orange" to 0xFFFB8C00.toInt(),
    "purple" to 0xFF8E24AA.toInt()
)
private const val DEBOUNCE_FRAMES = 2          // 连续命中帧数达到该值才判定命中（去抖防误报）
private const val DECODE_MAX_DIM = 1920        // 相册/拍照图片解码目标长边像素（防 OOM）

/**
 * 首页 = 实时扫描主界面（M3 Expressive，多取件码版）
 *
 * 结构：顶部标题 + 相册/拍照入口 → 380×507dp 相机实时预览（圆角 20dp，
 * AndroidView 包装 CameraX PreviewView，未授权时 inverseSurface 深色面板 +
 * 相机图标）→ 取件码输入行（输入单个码 + 加号加入待取件列表）→ 待取件列表
 * （已取件 ✓ / 未取件状态、可删除）→ 字符容错开关 → 确定按钮。
 *
 * 功能：进入自动申请相机权限并启动实时预览；逐个添加取件码到待取件列表后点
 * 「确定」启动实时匹配（ML Kit 中文 OCR + PickupCodeExtractor.extractAll +
 * CodeMatcher 容错匹配，640×480 + 250ms 跳帧 + 中央 70% ROI）；命中后在预览
 * 上方 Canvas 叠加粗红框 +「已找到」标签（多个码全部标注）并持续追踪，支持
 * 锁定/解锁；识别到的码自动在列表标记「已取件」，其余显示「未取件」；
 * 相册/拍照走四角度 OCR（空结果自动放大重试）后跳结果页（逐码显示取件状态）。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    settings: AppSettings,
    onOpenSettings: () -> Unit,
    onOpenResult: (Screen.Result) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    // ===== 状态 =====
    var targetInput by remember { mutableStateOf("") }   // 输入框（单个取件码）
    var targetCodes by remember { mutableStateOf<List<String>>(emptyList()) } // 待取件列表
    var pickedUpCodes by remember { mutableStateOf<Set<String>>(emptySet()) } // 已识别到的码
    var fuzzy by remember { mutableStateOf(false) }
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var matching by remember { mutableStateOf(false) }   // 点「确定」后开始实时匹配
    var userLocked by remember { mutableStateOf(false) } // 锁定/解锁标注
    var hitBoxes by remember { mutableStateOf<List<Pair<RectF, String>>>(emptyList()) }
    var cameraError by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(false) }   // 相册/拍照识别中
    var scanError by remember { mutableStateOf<String?>(null) }
    val previewSize = remember { mutableStateOf(IntSize.Zero) }

    val textRecognizer = remember {
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    }
    val analyzerExecutor = remember { java.util.concurrent.Executors.newSingleThreadExecutor() }
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FIT_CENTER }
    }

    // ===== 权限 =====
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        if (!granted) scanError = "需要相机权限才能实时扫描，请在系统设置中开启"
    }
    LaunchedEffect(Unit) {
        if (!permissionGranted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // ===== 实时扫描器 =====
    val liveScanner = remember {
        LiveScanner(
            mainHandler = mainHandler,
            previewSize = { previewSize.value },
            getTargets = { targetCodes },
            getFuzzy = { fuzzy },
            getMatching = { matching },
            getUserLocked = { userLocked },
            getAnalyzeMode = { settings.analyzeMode },
            getManualIntervalMs = { settings.manualIntervalMs },
            textRecognizer = textRecognizer,
            onHits = { hits ->
                hitBoxes = hits
            },
            onKeep = { /* 短暂丢失：保留旧命中框继续追踪 */ },
            onClear = {
                hitBoxes = emptyList()
            },
            onPickedUp = { code ->
                // 识别到的取件码标记为「已取件」，列表状态即时更新
                pickedUpCodes = pickedUpCodes + code
            },
            onVibrate = {
                if (settings.vibrateEnabled) vibrateOnce(context)
            }
        )
    }

    // ===== 相机绑定（仅权限已授予时，分辨率随设置变更自动重建）=====
    DisposableEffect(lifecycleOwner, permissionGranted, settings.analyzeResolution) {
        if (!permissionGranted) {
            onDispose {}
        } else {
            val future = ProcessCameraProvider.getInstance(context)
            val listener = Runnable {
                try {
                    val provider = future.get()
                    val preview = Preview.Builder()
                        .setTargetResolution(Size(1920, 1080))
                        .build()
                        .also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                    // 分析流分辨率跟随设置：720p 更流畅省电，1080p 源图更清晰
                    val analysisSize = if (settings.analyzeResolution == "720p") {
                        Size(1280, 720)
                    } else {
                        Size(1920, 1080)
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setTargetResolution(analysisSize)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    analysis.setAnalyzer(analyzerExecutor) { imageProxy ->
                        liveScanner.analyze(imageProxy)
                    }
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                    )
                } catch (e: Exception) {
                    mainHandler.post { cameraError = true }
                }
            }
            future.addListener(listener, ContextCompat.getMainExecutor(context))
            onDispose {
                try { future.get().unbindAll() } catch (_: Exception) {}
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose { analyzerExecutor.shutdown() }
    }

    // ===== 相册 / 拍照入口 =====
    val capturedUri = remember { mutableStateOf<Uri?>(null) }
    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = capturedUri.value
        if (success && uri != null) {
            val targets = targetCodes
            if (targets.isEmpty()) {
                scanError = "请先添加取件码，再重新扫描照片"
                return@rememberLauncherForActivityResult
            }
            scanning = true
            scanError = null
            recognizeFromUri(
                context, uri, targets, fuzzy, textRecognizer, mainHandler,
                onResult = onOpenResult,
                onError = { msg -> scanError = msg; scanning = false }
            )
        }
    }
    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            // 持久化读取授权：防止 Compose 重组后 Uri 失效导致打不开图
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // 部分返回的 Uri 不支持持久化（如云盘下载的临时授权），忽略即可
            }
            val targets = targetCodes
            if (targets.isEmpty()) {
                scanError = "请先添加取件码，再重新选择图片"
                return@rememberLauncherForActivityResult
            }
            scanning = true
            scanError = null
            recognizeFromUri(
                context, uri, targets, fuzzy, textRecognizer, mainHandler,
                onResult = onOpenResult,
                onError = { msg -> scanError = msg; scanning = false }
            )
        }
    }
    // 回退方案：设备不支持 Photo Picker（PickVisualMedia）时退回系统文件选择器
    val pickContentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // 忽略不支持持久化的 Uri
            }
            val targets = targetCodes
            if (targets.isEmpty()) {
                scanError = "请先添加取件码，再重新选择图片"
                return@rememberLauncherForActivityResult
            }
            scanning = true
            scanError = null
            recognizeFromUri(
                context, uri, targets, fuzzy, textRecognizer, mainHandler,
                onResult = onOpenResult,
                onError = { msg -> scanError = msg; scanning = false }
            )
        }
    }

    fun openCamera() {
        // 注意：入口不做取件码校验，先保证按钮能打开相机；
        // 未输入取件码时在拍照回调里统一提示
        scanError = null
        val uri = createCaptureUri(context) ?: run {
            scanError = "无法创建拍照文件"; return
        }
        capturedUri.value = uri
        takePictureLauncher.launch(uri)
    }

    fun openGallery() {
        // 注意：入口不做取件码校验，先保证按钮能弹出系统相册/文件选择器；
        // 未输入取件码时在选择回调里统一提示
        scanError = null
        try {
            // 优先使用系统 Photo Picker（Android 13+ 免存储权限）
            pickImageLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } catch (_: ActivityNotFoundException) {
            // 部分设备/模拟器无 Photo Picker 服务：回退到系统文件选择器
            pickContentLauncher.launch("image/*")
        }
    }

    // ===== 界面 =====
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
            // 顶部：标题 + 相册/拍照入口
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "取件码识别",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PressableIconButton(onClick = { onOpenSettings() }) {
                        Icon(
                            Icons.Rounded.Settings,
                            contentDescription = "设置",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    PressableIconButton(onClick = { openGallery() }) {
                        Icon(
                            Icons.Rounded.PhotoLibrary,
                            contentDescription = "相册扫描",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    PressableIconButton(onClick = { openCamera() }) {
                        Icon(
                            Icons.Rounded.PhotoCamera,
                            contentDescription = "拍照扫描",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            // 相机实时预览：380×507dp，圆角 20dp
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(507.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.inverseSurface)
            ) {
                if (permissionGranted) {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier
                            .matchParentSize()
                            .onSizeChanged { previewSize.value = it }
                    )
                    // Compose Canvas 叠加标注（所有匹配框 + 标签，样式跟随设置）
                    Canvas(Modifier.matchParentSize()) {
                        drawOverlay(hitBoxes, settings)
                    }
                    // 预览区内顶部：目标码状态 + 锁定/解锁
                    Row(
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                        ) {
                            Text(
                                when {
                                    !matching -> "实时预览"
                                    hitBoxes.isNotEmpty() -> "已找到 ${hitBoxes.size} 个"
                                    else -> "目标：${targetCodes.joinToString("、")} · 未找到"
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        PressableIconButton(onClick = { userLocked = !userLocked }) {
                            Icon(
                                if (userLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                                contentDescription = if (userLocked) "解锁" else "锁定",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else {
                    // 未授权 / 相机失败：inverseSurface 深色面板 + 相机图标
                    Column(
                        Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Rounded.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.inverseOnSurface,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (cameraError) "相机启动失败，请检查相机权限"
                            else "需要相机权限才能实时扫描",
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (!cameraError) {
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text("授予相机权限")
                            }
                        }
                    }
                }
                // 相册/拍照识别中遮罩
                if (scanning) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.inversePrimary)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            // 取件码输入行：输入单个取件码 + 加号按钮加入待取件列表
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = targetInput,
                    onValueChange = { targetInput = it },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    singleLine = true,
                    label = { Text("输入取件码") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                Spacer(Modifier.width(8.dp))
                PressableIconButton(onClick = {
                    val code = targetInput.trim()
                    when {
                        code.isEmpty() -> scanError = "请输入取件码后再添加"
                        !isValidPickupCode(code) -> scanError =
                            "取件码格式不合法：纯数字 4-8 位、数字+字母 3-10 位，或带横杠如 7-2-3001"
                        targetCodes.contains(code) -> scanError = "该取件码已在列表中"
                        else -> {
                            targetCodes = targetCodes + code
                            targetInput = ""
                            scanError = null
                            matching = false
                            pickedUpCodes = emptySet()
                        }
                    }
                }) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "添加到取件列表",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(4.dp))

            // 待取件列表：每个码显示状态（已取件 ✓ / 未取件），可删除
            if (targetCodes.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (code in targetCodes) {
                        val pickedUp = code in pickedUpCodes
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (pickedUp) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Row(
                                Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (pickedUp) Icons.Rounded.CheckCircle
                                    else Icons.Rounded.RadioButtonUnchecked,
                                    contentDescription = if (pickedUp) "已取件" else "未取件",
                                    tint = if (pickedUp) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    code,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (pickedUp) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.width(2.dp))
                                PressableIconButton(onClick = {
                                    targetCodes = targetCodes - code
                                    pickedUpCodes = pickedUpCodes - code
                                    if (targetCodes.isEmpty()) {
                                        matching = false
                                        hitBoxes = emptyList()
                                    }
                                }) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "移除",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    if (pickedUpCodes.isNotEmpty())
                        "已识别到 ${pickedUpCodes.size} 个快递，其余显示未取件"
                    else "识别到的取件码会自动标记为已取件",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
            }
            Spacer(Modifier.height(4.dp))

            // 字符容错开关
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = fuzzy, onCheckedChange = { fuzzy = it })
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        "字符容错",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "容忍 OCR 常见误识别（0/O、1/I、5/S 等）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            // 确定按钮：56dp 胶囊 + add 图标，按压轻微缩放 + 涟漪
            val btnInteraction = remember { MutableInteractionSource() }
            val btnPressed by btnInteraction.collectIsPressedAsState()
            val btnScale by animateFloatAsState(
                targetValue = if (btnPressed) 0.97f else 1f,
                label = "confirmScale"
            )
            Button(
                onClick = {
                    when {
                        targetCodes.isEmpty() -> scanError = "请先添加取件码再确定"
                        else -> {
                            scanError = null
                            matching = true
                            userLocked = false
                            pickedUpCodes = emptySet()
                            hitBoxes = emptyList()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .scale(btnScale),
                shape = RoundedCornerShape(28.dp),
                interactionSource = btnInteraction,
                enabled = permissionGranted
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (matching) "重新匹配" else "确定",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(Modifier.height(8.dp))

            // 状态 / 错误提示
            val tip = when {
                scanError != null -> scanError.orEmpty()
                matching -> "实时匹配中，将取件码标签对准相机…"
                else -> "实时预览已开启，输入取件码后点击确定开始匹配"
            }
            Text(
                tip,
                style = MaterialTheme.typography.bodySmall,
                color = if (scanError != null) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** 带按压缩放反馈的图标按钮 */
@Composable
internal fun PressableIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        label = "iconPressScale"
    )
    IconButton(
        onClick = onClick,
        modifier = Modifier.scale(scale),
        interactionSource = interactionSource
    ) {
        content()
    }
}

/**
 * 在 Compose Canvas 上绘制所有命中框与「已找到」标签。
 * 每个匹配文本块都画识别框 + 标签；标签位置/大小/颜色、框颜色/线宽按设置渲染；
 * 标签间自动防重叠（后放置的标签与已有标签冲突时下移），避免互相遮挡。
 */
private fun DrawScope.drawOverlay(
    hits: List<Pair<RectF, String>>,
    settings: AppSettings
) {
    if (hits.isEmpty()) return

    // 识别框样式：线宽为连续值（dp），直接使用设置值
    val boxColor = ComposeColor(BOX_COLORS[settings.boxColor] ?: DEFAULT_HIT_COLOR)
    val strokeWidthPx = settings.boxWidth.dp.toPx()
    // 标签文字样式：字号为连续值（sp），直接使用设置值
    val labelTextColor = LABEL_TEXT_COLORS[settings.labelColor] ?: Color.WHITE
    val labelBgColor = BOX_COLORS[settings.boxColor] ?: DEFAULT_HIT_COLOR
    val textSizeSp = settings.labelSize

    // 按 top 排序绘制，保证防重叠计算稳定
    val sorted = hits.sortedBy { it.first.top }
    val placedLabels = ArrayList<RectF>() // 已放置的标签区域（防重叠）

    for ((hit, label) in sorted) {
        drawRect(
            color = boxColor,
            topLeft = Offset(hit.left, hit.top),
            size = ComposeSize(hit.width(), hit.height()),
            style = Stroke(width = strokeWidthPx)
        )
        // 每个匹配框都画标签：文字 + 底色背景
        val fullLabel = "已找到 ${label}"
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = labelTextColor
            textSize = textSizeSp.sp.toPx()
            isFakeBoldText = true
        }
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = labelBgColor
            style = Paint.Style.FILL
        }
        val pad = 8.dp.toPx()
        val labelWidth = textPaint.measureText(fullLabel)
        val labelHeight = textPaint.textSize + pad * 2
        val maxRight = this.size.width

        // 水平位置：与框左对齐，限定在预览宽度内
        val labelLeft = (hit.left).coerceIn(
            2.dp.toPx(),
            (maxRight - labelWidth - pad * 2).coerceAtLeast(2.dp.toPx())
        )
        val labelRight = labelLeft + labelWidth + pad * 2

        // 垂直位置：上方 / 框内 / 下方
        var labelTop = when (settings.labelPosition) {
            "inside" -> hit.top + (hit.height() - labelHeight) / 2f
            "below" -> hit.bottom + 4.dp.toPx()
            else -> hit.top - labelHeight - 4.dp.toPx()   // above：框上方
        }

        // 防重叠：与已放置的标签区域冲突则逐级下移
        var guard = 0
        while (
            placedLabels.any { rectsOverlap(RectF(labelLeft, labelTop, labelRight, labelTop + labelHeight), it) } &&
            guard < 30
        ) {
            labelTop += labelHeight + 2.dp.toPx()
            guard++
        }
        labelTop = labelTop.coerceAtLeast(2.dp.toPx())
        // 超出预览底部时回退到框上方
        if (labelTop + labelHeight > this.size.height - 2.dp.toPx()) {
            labelTop = (hit.top - labelHeight - 4.dp.toPx()).coerceAtLeast(2.dp.toPx())
        }
        placedLabels.add(RectF(labelLeft, labelTop, labelRight, labelTop + labelHeight))

        drawContext.canvas.nativeCanvas.drawRoundRect(
            labelLeft, labelTop, labelRight, labelTop + labelHeight,
            6.dp.toPx(), 6.dp.toPx(), bgPaint
        )
        drawContext.canvas.nativeCanvas.drawText(
            fullLabel, labelLeft + pad, labelTop + labelHeight - pad, textPaint
        )
    }
}

/** 两个矩形是否相交（标签防重叠用） */
private fun rectsOverlap(a: RectF, b: RectF): Boolean =
    a.left < b.right && a.right > b.left && a.top < b.bottom && a.bottom > b.top

/**
 * 实时扫描器：CameraX ImageAnalysis 逐帧 OCR + 多目标匹配（复用 LiveScanFragment 逻辑）。
 * 所有状态更新均通过 mainHandler 切回主线程。
 */
private class LiveScanner(
    private val mainHandler: Handler,
    private val previewSize: () -> IntSize,
    private val getTargets: () -> List<String>,
    private val getFuzzy: () -> Boolean,
    private val getMatching: () -> Boolean,
    private val getUserLocked: () -> Boolean,
    private val getAnalyzeMode: () -> String,
    private val getManualIntervalMs: () -> Long,
    private val textRecognizer: TextRecognizer,
    private val onHits: (List<Pair<RectF, String>>) -> Unit,
    private val onKeep: () -> Unit,
    private val onClear: () -> Unit,
    private val onPickedUp: (String) -> Unit,
    private val onVibrate: () -> Unit
) {
    private var lastAnalyzeMs = 0L
    private var lastHitTime = 0L
    private var hitOnce = false
    private var hitStreak = 0   // 连续命中帧计数（去抖：连续 DEBOUNCE_FRAMES 帧命中才判定）
    private var vibrated = false
    private var ocrCostMs = DEFAULT_ANALYZE_INTERVAL_MS.toFloat() // 实测 OCR 耗时平滑值（自动模式用）

    /** 当前生效的跳帧间隔：自动模式按实测 OCR 耗时动态调节，手动模式用固定值 */
    private fun currentIntervalMs(): Long {
        return if (getAnalyzeMode() == "auto") {
            (ocrCostMs + AUTO_INTERVAL_MARGIN_MS)
                .toLong()
                .coerceIn(AUTO_INTERVAL_MIN_MS, AUTO_INTERVAL_MAX_MS)
        } else {
            getManualIntervalMs()
        }
    }

    fun analyze(imageProxy: ImageProxy) {
        val matching = getMatching()
        val userLocked = getUserLocked()
        if (!matching || userLocked) {
            imageProxy.close()
            return
        }
        val now = System.currentTimeMillis()
        if (now - lastAnalyzeMs < currentIntervalMs()) {
            imageProxy.close()
            return
        }
        lastAnalyzeMs = now
        try {
            val rawBitmap = imageProxyToBitmap(imageProxy)
            val rotation = imageProxy.imageInfo.rotationDegrees
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            val fullBitmap = Bitmap.createBitmap(
                rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true
            )
            rawBitmap.recycle()
            val cropLeft = (fullBitmap.width * CROP_RATIO).toInt()
            val cropTop = (fullBitmap.height * CROP_RATIO).toInt()
            val roiBitmap = Bitmap.createBitmap(
                fullBitmap, cropLeft, cropTop,
                fullBitmap.width - cropLeft * 2, fullBitmap.height - cropTop * 2
            )
            fullBitmap.recycle()
            val ocrStartMs = System.currentTimeMillis()
            textRecognizer.process(InputImage.fromBitmap(roiBitmap, 0))
                .addOnSuccessListener { visionText ->
                    onFrameRecognized(visionText, imageProxy, cropLeft, cropTop)
                }
                .addOnCompleteListener {
                    // 记录本帧 OCR 耗时（含排队），平滑更新后用于自动间隔调节
                    val costMs = (System.currentTimeMillis() - ocrStartMs).toFloat()
                    ocrCostMs = (ocrCostMs * 3f + costMs) / 4f
                    // OCR 完成（成功或失败）后回收 ROI 位图，避免逐帧累积内存
                    roiBitmap.recycle()
                    imageProxy.close()
                }
        } catch (e: Exception) {
            imageProxy.close()
        }
    }

    private fun onFrameRecognized(visionText: Text, imageProxy: ImageProxy, cropLeft: Int, cropTop: Int) {
        val targets = getTargets()
        val fuzzy = getFuzzy()
        if (targets.isEmpty()) {
            // 无目标：清空标注
            hitStreak = 0
            hitOnce = false
            vibrated = false
            mainHandler.post { onClear() }
            return
        }
        // 收集所有与任一目标匹配的文本块：每个文本块生成一个标注框（框 + 标签），
        // 同一取件码出现在多个面单/位置时全部保留，标签为该块内命中的全部码
        val frameHits = ArrayList<Pair<RectF, String>>()
        val matchedCodes = LinkedHashSet<String>()
        for (block in visionText.textBlocks) {
            val codes = PickupCodeExtractor.extractAll(block.text)
            if (codes.isEmpty()) continue
            val box = block.boundingBox ?: continue
            val matched = codes.filter { code -> targets.any { CodeMatcher.matches(it, code, fuzzy) } }
            if (matched.isEmpty()) continue
            matchedCodes.addAll(matched)
            frameHits.add(mapToPreview(box, imageProxy, cropLeft, cropTop) to matched.joinToString("、"))
        }

        val now = System.currentTimeMillis()
        if (frameHits.isNotEmpty()) {
            // 连续命中帧计数：达到 DEBOUNCE_FRAMES 才判定命中（去抖防误报）
            hitStreak++
            if (hitStreak >= DEBOUNCE_FRAMES) {
                hitOnce = true
                lastHitTime = now
                if (!vibrated) {
                    vibrated = true
                    onVibrate()
                }
                mainHandler.post {
                    onHits(frameHits)
                    // 命中的每个取件码上报，用于列表标记「已取件」
                    matchedCodes.forEach { onPickedUp(it) }
                }
            }
        } else {
            hitStreak = 0
            if (hitOnce && now - lastHitTime < HIT_HOLD_MS) {
                // 短暂丢失：保留旧命中框（持续追踪不冻结）
                mainHandler.post { onKeep() }
            } else {
                hitOnce = false
                vibrated = false
                mainHandler.post { onClear() }
            }
        }
    }

    /** 图像坐标系 → 预览显示坐标系（fitCenter 等比缩放 + 黑边偏移） */
    private fun mapToPreview(
        box: android.graphics.Rect,
        imageProxy: ImageProxy,
        cropLeft: Int,
        cropTop: Int
    ): RectF {
        val rotation = imageProxy.imageInfo.rotationDegrees
        val rotatedWidth = if (rotation == 90 || rotation == 270) imageProxy.height.toFloat()
            else imageProxy.width.toFloat()
        val rotatedHeight = if (rotation == 90 || rotation == 270) imageProxy.width.toFloat()
            else imageProxy.height.toFloat()

        val x0 = box.left + cropLeft
        val y0 = box.top + cropTop
        val x1 = box.right + cropLeft
        val y1 = box.bottom + cropTop

        val viewSize = previewSize()
        val viewWidth = viewSize.width.toFloat()
        val viewHeight = viewSize.height.toFloat()
        if (viewWidth <= 0f || viewHeight <= 0f) {
            return RectF(x0.toFloat(), y0.toFloat(), x1.toFloat(), y1.toFloat())
        }
        val scale = min(viewWidth / rotatedWidth, viewHeight / rotatedHeight)
        val offsetX = (viewWidth - rotatedWidth * scale) / 2f
        val offsetY = (viewHeight - rotatedHeight * scale) / 2f
        return RectF(
            x0 * scale + offsetX,
            y0 * scale + offsetY,
            x1 * scale + offsetX,
            y1 * scale + offsetY
        )
    }

    /** ImageProxy（RGBA_8888 输出）→ ARGB_8888 Bitmap，兼容 rowStride 行填充 */
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val plane = imageProxy.planes[0]
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * imageProxy.width
        return if (rowPadding == 0) {
            val bitmap = Bitmap.createBitmap(
                imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            bitmap
        } else {
            // 存在行填充：先按含填充宽度创建，拷贝后再裁掉右侧填充
            val paddedBitmap = Bitmap.createBitmap(
                imageProxy.width + rowPadding / pixelStride,
                imageProxy.height,
                Bitmap.Config.ARGB_8888
            )
            paddedBitmap.copyPixelsFromBuffer(buffer)
            Bitmap.createBitmap(
                paddedBitmap, 0, 0, imageProxy.width, imageProxy.height
            )
        }
    }
}

/** 一次性震动反馈 */
private fun vibrateOnce(context: Context) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(300)
    }
}

/** 创建拍照输出 Uri（复用 FileProvider） */
private fun createCaptureUri(context: Context): Uri? {
    return try {
        val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
            ?: context.filesDir
        val file = java.io.File(dir, "pickup_${System.currentTimeMillis()}.jpg")
        androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    } catch (e: Exception) {
        null
    }
}

/**
 * 相册/拍照图片识别：0/90/180/270 四角度 OCR + 多目标容错匹配，
 * 逆矩阵映射回原图坐标（复用 ImageRecognizeFragment 逻辑），完成后回调结果页。
 * 坐标基于采样解码图（长边≤1920），结果页用同一采样逻辑解码，保证框不位移。
 * 若首轮未识别到任何取件码（常见于文字过小/模糊），自动放大 2 倍重试一轮。
 */
private fun recognizeFromUri(
    context: Context,
    uri: Uri,
    targets: List<String>,
    fuzzy: Boolean,
    textRecognizer: TextRecognizer,
    mainHandler: Handler,
    onResult: (Screen.Result) -> Unit,
    onError: (String) -> Unit
) {
    Thread {
        try {
            // 采样解码（长边 ≤ DECODE_MAX_DIM），防止相册大图 OOM 崩溃
            var original = decodeSampledBitmapFromUri(context, uri)
                ?: throw Exception("图片读取失败")
            var codeLocations = ocrBitmapFourAngles(original, textRecognizer)
            if (codeLocations.isEmpty() && original.width * 2 <= 4096 && original.height * 2 <= 4096) {
                // 未识别到任何取件码：放大 2 倍重试，小字/模糊图识别率显著提升
                val scaled = Bitmap.createScaledBitmap(
                    original, original.width * 2, original.height * 2, true
                )
                original.recycle()
                original = scaled
                codeLocations = ocrBitmapFourAngles(original, textRecognizer)
            }
            original.recycle()
            val allCodes = ArrayList(codeLocations.keys)
            val allRects = ArrayList<android.graphics.Rect>()
            for (rectF in codeLocations.values) {
                allRects.add(
                    android.graphics.Rect(
                        rectF.left.toInt(), rectF.top.toInt(),
                        rectF.right.toInt(), rectF.bottom.toInt()
                    )
                )
            }
            // 多目标匹配：命中任一目标的所有取件码都进入结果
            val matchedCodes = allCodes.filter { code ->
                targets.any { CodeMatcher.matches(it, code, fuzzy) }
            }
            mainHandler.post {
                onResult(
                    Screen.Result(
                        targetCodes = targets,
                        allCodes = allCodes,
                        matchedCodes = matchedCodes,
                        imageUri = uri.toString(),
                        allRects = allRects
                    )
                )
            }
        } catch (e: Throwable) {
            val msg = when (e) {
                is java.io.FileNotFoundException -> "无法读取图片文件，请重新选择"
                is SecurityException -> "没有权限读取该图片，请重新选择"
                is OutOfMemoryError -> "图片过大，内存不足，请换一张较小的图片"
                else -> "识别失败：${e.message ?: "未知错误"}"
            }
            mainHandler.post { onError(msg) }
        }
    }.start()
}

/** 对单张 Bitmap 做 0/90/180/270 四角度 OCR，返回 取件码→原图坐标 映射（旋转后逆矩阵映射回原图） */
private fun ocrBitmapFourAngles(
    bitmap: Bitmap,
    textRecognizer: TextRecognizer
): LinkedHashMap<String, RectF> {
    val codeLocations = LinkedHashMap<String, RectF>()
    for (angle in intArrayOf(0, 90, 180, 270)) {
        val image: InputImage
        val inverse: Matrix?
        var rotated: Bitmap? = null
        if (angle == 0) {
            image = InputImage.fromBitmap(bitmap, 0)
            inverse = null
        } else {
            val matrix = Matrix().apply { postRotate(angle.toFloat()) }
            rotated = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )
            inverse = Matrix().apply { matrix.invert(this) }
            image = InputImage.fromBitmap(rotated, 0)
        }
        val visionText = Tasks.await(textRecognizer.process(image))
        for (block in visionText.textBlocks) {
            val codes = PickupCodeExtractor.extractAll(block.text)
            if (codes.isEmpty()) continue
            val bbox = block.boundingBox ?: continue
            val rectF = RectF(bbox)
            inverse?.mapRect(rectF)
            for (code in codes) {
                if (!codeLocations.containsKey(code)) codeLocations[code] = rectF
            }
        }
        // 旋转临时位图用完即回收，避免峰值内存叠加
        rotated?.recycle()
    }
    return codeLocations
}

/** 取件码格式校验：纯数字 4-8 位；数字+字母 3-10 位（含字母）；带横杠如 7-2-3001 */
private fun isValidPickupCode(code: String): Boolean {
    if (code.isBlank()) return false
    if (code.contains('-')) {
        // 带横杠：各段非空且均为字母数字，如 7-2-3001
        val parts = code.split('-')
        return parts.size >= 2 && parts.all { it.isNotBlank() && it.all(Char::isLetterOrDigit) }
    }
    if (code.all(Char::isDigit)) return code.length in 4..8   // 纯数字 4-8 位
    if (code.all(Char::isLetterOrDigit) && code.any(Char::isLetter)) {
        return code.length in 3..10                            // 数字+字母 3-10 位
    }
    return false
}

/** 从相册/拍照 Uri 采样解码 Bitmap（长边不超过 DECODE_MAX_DIM），避免大图 OOM */
private fun decodeSampledBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        val resolver = context.contentResolver
        resolver.openFileDescriptor(uri, "r")?.use { pfd ->
            // 第一遍：仅读取尺寸
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFileDescriptor(pfd.fileDescriptor, null, bounds)
            // 计算 2 的幂采样率，保证解码后长边不超过 DECODE_MAX_DIM
            var sampleSize = 1
            val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
            while (maxSide / (sampleSize * 2) >= DECODE_MAX_DIM) sampleSize *= 2
            // 第二遍：按采样率解码
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeFileDescriptor(pfd.fileDescriptor, null, opts)
        }
    } catch (e: Exception) {
        null
    }
}
