package com.thigazhini_labs.samuraijack.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Design tokens for consistent spacing throughout the app.
 * Based on Material Design 3 spacing scale (4dp base unit).
 */
object Spacing {
    val xs = 2.dp      // Extra small
    val sm = 4.dp      // Small
    val md = 8.dp      // Medium
    val lg = 16.dp     // Large
    val xl = 24.dp     // Extra large
    val xxl = 32.dp    // 2X extra large
    val xxxl = 48.dp   // 3X extra large
}

/**
 * Design tokens for consistent corner radiuses.
 */
object Radius {
    val xs = 4.dp      // Extra small
    val sm = 8.dp      // Small
    val md = 12.dp     // Medium
    val lg = 16.dp     // Large
    val xl = 24.dp     // Extra large
    val full = 999.dp  // Fully rounded
}

/**
 * Design tokens for elevation (shadows).
 */
object Elevation {
    val level0 = 0.dp
    val level1 = 1.dp
    val level2 = 3.dp
    val level3 = 6.dp
    val level4 = 8.dp
    val level5 = 12.dp
}

/**
 * Design tokens for animation durations (milliseconds).
 */
object AnimationDurations {
    const val EXTRA_FAST = 50
    const val FAST = 100
    const val NORMAL = 300
    const val SLOW = 500
    const val EXTRA_SLOW = 1000
}

/**
 * Design tokens for font sizes.
 */
object FontSizes {
    val xs = 10.sp
    val sm = 12.sp
    val md = 14.sp
    val lg = 16.sp
    val xl = 18.sp
    val xxl = 20.sp
    val xxxl = 24.sp
}

/**
 * Design tokens for opacity values.
 */
object Opacity {
    const val FULLY_TRANSPARENT = 0f
    const val VERY_SUBTLE = 0.12f
    const val SUBTLE = 0.24f
    const val MODERATE = 0.38f
    const val EMPHASIS = 0.60f
    const val FULL_EMPHASIS = 0.87f
    const val FULLY_OPAQUE = 1f
}

/**
 * Design tokens for touch targets (minimum size for interactive elements).
 */
object TouchTargets {
    val minimum = 48.dp  // Material Design minimum
    val compact = 32.dp
    val large = 56.dp
}
