package com.trackfiercely.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.trackfiercely.data.model.Category
import com.trackfiercely.data.model.Task
import com.trackfiercely.data.model.TaskInputType
import com.trackfiercely.data.model.ScheduleType
import com.trackfiercely.data.repository.TaskRepository
import com.trackfiercely.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month

data class CreateTaskUiState(
    val taskName: String = "",
    val selectedCategory: Category = Category.HEALTH_FITNESS,
    
    // Input type
    val inputType: TaskInputType = TaskInputType.CHECKBOX,
    val sliderMin: Int = 1,
    val sliderMax: Int = 10,
    val starCount: Int = 5,
    val numberSuffix: String = "",
    val numberIsInteger: Boolean = false,
    val photoTimerSeconds: Int = 5,
    
    // Scheduled time (hour only, for ordering tasks)
    val scheduledHour: Int? = null, // 0-23, null = no specific time
    
    // Schedule type
    val scheduleType: ScheduleType = ScheduleType.WEEKLY,
    
    // For WEEKLY schedule
    val selectedDays: Set<DayOfWeek> = emptySet(),
    
    // For ONE_TIME schedule
    val scheduledDate: LocalDate = LocalDate.now(),
    val autoRollover: Boolean = true,
    
    // For MONTHLY schedule
    val monthlyScheduleMode: MonthlyScheduleMode = MonthlyScheduleMode.DAY_OF_MONTH,
    val monthlyDay: Int = 1, // 1-31
    val monthlyWeek: Int = 1, // 1-5 (1st, 2nd, 3rd, 4th, 5th/last)
    val monthlyDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    
    // For YEARLY schedule
    val yearlyMonth: Month = Month.JANUARY,
    val yearlyScheduleMode: YearlyScheduleMode = YearlyScheduleMode.SPECIFIC_DATE,
    val yearlyDay: Int = 1, // 1-31
    val yearlyWeek: Int = 1, // 1-5
    val yearlyDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    
    val showDatePicker: Boolean = false,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    val editingTaskId: Long? = null,
    val isEditMode: Boolean = false,
    val isSystemTask: Boolean = false,
    val isHidden: Boolean = false
) {
    val isValid: Boolean
        get() = taskName.isNotBlank()
    
    // Warning when no days selected for weekly tasks (but still allows save)
    val showNoDaysWarning: Boolean
        get() = scheduleType == ScheduleType.WEEKLY && selectedDays.isEmpty() && !isHidden
    
    val title: String
        get() = if (isEditMode) "Edit Task" else "Create Task"
    
    val formattedScheduledDate: String
        get() = DateUtils.formatFull(scheduledDate)
    
    val canEditInputType: Boolean
        get() = !isSystemTask || !isEditMode
    
    // Display text for scheduled hour
    val scheduledHourDisplay: String
        get() = scheduledHour?.let { hour ->
            when {
                hour == 0 -> "12 AM"
                hour < 12 -> "$hour AM"
                hour == 12 -> "12 PM"
                else -> "${hour - 12} PM"
            }
        } ?: "No specific time"
}

enum class MonthlyScheduleMode {
    DAY_OF_MONTH,  // e.g., "On day 15"
    NTH_WEEKDAY    // e.g., "On 2nd Tuesday"
}

enum class YearlyScheduleMode {
    SPECIFIC_DATE, // e.g., "January 15"
    NTH_WEEKDAY    // e.g., "2nd Tuesday of January"
}

