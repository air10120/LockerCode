package com.example.pickupcode.ui.compose

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 拍照/相册识别结果页（Compose 版，多取件码）
 *
 * 与识别端使用同一套采样解码（长边≤1920），保证标注框坐标与原图一致，不位移。
 * 命中：每个匹配到的取件码都绘制粗红框 +「已找到」标签；未命中：不绘制任何框，
 * 仅以文字列出识别到的取件码 + 未找到提示 + 重新扫描按钮。
 */
@Composable
fun ResultScreen(
    targetCodes: List<String>,
    allCodes: List<String>,
    matchedCodes: List<String>,
    imageUri: String,
    allRects: List<Rect>,
    onRescan: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(imageUri) {
        previewBitmap = withContext(Dispatchers.IO) {
            drawPreview(context, imageUri, allCodes, matchedCodes, allRects)
        }
    }

    BackHandler { onBack() }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 顶栏
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Rounded.ArrowBack,
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                if (matchedCodes.isNotEmpty()) "已找到 ${matchedCodes.size} 个快递！" else "未找到匹配取件码",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(8.dp))

        // 结果图（带标注框）
        val bmp = previewBitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "扫描结果",
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        Spacer(Modifier.height(12.dp))

        // 状态文字
        // 目标取件码状态列表：识别到的标「已取件」，未识别到标「未取件」
        Text(
            "取件状态",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(6.dp))
        for (code in targetCodes) {
            val found = matchedCodes.any { it.equals(code, ignoreCase = true) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (found) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = if (found) "已取件" else "未取件",
                    tint = if (found) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    code,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (found) "已取件" else "未取件",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (found) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.height(2.dp))
        }
        Spacer(Modifier.height(6.dp))
        // 识别明细：命中列表 / 图中识别到的取件码
        if (matchedCodes.isNotEmpty()) {
            Text(
                "已找到 ${matchedCodes.size} 个快递：${matchedCodes.joinToString("、")}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                if (allCodes.isEmpty()) "图中未识别到任何取件码，请换一张更清晰的照片重试"
                else "图中识别到的取件码：${allCodes.joinToString("、")}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(12.dp))

        // 重新扫描
        Button(
            onClick = onRescan,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("重新扫描", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(12.dp))
    }
}

/**
 * 采样解码原图并绘制定位框。
 * 必须与识别端 decodeSampledBitmapFromUri 使用相同采样逻辑，否则采样图上的
 * 坐标画到全尺寸原图上会整体位移（之前的“识别位置发生位移”根因）。
 */
private fun drawPreview(
    context: Context,
    imageUri: String,
    allCodes: List<String>,
    matchedCodes: List<String>,
    allRects: List<Rect>
): Bitmap? {
    return try {
        val src = decodeSampledBitmapFromUri(context, Uri.parse(imageUri)) ?: return null
        val draw = src.copy(Bitmap.Config.ARGB_8888, true)
        src.recycle()
        val canvas = Canvas(draw)
        val scale = (draw.width / 1080f).coerceAtLeast(1f)
        // 所有匹配到的取件码都画红框
        for (matched in matchedCodes) {
            drawHitBox(canvas, draw, scale, allCodes, matched, allRects)
        }
        draw
    } catch (e: Exception) {
        null
    }
}

/** 命中：粗红框 + 红底白字「已找到」标签 + 大字号取件码 */
private fun drawHitBox(
    canvas: Canvas,
    draw: Bitmap,
    scale: Float,
    allCodes: List<String>,
    matchedCode: String,
    allRects: List<Rect>
) {
    val idx = allCodes.indexOfFirst { it.equals(matchedCode, ignoreCase = true) }
    val rect = if (idx in allRects.indices) allRects[idx] else null
    if (rect == null) return

    val hitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f * scale
        color = 0xFFE53935.toInt()
    }
    val labelBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFE53935.toInt()
    }
    val labelTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 16f * scale
        isFakeBoldText = true
    }
    val codeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE53935.toInt()
        textSize = 28f * scale
        isFakeBoldText = true
    }

    canvas.drawRect(rect, hitPaint)
    val codeBaseline = (rect.top - 10f * scale).coerceAtLeast(codeTextPaint.textSize + 8f * scale)
    canvas.drawText(matchedCode, rect.left.toFloat(), codeBaseline, codeTextPaint)

    val labelText = "已找到"
    val labelWidth = labelTextPaint.measureText(labelText)
    val pad = 6f * scale
    val labelLeft = rect.left.toFloat()
    val labelRight = labelLeft + labelWidth + pad * 2
    val labelBottom = codeBaseline - codeTextPaint.textSize - 6f * scale
    val labelTop = labelBottom - labelTextPaint.textSize - pad * 2
    val finalTop = if (labelTop < 2f * scale) rect.top + 6f * scale else labelTop
    val finalBottom = finalTop + labelTextPaint.textSize + pad * 2
    canvas.drawRoundRect(
        labelLeft, finalTop, labelRight, finalBottom,
        4f * scale, 4f * scale, labelBgPaint
    )
    canvas.drawText(labelText, labelLeft + pad, finalBottom - pad, labelTextPaint)
}

/** 从相册/拍照 Uri 采样解码 Bitmap（长边不超过 1920），必须与识别端逻辑一致，保证标注框不位移 */
private fun decodeSampledBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        val resolver = context.contentResolver
        resolver.openFileDescriptor(uri, "r")?.use { pfd ->
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFileDescriptor(pfd.fileDescriptor, null, bounds)
            var sampleSize = 1
            val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
            while (maxSide / (sampleSize * 2) >= 1920) sampleSize *= 2
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
