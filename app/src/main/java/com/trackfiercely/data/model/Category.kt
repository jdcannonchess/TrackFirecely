package com.trackfiercely.data.model

import androidx.compose.ui.graphics.Color

/**
 * Task categories with associated colors for visual organization
 */
enum class Category(
    val displayName: String,
    val colorHex: Long
) {
    HEALTH_FITNESS("Health & Fitness", 0xFFEF6C57),      // Coral
    FAMILY("Family", 0xFF14B8A6),                        // Teal
    HOME_CHORES("Home & Chores", 0xFFF59E0B),            // Amber
    HOBBIES("Hobbies", 0xFF8B5CF6),                      // Purple
    PERSONAL_GROWTH("Personal Growth", 0xFF22C55E),      // Green
    WORK("Work", 0xFF3B82F6);                            // Blue
    
    val color: Color get() = Color(colorHex)
    
    companion object {
        fun fromDisplayName(name: String): Category? {
            return entries.find { it.displayName == name }
        }
    }
}

