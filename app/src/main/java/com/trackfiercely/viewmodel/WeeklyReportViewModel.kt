package com.trackfiercely.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.trackfiercely.data.model.Category
import com.trackfiercely.data.model.WeeklySnapshot
import com.trackfiercely.data.repository.TaskRepository
import com.trackfiercely.data.repository.WeeklySnapshotRepository
import com.trackfiercely.data.repository.WellnessStats
import com.trackfiercely.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class CategoryStats(
    val category: Category,
    val completed: Int,
    val total: Int
) {
    val percentage: Float get() = if (total > 0) completed.toFloat() / total else 0f
}

data class DayProgress(
    val date: LocalDate,
    val completed: Int,
    val total: Int
) {
    val percentage: Float get() = if (total > 0) completed.toFloat() / total else 0f
    val isPerfect: Boolean get() = total > 0 && completed == total
}

data class WeeklyReportUiState(
    val weekStart: LocalDate = DateUtils.getWeekStart(LocalDate.now()),
    val snapshot: WeeklySnapshot? = null,
    val wellnessStats: WellnessStats? = null,
    val dayProgress: List<DayProgress> = emptyList(),
    val categoryStats: List<CategoryStats> = emptyList(),
    val perfectDays: List<LocalDate> = emptyList(),
    val highlights: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    val isCurrentWeek: Boolean = true
) {
    val weekRangeFormatted: String
        get() = DateUtils.formatWeekRange(weekStart)
    
    val tasksCompleted: Int get() = snapshot?.tasksCompleted ?: 0
    val totalTasks: Int get() = snapshot?.totalTasks ?: 0
    val completionPercentage: Float get() = snapshot?.completionPercentage ?: 0f
    val grade: String get() = snapshot?.grade ?: "N/A"
    
    val avgMood: Float? get() = wellnessStats?.avgMood ?: snapshot?.avgMood
    val avgSleepHours: Float? get() = wellnessStats?.avgSleepHours ?: snapshot?.avgSleepHours
    val avgSleepQuality: Float? get() = wellnessStats?.avgSleepQuality ?: snapshot?.avgSleepQuality
    val totalSteps: Int get() = wellnessStats?.totalSteps ?: snapshot?.totalSteps ?: 0
}

class WeeklyReportViewModel(
    private val weeklySnapshotRepository: WeeklySnapshotRepository,
    private val taskRepository: TaskRepository,
    initialWeekStart: LocalDate = DateUtils.getWeekStart(LocalDate.now())
) : ViewModel() {
    
    private val _weekStart = MutableStateFlow(initialWeekStart)
    private val _uiState = MutableStateFlow(WeeklyReportUiState())
    val uiState: StateFlow<WeeklyReportUiState> = _uiState.asStateFlow()
    
    init {
        observeWeek()
    }
    
    private fun observeWeek() {
        viewModelScope.launch {
            _weekStart.collectLatest { weekStart ->
                _uiState.update { it.copy(
                    weekStart = weekStart,
                    isLoading = true,
                    isCurrentWeek = DateUtils.isCurrentWeek(weekStart)
                )}
                
                loadWeekData(weekStart)
            }
        }
    }
    
    private suspend fun loadWeekData(weekStart: LocalDate) {
        // Generate snapshot if it doesn't exist
        var snapshot = weeklySnapshotRepository.getSnapshotForWeek(weekStart)
        if (snapshot == null) {
            snapshot = weeklySnapshotRepository.generateSnapshot(weekStart)
        }
        
        // Get wellness stats from wellness tasks
        val wellnessStats = taskRepository.getWellnessStatsForWeek(weekStart)
        
        // Get perfect days
        val perfectDays = taskRepository.getPerfectDaysInWeek(weekStart)
        
        // Parse highlights
        val highlights = snapshot.highlights?.split("|")?.filter { it.isNotBlank() } ?: emptyList()
        
        // Calculate category stats (simplified)
        val categoryStats = calculateCategoryStats(weekStart)
        
        // Calculate day-by-day progress
        val dayProgress = calculateDayProgress(weekStart)
        
        _uiState.update { state ->
            state.copy(
                snapshot = snapshot,
                wellnessStats = wellnessStats,
                dayProgress = dayProgress,
                perfectDays = perfectDays,
                highlights = highlights,
                categoryStats = categoryStats,
                isLoading = false
            )
        }
    }
    
    private suspend fun calculateCategoryStats(weekStart: LocalDate): List<CategoryStats> {
        // This is a simplified version - in production, you'd want more detailed category tracking
        return emptyList()
    }
    
    private fun calculateDayProgress(weekStart: LocalDate): List<DayProgress> {
        // Would need to fetch completion data per day
        // For now, return placeholder
        return DateUtils.getWeekDates(weekStart).map { date ->
            DayProgress(date, 0, 0)
        }
    }
    
    fun goToPreviousWeek() {
        _weekStart.value = _weekStart.value.minusWeeks(1)
    }
    
    fun goToNextWeek() {
        _weekStart.value = _weekStart.value.plusWeeks(1)
    }
    
    fun goToCurrentWeek() {
        _weekStart.value = DateUtils.getWeekStart(LocalDate.now())
    }
    
    fun saveSnapshot() {
        viewModelScope.launch {
            val snapshot = _uiState.value.snapshot
            if (snapshot != null) {
                weeklySnapshotRepository.generateAndSaveSnapshot(_weekStart.value)
                _uiState.update { it.copy(isSaved = true) }
            }
        }
    }
    
    class Factory(
        private val weeklySnapshotRepository: WeeklySnapshotRepository,
        private val taskRepository: TaskRepository,
        private val initialWeekStart: LocalDate = DateUtils.getWeekStart(LocalDate.now())
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WeeklyReportViewModel(
                weeklySnapshotRepository, 
                taskRepository,
                initialWeekStart
            ) as T
        }
    }
}
