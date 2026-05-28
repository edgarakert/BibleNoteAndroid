package ru.edgarakert.biblenote.data.bible

import androidx.compose.ui.graphics.Color

enum class HighlightColor(val colorName: String) {
    AMBER("amber"),
    ROSE("rose"),
    SAGE("sage"),
    SKY("sky"),
    LAVENDER("lavender");

    val lightColor: Color
        get() = when (this) {
            AMBER    -> Color(0xFFF5D78E)
            ROSE     -> Color(0xFFF5B8B8)
            SAGE     -> Color(0xFFB8D4B8)
            SKY      -> Color(0xFFB8D0E8)
            LAVENDER -> Color(0xFFD0B8E8)
        }

    val darkColor: Color
        get() = when (this) {
            AMBER    -> Color(0xFF42300D)
            ROSE     -> Color(0xFF4D1717)
            SAGE     -> Color(0xFF143814)
            SKY      -> Color(0xFF0F264D)
            LAVENDER -> Color(0xFF2E1449)
        }

    companion object {
        fun fromName(name: String?): HighlightColor? =
            entries.firstOrNull { it.colorName == name }
    }
}