class CreateTaskViewModel(
    private val taskRepository: TaskRepository,
    private val editTaskId: Long? = null
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CreateTaskUiState())
    val uiState: StateFlow<CreateTaskUiState> = _uiState.asStateFlow()
    
    init {
        if (editTaskId != null) {
            loadTask(editTaskId)
        }
        // New tasks start with no days selected - user can add or hide the task
    }
    
    private fun loadTask(taskId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val task = taskRepository.getTask(taskId)
            if (task != null) {
                val config = parseInputConfig(task.inputConfig)
                
                _uiState.update { state ->
                    state.copy(
                        taskName = task.name,
                        selectedCategory = task.category,
                        inputType = task.inputType,
                        sliderMin = config.minValue,
                        sliderMax = config.maxValue,
                        starCount = config.starCount,
                        numberSuffix = config.suffix,
                        numberIsInteger = config.isInteger,
                        photoTimerSeconds = config.timerSeconds,
                        scheduledHour = task.scheduledHour,
                        scheduleType = task.scheduleType,
                        selectedDays = task.getAssignedDaysList().toSet(),
                        scheduledDate = task.scheduledDate?.let { DateUtils.fromEpochMillis(it) } ?: LocalDate.now(),
                        autoRollover = task.autoRollover,
                        monthlyDay = task.monthlyDay ?: 1,
                        monthlyWeek = task.monthlyWeek ?: 1,
                        monthlyDayOfWeek = task.monthlyDayOfWeek?.let { DayOfWeek.of(it) } ?: DayOfWeek.MONDAY,
                        monthlyScheduleMode = if (task.monthlyDay != null) MonthlyScheduleMode.DAY_OF_MONTH else MonthlyScheduleMode.NTH_WEEKDAY,
                        yearlyMonth = task.yearlyMonth?.let { Month.of(it) } ?: Month.JANUARY,
                        yearlyDay = task.yearlyDay ?: 1,
                        yearlyWeek = task.yearlyWeek ?: 1,
                        yearlyDayOfWeek = task.yearlyDayOfWeek?.let { DayOfWeek.of(it) } ?: DayOfWeek.MONDAY,
                        yearlyScheduleMode = if (task.yearlyDay != null) YearlyScheduleMode.SPECIFIC_DATE else YearlyScheduleMode.NTH_WEEKDAY,
                        editingTaskId = taskId,
                        isEditMode = true,
                        isSystemTask = task.isSystemTask,
                        isHidden = task.isHidden,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { 
                    it.copy(isLoading = false, errorMessage = "Task not found")
                }
            }
        }
    }
    
    // ============ BASIC FIELDS ============
    
    fun updateName(name: String) {
        _uiState.update { it.copy(taskName = name, errorMessage = null) }
    }
    
    fun selectCategory(category: Category) {
        _uiState.update { it.copy(selectedCategory = category) }
    }
    
    // ============ INPUT TYPE ============
    
    fun selectInputType(inputType: TaskInputType) {
        _uiState.update { it.copy(inputType = inputType) }
    }
    
    fun setSliderRange(min: Int, max: Int) {
        _uiState.update { it.copy(sliderMin = min, sliderMax = max) }
    }
    
    fun setStarCount(count: Int) {
        _uiState.update { it.copy(starCount = count.coerceIn(1, 10)) }
    }
    
    fun setNumberConfig(suffix: String, isInteger: Boolean) {
        _uiState.update { it.copy(numberSuffix = suffix, numberIsInteger = isInteger) }
    }
    
    fun setPhotoTimerSeconds(seconds: Int) {
        _uiState.update { it.copy(photoTimerSeconds = seconds) }
    }
    
    // ============ SCHEDULED HOUR (for ordering) ============
    
    fun setScheduledHour(hour: Int?) {
        _uiState.update { it.copy(scheduledHour = hour?.coerceIn(0, 23)) }
    }
    
    fun clearScheduledHour() {
        _uiState.update { it.copy(scheduledHour = null) }
    }
    
    // ============ SCHEDULE TYPE ============
    
    fun setScheduleType(scheduleType: ScheduleType) {
        _uiState.update { state ->
            state.copy(
                scheduleType = scheduleType,
                scheduledDate = if (scheduleType == ScheduleType.ONE_TIME) LocalDate.now() else state.scheduledDate
            )
        }
    }
    
    // ============ WEEKLY SCHEDULE ============
    
    fun toggleDay(day: DayOfWeek) {
        _uiState.update { state ->
            val newDays = if (state.selectedDays.contains(day)) {
                state.selectedDays - day
            } else {
                state.selectedDays + day
            }
            state.copy(selectedDays = newDays)
        }
    }
    
    fun selectAllDays() {
        _uiState.update { it.copy(selectedDays = DayOfWeek.entries.toSet()) }
    }
    
    fun selectWeekdays() {
        _uiState.update {
            it.copy(selectedDays = setOf(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
            ))
        }
    }
    
    fun selectWeekends() {
        _uiState.update {
            it.copy(selectedDays = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))
        }
    }
    
    fun clearDays() {
        _uiState.update { it.copy(selectedDays = emptySet()) }
    }
    
    // ============ ONE-TIME SCHEDULE ============
    
    fun selectToday() {
        _uiState.update { it.copy(scheduledDate = LocalDate.now()) }
    }
    
    fun selectTomorrow() {
        _uiState.update { it.copy(scheduledDate = LocalDate.now().plusDays(1)) }
    }
    
    fun selectThisWeekend() {
        val today = LocalDate.now()
        val daysUntilSaturday = (DayOfWeek.SATURDAY.value - today.dayOfWeek.value + 7) % 7
        val saturday = if (daysUntilSaturday == 0 && today.dayOfWeek == DayOfWeek.SATURDAY) {
            today
        } else {
            today.plusDays(daysUntilSaturday.toLong())
        }
        _uiState.update { it.copy(scheduledDate = saturday) }
    }
    
    fun selectNextMonday() {
        val today = LocalDate.now()
        val daysUntilMonday = (DayOfWeek.MONDAY.value - today.dayOfWeek.value + 7) % 7
        val nextMonday = if (daysUntilMonday == 0) {
            today.plusWeeks(1)
        } else {
            today.plusDays(daysUntilMonday.toLong())
        }
        _uiState.update { it.copy(scheduledDate = nextMonday) }
    }
    
    fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(scheduledDate = date, showDatePicker = false) }
    }
    
    fun showDatePicker() {
        _uiState.update { it.copy(showDatePicker = true) }
    }
    
    fun hideDatePicker() {
        _uiState.update { it.copy(showDatePicker = false) }
    }
    
    fun setAutoRollover(enabled: Boolean) {
        _uiState.update { it.copy(autoRollover = enabled) }
    }
    
    // ============ MONTHLY SCHEDULE ============
    
    fun setMonthlyScheduleMode(mode: MonthlyScheduleMode) {
        _uiState.update { it.copy(monthlyScheduleMode = mode) }
    }
    
    fun setMonthlyDay(day: Int) {
        _uiState.update { it.copy(monthlyDay = day.coerceIn(1, 31)) }
    }
    
    fun setMonthlyWeek(week: Int) {
        _uiState.update { it.copy(monthlyWeek = week.coerceIn(1, 5)) }
    }
    
    fun setMonthlyDayOfWeek(dayOfWeek: DayOfWeek) {
        _uiState.update { it.copy(monthlyDayOfWeek = dayOfWeek) }
    }
    
    // ============ YEARLY SCHEDULE ============
    
    fun setYearlyMonth(month: Month) {
        _uiState.update { it.copy(yearlyMonth = month) }
    }
    
    fun setYearlyScheduleMode(mode: YearlyScheduleMode) {
        _uiState.update { it.copy(yearlyScheduleMode = mode) }
    }
    
    fun setYearlyDay(day: Int) {
        _uiState.update { it.copy(yearlyDay = day.coerceIn(1, 31)) }
    }
    
    fun setYearlyWeek(week: Int) {
        _uiState.update { it.copy(yearlyWeek = week.coerceIn(1, 5)) }
    }
    
    fun setYearlyDayOfWeek(dayOfWeek: DayOfWeek) {
        _uiState.update { it.copy(yearlyDayOfWeek = dayOfWeek) }
    }
    
    // ============ HIDDEN ============
    
    fun setTaskHidden(hidden: Boolean) {
        _uiState.update { it.copy(isHidden = hidden) }
    }
    
    // ============ SAVE ============
    
    fun saveTask() {
        val state = _uiState.value
        
        if (state.taskName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter a task name") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val inputConfig = buildInputConfig(state)
                
                val task = Task(
                    id = state.editingTaskId ?: 0,
                    name = state.taskName.trim(),
                    category = state.selectedCategory,
                    inputType = state.inputType,
                    inputConfig = inputConfig,
                    scheduleType = state.scheduleType,
                    assignedDays = if (state.scheduleType == ScheduleType.WEEKLY) {
                        Task.createDaysMask(state.selectedDays.toList())
                    } else 0,
                    scheduledDate = if (state.scheduleType == ScheduleType.ONE_TIME) {
                        DateUtils.toEpochMillis(state.scheduledDate)
                    } else null,
                    monthlyDay = if (state.scheduleType == ScheduleType.MONTHLY && state.monthlyScheduleMode == MonthlyScheduleMode.DAY_OF_MONTH) {
                        state.monthlyDay
                    } else null,
                    monthlyWeek = if (state.scheduleType == ScheduleType.MONTHLY && state.monthlyScheduleMode == MonthlyScheduleMode.NTH_WEEKDAY) {
                        state.monthlyWeek
                    } else null,
                    monthlyDayOfWeek = if (state.scheduleType == ScheduleType.MONTHLY && state.monthlyScheduleMode == MonthlyScheduleMode.NTH_WEEKDAY) {
                        state.monthlyDayOfWeek.value
                    } else null,
                    yearlyMonth = if (state.scheduleType == ScheduleType.YEARLY) {
                        state.yearlyMonth.value
                    } else null,
                    yearlyDay = if (state.scheduleType == ScheduleType.YEARLY && state.yearlyScheduleMode == YearlyScheduleMode.SPECIFIC_DATE) {
                        state.yearlyDay
                    } else null,
                    yearlyWeek = if (state.scheduleType == ScheduleType.YEARLY && state.yearlyScheduleMode == YearlyScheduleMode.NTH_WEEKDAY) {
                        state.yearlyWeek
                    } else null,
                    yearlyDayOfWeek = if (state.scheduleType == ScheduleType.YEARLY && state.yearlyScheduleMode == YearlyScheduleMode.NTH_WEEKDAY) {
                        state.yearlyDayOfWeek.value
                    } else null,
                    autoRollover = state.autoRollover,
                    isSystemTask = state.isSystemTask,
                    isHidden = state.isHidden,
                    scheduledHour = state.scheduledHour
                )
                
                if (state.isEditMode) {
                    taskRepository.updateTask(task)
                } else {
                    val taskId = taskRepository.createTask(task)
                    val newTask = taskRepository.getTask(taskId)
                    newTask?.let { taskRepository.setupTaskForCurrentWeek(it) }
                }
                
                _uiState.update { it.copy(isSaved = true, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(isLoading = false, errorMessage = "Failed to save task: ${e.message}")
                }
            }
        }
    }
    
    private fun buildInputConfig(state: CreateTaskUiState): String {
        return when (state.inputType) {
            TaskInputType.CHECKBOX -> "{}"
            TaskInputType.SLIDER -> JSONObject().apply {
                put("minValue", state.sliderMin)
                put("maxValue", state.sliderMax)
            }.toString()
            TaskInputType.STARS -> JSONObject().apply {
                put("starCount", state.starCount)
            }.toString()
            TaskInputType.NUMBER -> JSONObject().apply {
                put("suffix", state.numberSuffix)
                put("isInteger", state.numberIsInteger)
            }.toString()
            TaskInputType.PHOTO -> JSONObject().apply {
                put("defaultTimerSeconds", state.photoTimerSeconds)
            }.toString()
            TaskInputType.BLOOD_PRESSURE -> "{}"
        }
    }
    
    private fun parseInputConfig(json: String): InputConfigData {
        return try {
            val obj = JSONObject(json)
            InputConfigData(
                minValue = obj.optInt("minValue", 1),
                maxValue = obj.optInt("maxValue", 10),
                starCount = obj.optInt("starCount", 5),
                suffix = obj.optString("suffix", ""),
                isInteger = obj.optBoolean("isInteger", false),
                timerSeconds = obj.optInt("defaultTimerSeconds", 5)
            )
        } catch (e: Exception) {
            InputConfigData()
        }
    }
    
    class Factory(
        private val taskRepository: TaskRepository,
        private val editTaskId: Long? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CreateTaskViewModel(taskRepository, editTaskId) as T
        }
    }
}

private data class InputConfigData(
    val minValue: Int = 1,
    val maxValue: Int = 10,
    val starCount: Int = 5,
    val suffix: String = "",
    val isInteger: Boolean = false,
    val timerSeconds: Int = 5
)
