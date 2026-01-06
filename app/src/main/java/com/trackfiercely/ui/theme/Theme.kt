package com.trackfiercely.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.trackfiercely.data.model.Category

// =============================================================================
// LIGHT COLOR SCHEME - Morning in the fall forest
// =============================================================================
private val LightColorScheme = lightColorScheme(
    // Primary - Amber/Burnt Orange
    primary = FallPrimary,
    onPrimary = TextOnColorLight,
    primaryContainer = FallContainer,
    onPrimaryContainer = TextPrimaryDark,
    
    // Secondary - Forest Green
    secondary = ForestGreen,
    onSecondary = TextOnColorLight,
    secondaryContainer = Color(0xFFD4E8D0),
    onSecondaryContainer = TextPrimaryDark,
    
    // Tertiary - Golden Yellow
    tertiary = GoldenYellow,
    onTertiary = TextPrimaryDark,
    tertiaryContainer = Color(0xFFFFF0C8),
    onTertiaryContainer = TextPrimaryDark,
    
    // Background & Surface
    background = LightBackground,
    onBackground = TextPrimaryDark,
    surface = LightSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = LightSurfaceAlt,
    onSurfaceVariant = TextSecondaryDark,
    
    // Error - Rusty Red
    error = DangerRed,
    onError = TextOnColorLight,
    errorContainer = Color(0xFFFFDAD4),
    onErrorContainer = Color(0xFF6B2820),
    
    // Outline
    outline = TextMutedDark,
    outlineVariant = StrokeLight,
    
    // Inverse
    inverseSurface = TextPrimaryDark,
    inverseOnSurface = LightBackground,
    inversePrimary = FallPrimaryLight,
    
    // Surface tint
    surfaceTint = FallPrimary
)

// =============================================================================
// DARK COLOR SCHEME - Dusk in the forest
// =============================================================================
private val DarkColorScheme = darkColorScheme(
    // Primary - Amber/Burnt Orange (lighter for dark mode)
    primary = FallPrimaryLight,
    onPrimary = TextPrimaryDark,
    primaryContainer = DarkFallContainer,
    onPrimaryContainer = TextPrimaryLight,
    
    // Secondary - Forest Green
    secondary = ForestGreenLight,
    onSecondary = TextPrimaryDark,
    secondaryContainer = ForestGreenDark,
    onSecondaryContainer = TextPrimaryLight,
    
    // Tertiary - Golden Yellow
    tertiary = GoldenYellowLight,
    onTertiary = TextPrimaryDark,
    tertiaryContainer = GoldenYellowDark,
    onTertiaryContainer = TextPrimaryLight,
    
    // Background & Surface - Deep forest browns
    background = DarkBackground,
    onBackground = TextPrimaryLight,
    surface = DarkSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = DarkSurfaceAlt,
    onSurfaceVariant = TextSecondaryLight,
    
    // Error - Rusty Red
    error = Color(0xFFCF6B60),
    onError = TextOnColorLight,
    errorContainer = Color(0xFF5A2520),
    onErrorContainer = Color(0xFFFFDAD4),
    
    // Outline
    outline = TextMutedLight,
    outlineVariant = StrokeDark,
    
    // Inverse
    inverseSurface = TextPrimaryLight,
    inverseOnSurface = DarkBackground,
    inversePrimary = FallPrimaryDark,
    
    // Surface tint
    surfaceTint = FallPrimaryLight
)

// =============================================================================
// THEME COMPOSABLE
// =============================================================================
@Composable
fun TrackFiercelyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FierceTypography,
        content = content
    )
}

// =============================================================================
// CATEGORY COLOR HELPERS
// =============================================================================

/**
 * Returns the primary color for a task category
 */
@Composable
fun getCategoryColor(category: Category): Color {
    return when (category) {
        Category.HEALTH_FITNESS -> HealthFitnessColor
        Category.FAMILY -> FamilyColor
        Category.HOME_CHORES -> HomeChoresColor
        Category.HOBBIES -> HobbiesColor
        Category.PERSONAL_GROWTH -> PersonalGrowthColor
        Category.WORK -> WorkColor
    }
}

/**
 * Returns the container/background color for a category
 */
@Composable
fun getCategoryContainerColor(category: Category): Color {
    return when (category) {
        Category.HEALTH_FITNESS -> HealthFitnessLight
        Category.FAMILY -> FamilyLight
        Category.HOME_CHORES -> HomeChoresLight
        Category.HOBBIES -> HobbiesLight
        Category.PERSONAL_GROWTH -> PersonalGrowthLight
        Category.WORK -> WorkLight
    }
}

/**
 * Returns the darker variant for a category
 */
@Composable
fun getCategoryColorDark(category: Category): Color {
    return when (category) {
        Category.HEALTH_FITNESS -> HealthFitnessDark
        Category.FAMILY -> FamilyDark
        Category.HOME_CHORES -> HomeChoresDark
        Category.HOBBIES -> HobbiesDark
        Category.PERSONAL_GROWTH -> PersonalGrowthDark
        Category.WORK -> WorkDark
    }
}

// =============================================================================
// SEMANTIC COLORS OBJECT
// =============================================================================
object SemanticColors {
    val success = SuccessGreen
    val warning = WarningAmber
    val error = DangerRed
    val info = InfoBlue
}
