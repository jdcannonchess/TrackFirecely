package com.trackfiercely.ui.theme

import androidx.compose.ui.graphics.Color

// =============================================================================
// PRIMARY COLORS - Fall Forest Theme (Amber/Burnt Orange)
// =============================================================================
val FallPrimary = Color(0xFFD4843E)              // Amber/Burnt Orange - main accent
val FallPrimaryLight = Color(0xFFE09B5A)         // Lighter for highlights
val FallPrimaryDark = Color(0xFFB86D2A)          // Darker for containers
val FallContainer = Color(0xFFFFF3E6)            // Light mode container
val DarkFallContainer = Color(0xFF4A3526)        // Dark mode container

// Secondary - Forest Green (nature, trees)
val ForestGreen = Color(0xFF4A6741)              // Deep forest green
val ForestGreenLight = Color(0xFF6B8B61)         // Lighter green
val ForestGreenDark = Color(0xFF3A5234)          // Darker green

// Tertiary - Golden Yellow (fallen leaves, sunlight)
val GoldenYellow = Color(0xFFC9A227)             // Rich golden amber
val GoldenYellowLight = Color(0xFFDDB840)
val GoldenYellowDark = Color(0xFFA88A1E)

// Completed/Success - Sage Green (natural, calming)
val SageGreen = Color(0xFF7D9B76)                // Sage green for completion
val SageGreenLight = Color(0xFF9BB594)
val SageGreenDark = Color(0xFF5F7A58)

// =============================================================================
// CATEGORY COLORS - Earthy Fall Palette
// =============================================================================
val HealthFitnessColor = Color(0xFFB54A3C)       // Rusty Red (maple leaves)
val HealthFitnessLight = Color(0xFFE8B5AE)
val HealthFitnessDark = Color(0xFF8C3930)

val FamilyColor = Color(0xFF5E8B7E)              // Soft Teal (pine needles)
val FamilyLight = Color(0xFFA8C9C0)
val FamilyDark = Color(0xFF4A6E63)

val HomeChoresColor = Color(0xFFD4843E)          // Burnt Orange (fall leaves)
val HomeChoresLight = Color(0xFFF0D4B8)
val HomeChoresDark = Color(0xFFB86D2A)

val HobbiesColor = Color(0xFF8B6F47)             // Warm Brown (tree bark)
val HobbiesLight = Color(0xFFCBB89E)
val HobbiesDark = Color(0xFF6B5436)

val PersonalGrowthColor = Color(0xFF7D9B76)      // Sage Green (moss, growth)
val PersonalGrowthLight = Color(0xFFC5D9C0)
val PersonalGrowthDark = Color(0xFF5F7A58)

val WorkColor = Color(0xFF6B7B8C)                // Slate Blue-Gray (morning mist)
val WorkLight = Color(0xFFB5C0CB)
val WorkDark = Color(0xFF4F5D6B)

// =============================================================================
// SEMANTIC COLORS - Natural Palette
// =============================================================================
val SuccessGreen = SageGreen
val WarningAmber = GoldenYellow
val DangerRed = Color(0xFFB54A3C)                // Rusty Red
val InfoBlue = Color(0xFF6B7B8C)                 // Misty slate

// =============================================================================
// MOOD COLORS (Earth tones gradient)
// =============================================================================
val MoodColors = listOf(
    Color(0xFFB54A3C),  // 1 - Rusty Red
    Color(0xFFC75F45),  // 2 - Burnt Sienna
    Color(0xFFD4843E),  // 3 - Burnt Orange
    Color(0xFFDBA042),  // 4 - Amber
    Color(0xFFC9A227),  // 5 - Golden
    Color(0xFFA0A840),  // 6 - Olive Gold
    Color(0xFF7D9B76),  // 7 - Sage
    Color(0xFF5E8B7E),  // 8 - Teal Pine
    Color(0xFF4A6741),  // 9 - Forest Green
    Color(0xFF3A5234)   // 10 - Deep Forest
)

fun getMoodColor(mood: Int): Color {
    return MoodColors.getOrElse(mood - 1) { MoodColors[4] }
}

// =============================================================================
// LIGHT THEME COLORS - Warm daylight through trees
// =============================================================================
val LightBackground = Color(0xFFFAF8F5)          // Warm off-white
val LightSurface = Color(0xFFFFFBF7)             // Creamy white
val LightSurfaceAlt = Color(0xFFF5F0E8)          // Warm beige tint

val TextPrimaryDark = Color(0xFF2A2520)          // Warm dark brown
val TextSecondaryDark = Color(0xFF5A5248)        // Muted brown
val TextMutedDark = Color(0xFF8A8278)            // Light brown
val TextOnColorLight = Color(0xFFFFFBF7)         // Creamy white

val StrokeLight = Color(0xFFE5DED4)              // Warm light stroke

// =============================================================================
// DARK THEME COLORS - Dusk in the forest
// =============================================================================
val DarkBackground = Color(0xFF1A1A18)           // Deep forest brown
val DarkSurface = Color(0xFF2A2A26)              // Warm dark brown
val DarkSurfaceAlt = Color(0xFF3A3A34)           // Mossy brown

val TextPrimaryLight = Color(0xFFF5F0E8)         // Warm off-white
val TextSecondaryLight = Color(0xFFB5A998)       // Warm gray
val TextMutedLight = Color(0xFF7A7268)           // Muted brown

val StrokeDark = Color(0xFF4A4A42)               // Warm dark stroke

// =============================================================================
// COMPLETION INDICATOR COLORS
// =============================================================================
val CompletedColor = SageGreen
val PendingColor = Color(0xFFCBC4B8)             // Warm gray
val SkippedColor = Color(0xFF9A9488)             // Muted warm gray

// =============================================================================
// LEGACY ALIAS - For compatibility during migration
// =============================================================================
val EmeraldPrimary = FallPrimary                 // Alias for gradual migration
