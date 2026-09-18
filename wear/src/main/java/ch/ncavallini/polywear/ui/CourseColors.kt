package ch.ncavallini.polywear.ui

import androidx.compose.ui.graphics.Color

/**
 * Deterministic color per course, keyed by the course [code] so that all lessons
 * of the same course (lecture `V`, exercise `U`, lab `P`, …) get the same color.
 */
object CourseColors {
    // ETH Zürich brand palette.
    private val palette = listOf(
        Color(0xFF215CAF), // blue
        Color(0xFF007894), // petrol
        Color(0xFF627313), // green
        Color(0xFF8E6713), // bronze
        Color(0xFFB7352D), // red
        Color(0xFFA7117A), // purple
        Color(0xFF6F6F6F), // grey
    )
    private val fallback = Color(0xFF6F6F6F) // grey

    fun forCode(code: String?): Color {
        if (code.isNullOrBlank()) return fallback
        val index = (code.hashCode() % palette.size + palette.size) % palette.size
        return palette[index]
    }
}
