package com.trackfiercely.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.trackfiercely.data.model.TaskCompletion
import com.trackfiercely.data.repository.TaskRepository
import com.trackfiercely.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Represents a progress photo with its associated weight data
 */
data class ProgressPhotoEntry(
    val completion: TaskCompletion,
    val date: LocalDate,
    val photoUri: String,
    val weight: Float?,
    val isWeightExact: Boolean // true if weight is from same date, false if closest
)

/**
 * Comparison preset options
 */
enum class ComparePreset(val label: String, val daysBack: Long) {
    ONE_WEEK("1 Week", 7),
    ONE_MONTH("1 Month", 30),
    THREE_MONTHS("3 Months", 90),
    SIX_MONTHS("6 Months", 180),
    ONE_YEAR("1 Year", 365),
    FIRST("First", -1) // Special value meaning oldest photo
}

data class ProgressGalleryUiState(
    val photos: List<ProgressPhotoEntry> = emptyList(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = true,
    val isCompareMode: Boolean = false,
    val compareLeftIndex: Int = 0,
    val compareRightIndex: Int = 0,
    val hasPhotos: Boolean = false
) {
    val currentPhoto: ProgressPhotoEntry? get() = photos.getOrNull(currentIndex)
    val compareLeftPhoto: ProgressPhotoEntry? get() = photos.getOrNull(compareLeftIndex)
    val compareRightPhoto: ProgressPhotoEntry? get() = photos.getOrNull(compareRightIndex)
    
    val weightChange: Float? get() {
        val leftWeight = compareLeftPhoto?.weight
        val rightWeight = compareRightPhoto?.weight
        return if (leftWeight != null && rightWeight != null) {
            leftWeight - rightWeight
        } else null
    }
}

class ProgressGalleryViewModel(
    private val taskRepository: TaskRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProgressGalleryUiState())
    val uiState: StateFlow<ProgressGalleryUiState> = _uiState.asStateFlow()
    
    init {
        loadProgressPhotos()
    }
    
    private fun loadProgressPhotos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val completions = taskRepository.getProgressPhotos()
            
            // Convert completions to photo entries with weight data
            val photoEntries = completions.mapNotNull { completion ->
                val photoUri = completion.photoUri ?: return@mapNotNull null
                val date = DateUtils.fromEpochMillis(completion.date)
                val (weight, isExact) = taskRepository.getWeightForDateMillis(completion.date)
                
                ProgressPhotoEntry(
                    completion = completion,
                    date = date,
                    photoUri = photoUri,
                    weight = weight,
                    isWeightExact = isExact
                )
            }
            
            _uiState.update { state ->
                state.copy(
                    photos = photoEntries,
                    currentIndex = 0,
                    isLoading = false,
                    hasPhotos = photoEntries.isNotEmpty(),
                    // Default compare: newest vs oldest
                    compareLeftIndex = 0,
                    compareRightIndex = (photoEntries.size - 1).coerceAtLeast(0)
                )
            }
        }
    }
    
    fun setCurrentIndex(index: Int) {
        _uiState.update { state ->
            val safeIndex = index.coerceIn(0, (state.photos.size - 1).coerceAtLeast(0))
            state.copy(currentIndex = safeIndex)
        }
    }
    
    fun goToNext() {
        _uiState.update { state ->
            val newIndex = (state.currentIndex + 1).coerceAtMost(state.photos.size - 1)
            state.copy(currentIndex = newIndex)
        }
    }
    
    fun goToPrevious() {
        _uiState.update { state ->
            val newIndex = (state.currentIndex - 1).coerceAtLeast(0)
            state.copy(currentIndex = newIndex)
        }
    }
    
    // ============ COMPARE MODE ============
    
    fun enterCompareMode() {
        _uiState.update { state ->
            state.copy(
                isCompareMode = true,
                compareLeftIndex = state.currentIndex,
                // Default right side to a comparison based on time
                compareRightIndex = findComparisonIndex(state.currentIndex, ComparePreset.ONE_MONTH)
            )
        }
    }
    
    fun exitCompareMode() {
        _uiState.update { it.copy(isCompareMode = false) }
    }
    
    fun setCompareLeft(index: Int) {
        _uiState.update { state ->
            val safeIndex = index.coerceIn(0, (state.photos.size - 1).coerceAtLeast(0))
            state.copy(compareLeftIndex = safeIndex)
        }
    }
    
    fun setCompareRight(index: Int) {
        _uiState.update { state ->
            val safeIndex = index.coerceIn(0, (state.photos.size - 1).coerceAtLeast(0))
            state.copy(compareRightIndex = safeIndex)
        }
    }
    
    /**
     * Apply a comparison preset relative to the left photo
     */
    fun applyComparePreset(preset: ComparePreset) {
        _uiState.update { state ->
            val newRightIndex = findComparisonIndex(state.compareLeftIndex, preset)
            state.copy(compareRightIndex = newRightIndex)
        }
    }
    
    /**
     * Find the photo index closest to the target date based on preset
     */
    private fun findComparisonIndex(fromIndex: Int, preset: ComparePreset): Int {
        val state = _uiState.value
        if (state.photos.isEmpty()) return 0
        
        val fromPhoto = state.photos.getOrNull(fromIndex) ?: return 0
        
        // For "First" preset, return the oldest photo (last in the list since sorted newest first)
        if (preset == ComparePreset.FIRST) {
            return state.photos.size - 1
        }
        
        val targetDate = fromPhoto.date.minusDays(preset.daysBack)
        
        // Find the photo closest to the target date
        var closestIndex = 0
        var closestDiff = Long.MAX_VALUE
        
        state.photos.forEachIndexed { index, photo ->
            val diff = kotlin.math.abs(java.time.temporal.ChronoUnit.DAYS.between(photo.date, targetDate))
            if (diff < closestDiff) {
                closestDiff = diff
                closestIndex = index
            }
        }
        
        return closestIndex
    }
    
    /**
     * Find photo index for a specific date (or closest)
     */
    fun findIndexForDate(date: LocalDate): Int {
        val state = _uiState.value
        if (state.photos.isEmpty()) return 0
        
        // First try exact match
        val exactIndex = state.photos.indexOfFirst { it.date == date }
        if (exactIndex >= 0) return exactIndex
        
        // Find closest
        var closestIndex = 0
        var closestDiff = Long.MAX_VALUE
        
        state.photos.forEachIndexed { index, photo ->
            val diff = kotlin.math.abs(java.time.temporal.ChronoUnit.DAYS.between(photo.date, date))
            if (diff < closestDiff) {
                closestDiff = diff
                closestIndex = index
            }
        }
        
        return closestIndex
    }
    
    fun refresh() {
        loadProgressPhotos()
    }
    
    // ============ FACTORY ============
    
    class Factory(
        private val taskRepository: TaskRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProgressGalleryViewModel(taskRepository) as T
        }
    }
}

