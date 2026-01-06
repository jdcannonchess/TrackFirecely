package com.trackfiercely.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.trackfiercely.data.model.BloodPressureData
import com.trackfiercely.data.model.BloodPressureReading
import com.trackfiercely.data.model.ScheduleType
import com.trackfiercely.data.model.Task
import com.trackfiercely.data.model.TaskCompletion
import com.trackfiercely.data.repository.TaskRepository
import com.trackfiercely.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Represents a task with its completion status and value for display
 */
data class TaskWithStatus(
    val task: Task,
    val isCompleted: Boolean,
    val completion: TaskCompletion? = null
) {
    val numericValue: Float? get() = completion?.numericValue
    val starValue: Int? get() = completion?.starValue
    val photoUri: String? get() = completion?.photoUri
}

data class HomeUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val tasksForDay: List<TaskWithStatus> = emptyList(),
    val isLoading: Boolean = true,
    val completionProgress: Float = 0f
) {
    val isToday: Boolean get() = selectedDate == LocalDate.now()
    val formattedDate: String get() = DateUtils.formatFull(selectedDate)
    
    // For pager initialization
    val currentPageIndex: Int get() = DateUtils.daysSinceMinDate(selectedDate)
}

class HomeViewModel(
    private val taskRepository: TaskRepository
) : ViewModel() {
    
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        observeSelectedDate()
    }
    
    private fun observeSelectedDate() {
        viewModelScope.launch {
            _selectedDate.collectLatest { date ->
                _uiState.update { it.copy(
                    selectedDate = date,
                    isLoading = true
                )}
                
                // Observe tasks with completions for the selected date
                taskRepository.observeTasksWithCompletionsForDate(date).collect { tasksWithCompletions ->
                    val dateMillis = DateUtils.toEpochMillis(date)
                    val taskStatusList = tasksWithCompletions.map { taskWithCompletion ->
                        val completion = taskWithCompletion.getCompletionForDate(dateMillis)
                        TaskWithStatus(
                            task = taskWithCompletion.task,
                            isCompleted = completion?.isCompleted == true,
                            completion = completion
                        )
                    }.filter { !it.task.isHidden }
                    
                    // Sort tasks: incomplete first, then by schedule type and time
                    val sortedTasks = taskStatusList.sortedWith(
                        compareBy<TaskWithStatus> { it.isCompleted } // Incomplete first (false < true)
                            .thenBy { 
                                // One-time tasks first, then others
                                if (it.task.scheduleType == ScheduleType.ONE_TIME) 0 else 1 
                            }
                            .thenBy { it.task.scheduledHour ?: Int.MAX_VALUE } // By scheduled time
                            .thenBy { it.task.name } // Finally by name
                    )
                    
                    val completedCount = sortedTasks.count { it.isCompleted }
                    val progress = if (sortedTasks.isNotEmpty()) {
                        completedCount.toFloat() / sortedTasks.size
                    } else 0f
                    
                    _uiState.update { it.copy(
                        tasksForDay = sortedTasks,
                        completionProgress = progress,
                        isLoading = false
                    )}
                }
            }
        }
    }
    
    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }
    
    fun goToToday() {
        _selectedDate.value = LocalDate.now()
    }
    
    fun goToPreviousDay() {
        _selectedDate.value = _selectedDate.value.minusDays(1)
    }
    
    fun goToNextDay() {
        _selectedDate.value = _selectedDate.value.plusDays(1)
    }
    
    // ============ TASK COMPLETION ============
    
    /**
     * Toggle completion for checkbox-type tasks
     */
    fun toggleTaskCompletion(taskId: Long) {
        viewModelScope.launch {
            taskRepository.toggleTaskCompletion(taskId, _selectedDate.value)
        }
    }
    
    /**
     * Set numeric value for slider or number input tasks
     */
    fun setTaskNumericValue(taskId: Long, value: Float) {
        viewModelScope.launch {
            taskRepository.setTaskNumericValue(taskId, _selectedDate.value, value)
        }
    }
    
    /**
     * Set star rating value
     */
    fun setTaskStarValue(taskId: Long, stars: Int) {
        viewModelScope.launch {
            taskRepository.setTaskStarValue(taskId, _selectedDate.value, stars)
        }
    }
    
    /**
     * Set photo URI for photo tasks (called after capture)
     */
    fun setTaskPhotoUri(taskId: Long, uri: String) {
        viewModelScope.launch {
            taskRepository.setTaskPhotoUri(taskId, _selectedDate.value, uri)
        }
    }
    
    /**
     * Add a blood pressure reading
     */
    fun addBpReading(taskId: Long, systolic: Int, diastolic: Int, heartRate: Int) {
        viewModelScope.launch {
            val dateMillis = DateUtils.toEpochMillis(_selectedDate.value)
            val existing = taskRepository.completionDao.getCompletion(taskId, dateMillis)
            
            // Get existing BP data or create new
            val existingData = existing?.bpData?.let { BloodPressureData.fromJson(it) }
                ?: BloodPressureData.empty()
            
            // Add new reading
            val newReading = BloodPressureReading(
                systolic = systolic,
                diastolic = diastolic,
                heartRate = heartRate
            )
            val updatedData = existingData.addReading(newReading)
            
            // Save
            taskRepository.setTaskBpData(taskId, _selectedDate.value, updatedData.toJson())
        }
    }
    
    /**
     * Get the current selected date
     */
    fun getSelectedDate(): LocalDate = _selectedDate.value
    
    // ============ FACTORY ============
    
    class Factory(
        private val taskRepository: TaskRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(taskRepository) as T
        }
    }
}
