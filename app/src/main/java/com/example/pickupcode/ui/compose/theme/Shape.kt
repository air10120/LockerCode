package com.example.pickupcode.ui.compose.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * M3 Expressive 形状：
 *  - 按钮胶囊形（Button 默认 Full pill）
 *  - 卡片 20dp（large）
 *  - 对话框 28dp（extraLarge）
 */
val PickupCodeShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
