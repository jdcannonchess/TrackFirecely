package com.trackfiercely.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.trackfiercely.data.model.ScheduleType
import com.trackfiercely.data.model.Task
import com.trackfiercely.data.model.TaskCompletion
import com.trackfiercely.data.repository.TaskRepository
import com.trackfiercely.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Represents a one-off task with its completion info for display
 */
data class OneOffTaskEntry(
    val task: Task,
    val completion: TaskCompletion?,
    val scheduledDate: LocalDate?,
    val isCompleted: Boolean,
    val completedDate: LocalDate?
)

/**
 * Filter options for the history list
 */
enum class HistoryFilter(val label: String) {
    ALL("All"),
    COMPLETED("Completed"),
    INCOMPLETE("Incomplete")
}

data class HistoryUiState(
    val entries: List<OneOffTaskEntry> = emptyList(),
    val isLoading: Boolean = true,
    val selectedFilter: HistoryFilter = HistoryFilter.ALL,
    val totalTasks: Int = 0,
    val completedCount: Int = 0,
    val incompleteCount: Int = 0
)

class HistoryViewModel(
    private val taskRepository: TaskRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()
    
    private var allEntries: List<OneOffTaskEntry> = emptyList()
    
    init {
        loadHistory()
    }
    
    private fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Observe one-time tasks
            taskRepository.observeOneTimeTasks().collect { tasks ->
                // Get completions for each task
                val entries = tasks.mapNotNull { task ->
                    val scheduledDate = task.scheduledDate?.let { DateUtils.fromEpochMillis(it) }
                    val dateMillis = task.scheduledDate ?: return@mapNotNull null
                    
                    val completion = taskRepository.completionDao.getCompletion(task.id, dateMillis)
                    val isCompleted = completion?.isCompleted == true
                    val completedDate = completion?.completedAt?.let { 
                        DateUtils.fromEpochMillis(it) 
                    }
                    
                    OneOffTaskEntry(
                        task = task,
                        completion = completion,
                        scheduledDate = scheduledDate,
                        isCompleted = isCompleted,
                        completedDate = completedDate
                    )
                }.sortedByDescending { it.scheduledDate }
                
                allEntries = entries
                
                val completedCount = entries.count { it.isCompleted }
                val incompleteCount = entries.count { !it.isCompleted }
                
                _uiState.update { state ->
                    state.copy(
                        entries = filterEntries(entries, state.selectedFilter),
                        isLoading = false,
                        totalTasks = entries.size,
                        completedCount = completedCount,
                        incompleteCount = incompleteCount
                    )
                }
            }
        }
    }
    
    fun setFilter(filter: HistoryFilter) {
        _uiState.update { state ->
            state.copy(
                selectedFilter = filter,
                entries = filterEntries(allEntries, filter)
            )
        }
    }
    
    private fun filterEntries(entries: List<OneOffTaskEntry>, filter: HistoryFilter): List<OneOffTaskEntry> {
        return when (filter) {
            HistoryFilter.ALL -> entries
            HistoryFilter.COMPLETED -> entries.filter { it.isCompleted }
            HistoryFilter.INCOMPLETE -> entries.filter { !it.isCompleted }
        }
    }
    
    class Factory(
        private val taskRepository: TaskRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HistoryViewModel(taskRepository) as T
        }
    }
}
