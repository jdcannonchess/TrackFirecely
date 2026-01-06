package com.trackfiercely.data.model

/**
 * Configuration for task input types.
 * Stored as JSON in the Task entity.
 */
data class InputConfig(
    // For SLIDER type
    val minValue: Int = 1,
    val maxValue: Int = 10,
    
    // For STARS type
    val starCount: Int = 5,
    
    // For NUMBER type
    val suffix: String = "",
    val isInteger: Boolean = false,
    
    // For PHOTO type
    val defaultTimerSeconds: Int = 5
) {
    companion object {
        val DEFAULT = InputConfig()
        
        fun forSlider(min: Int = 1, max: Int = 10) = InputConfig(minValue = min, maxValue = max)
        
        fun forStars(count: Int = 5) = InputConfig(starCount = count)
        
        fun forNumber(suffix: String = "", isInteger: Boolean = false) = 
            InputConfig(suffix = suffix, isInteger = isInteger)
        
        fun forPhoto(timerSeconds: Int = 5) = InputConfig(defaultTimerSeconds = timerSeconds)
    }
}

