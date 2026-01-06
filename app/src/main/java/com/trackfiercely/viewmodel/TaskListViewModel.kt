package com.trackfiercely.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.trackfiercely.data.model.Category
import com.trackfiercely.data.model.Task
import com.trackfiercely.data.model.TaskInputType
import com.trackfiercely.data.repository.TaskRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class TaskFilter {
    ALL,
    ASSIGNED,
    UNASSIGNED,
    HIDDEN
}

data class TaskListUiState(
    val visibleTasks: List<Task> = emptyList(),
    val hiddenTasks: List<Task> = emptyList(),
    val assignedTasks: List<Task> = emptyList(),
    val unassignedTasks: List<Task> = emptyList(),
    val selectedFilter: TaskFilter = TaskFilter.ALL,
    val searchQuery: String = "",
    val isLoading: Boolean = true
) {
    val displayedTasks: List<Task>
        get() {
            val baseTasks = when (selectedFilter) {
                TaskFilter.ALL -> visibleTasks
                TaskFilter.ASSIGNED -> assignedTasks
                TaskFilter.UNASSIGNED -> unassignedTasks
                TaskFilter.HIDDEN -> hiddenTasks
            }
            
            return if (searchQuery.isBlank()) {
                baseTasks
            } else {
                baseTasks.filter { 
                    it.name.contains(searchQuery, ignoreCase = true) ||
                    it.category.displayName.contains(searchQuery, ignoreCase = true)
                }
            }
        }
    
    val tasksByCategory: Map<Category, List<Task>>
        get() = displayedTasks.groupBy { it.category }
    
    // Counts for filter chips
    val allCount: Int get() = visibleTasks.size
    val assignedCount: Int get() = assignedTasks.size
    val unassignedCount: Int get() = unassignedTasks.size
    val hiddenCount: Int get() = hiddenTasks.size
}

class TaskListViewModel(
    private val taskRepository: TaskRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TaskListUiState())
    val uiState: StateFlow<TaskListUiState> = _uiState.asStateFlow()
    
    init {
        observeTasks()
    }
    
    private fun observeTasks() {
        viewModelScope.launch {
            combine(
                taskRepository.observeVisibleTasks(),
                taskRepository.observeHiddenTasks(),
                taskRepository.observeAssignedTasks(),
                taskRepository.observeUnassignedTasks()
            ) { visible, hidden, assigned, unassigned ->
                TaskListData(visible, hidden, assigned, unassigned)
            }.collect { data ->
                _uiState.update { state ->
                    state.copy(
                        visibleTasks = data.visible,
                        hiddenTasks = data.hidden,
                        assignedTasks = data.assigned,
                        unassignedTasks = data.unassigned,
                        isLoading = false
                    )
                }
            }
        }
    }
    
    fun setFilter(filter: TaskFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }
    
    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }
    
    fun hideTask(taskId: Long) {
        viewModelScope.launch {
            taskRepository.hideTask(taskId)
        }
    }
    
    fun showTask(taskId: Long) {
        viewModelScope.launch {
            taskRepository.showTask(taskId)
        }
    }
    
    fun toggleTaskHidden(taskId: Long) {
        viewModelScope.launch {
            taskRepository.toggleTaskHidden(taskId)
        }
    }
    
    fun archiveTask(taskId: Long) {
        viewModelScope.launch {
            taskRepository.archiveTask(taskId)
        }
    }
    
    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            taskRepository.deleteTask(taskId)
        }
    }
    
    class Factory(
        private val taskRepository: TaskRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TaskListViewModel(taskRepository) as T
        }
    }
}

private data class TaskListData(
    val visible: List<Task>,
    val hidden: List<Task>,
    val assigned: List<Task>,
    val unassigned: List<Task>
)
