package com.trackfiercely.viewmodel

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.trackfiercely.data.model.TaskCompletion
import com.trackfiercely.data.repository.TaskRepository
import com.trackfiercely.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

// Extension for DataStore
private val Context.weightDataStore: DataStore<Preferences> by preferencesDataStore(name = "weight_preferences")

/**
 * Time period options for the weight graph
 */
enum class WeightTimePeriod(val label: String, val days: Int) {
    WEEK("Week", 7),
    MONTH("Month", 30),
    THREE_MONTHS("3 Months", 90)
}

/**
 * Weight milestone types
 */
enum class WeightMilestone(val label: String) {
    FIRST_WEIGH_IN("First weigh-in!"),
    LOST_5_LBS("5 lbs lost!"),
    LOST_10_LBS("10 lbs lost!"),
    LOST_15_LBS("15 lbs lost!"),
    LOST_20_LBS("20 lbs lost!"),
    GOAL_REACHED("Goal reached!")
}

/**
 * Represents a weight entry for display
 */
data class WeightEntry(
    val date: LocalDate,
    val weight: Float,
    val dateMillis: Long
)

/**
 * Statistics for the selected period
 */
data class WeightStats(
    val average: Float?,
    val min: Float?,
    val max: Float?,
    val trendPerWeek: Float?, // lbs per week (negative = losing)
    val entries: List<WeightEntry>
)

data class WeightTrackerUiState(
    val currentWeight: Float? = null,
    val goalWeight: Float? = null,
    val startingWeight: Float? = null,
    val selectedPeriod: WeightTimePeriod = WeightTimePeriod.MONTH,
    val stats: WeightStats = WeightStats(null, null, null, null, emptyList()),
    val allEntries: List<WeightEntry> = emptyList(),
    val isLoading: Boolean = true,
    val achievedMilestones: Set<WeightMilestone> = emptySet(),
    val newMilestone: WeightMilestone? = null, // For celebration animation
    val showGoalEditor: Boolean = false
) {
    val progressToGoal: Float? get() {
        val current = currentWeight ?: return null
        val goal = goalWeight ?: return null
        val start = startingWeight ?: return null
        
        if (start == goal) return 1f
        
        val totalToLose = start - goal
        val lost = start - current
        return (lost / totalToLose).coerceIn(0f, 1f)
    }
    
    val weightToGoal: Float? get() {
        val current = currentWeight ?: return null
        val goal = goalWeight ?: return null
        return current - goal
    }
    
    val hasEntries: Boolean get() = allEntries.isNotEmpty()
}

class WeightTrackerViewModel(
    private val taskRepository: TaskRepository,
    private val context: Context
) : ViewModel() {
    
    private val dataStore = context.weightDataStore
    
    companion object {
        private val GOAL_WEIGHT_KEY = floatPreferencesKey("goal_weight")
        private val STARTING_WEIGHT_KEY = floatPreferencesKey("starting_weight")
        private val ACHIEVED_MILESTONES_KEY = stringSetPreferencesKey("achieved_milestones")
    }
    
    private val _uiState = MutableStateFlow(WeightTrackerUiState())
    val uiState: StateFlow<WeightTrackerUiState> = _uiState.asStateFlow()
    
    init {
        loadPreferences()
        loadWeightData()
    }
    
    private fun loadPreferences() {
        viewModelScope.launch {
            dataStore.data.collect { prefs ->
                val goalWeight = prefs[GOAL_WEIGHT_KEY]
                val startingWeight = prefs[STARTING_WEIGHT_KEY]
                val milestones = prefs[ACHIEVED_MILESTONES_KEY]?.mapNotNull { name ->
                    try { WeightMilestone.valueOf(name) } catch (e: Exception) { null }
                }?.toSet() ?: emptySet()
                
                _uiState.update { it.copy(
                    goalWeight = goalWeight,
                    startingWeight = startingWeight,
                    achievedMilestones = milestones
                )}
            }
        }
    }
    
    private fun loadWeightData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val weightTask = taskRepository.getWeightTask()
            if (weightTask == null) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }
            
            // Load all weight entries
            val completions = taskRepository.completionDao.getAllWeightEntries(weightTask.id)
            val entries = completions.mapNotNull { completion ->
                completion.numericValue?.let { weight ->
                    WeightEntry(
                        date = DateUtils.fromEpochMillis(completion.date),
                        weight = weight,
                        dateMillis = completion.date
                    )
                }
            }
            
            val currentWeight = entries.firstOrNull()?.weight
            
            // Update starting weight if this is first entry and no starting weight set
            if (currentWeight != null && _uiState.value.startingWeight == null) {
                setStartingWeight(currentWeight)
            }
            
            _uiState.update { it.copy(
                currentWeight = currentWeight,
                allEntries = entries,
                isLoading = false
            )}
            
            // Load stats for current period
            loadStatsForPeriod(_uiState.value.selectedPeriod)
            
            // Check for new milestones
            checkMilestones()
        }
    }
    
    fun selectPeriod(period: WeightTimePeriod) {
        _uiState.update { it.copy(selectedPeriod = period) }
        loadStatsForPeriod(period)
    }
    
    private fun loadStatsForPeriod(period: WeightTimePeriod) {
        viewModelScope.launch {
            val weightTask = taskRepository.getWeightTask() ?: return@launch
            
            val endDate = LocalDate.now()
            val startDate = endDate.minusDays(period.days.toLong())
            val startMillis = DateUtils.toEpochMillis(startDate)
            val endMillis = DateUtils.toEpochMillis(endDate)
            
            val completions = taskRepository.completionDao.getWeightEntriesInRange(
                weightTask.id, startMillis, endMillis
            )
            
            val entries = completions.mapNotNull { completion ->
                completion.numericValue?.let { weight ->
                    WeightEntry(
                        date = DateUtils.fromEpochMillis(completion.date),
                        weight = weight,
                        dateMillis = completion.date
                    )
                }
            }
            
            val average = taskRepository.completionDao.getAverageNumericValue(
                weightTask.id, startMillis, endMillis
            )
            val min = taskRepository.completionDao.getMinWeightInRange(
                weightTask.id, startMillis, endMillis
            )
            val max = taskRepository.completionDao.getMaxWeightInRange(
                weightTask.id, startMillis, endMillis
            )
            
            // Calculate trend (lbs per week)
            val trend = calculateTrend(entries, period.days)
            
            _uiState.update { it.copy(
                stats = WeightStats(
                    average = average,
                    min = min,
                    max = max,
                    trendPerWeek = trend,
                    entries = entries
                )
            )}
        }
    }
    
    private fun calculateTrend(entries: List<WeightEntry>, days: Int): Float? {
        if (entries.size < 2) return null
        
        // Simple linear regression or just first vs last comparison
        val oldest = entries.firstOrNull() ?: return null
        val newest = entries.lastOrNull() ?: return null
        
        val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(oldest.date, newest.date).toFloat()
        if (daysBetween <= 0) return null
        
        val weightChange = newest.weight - oldest.weight
        val changePerDay = weightChange / daysBetween
        return changePerDay * 7 // Convert to per week
    }
    
    // ============ GOAL MANAGEMENT ============
    
    fun showGoalEditor() {
        _uiState.update { it.copy(showGoalEditor = true) }
    }
    
    fun hideGoalEditor() {
        _uiState.update { it.copy(showGoalEditor = false) }
    }
    
    fun setGoalWeight(weight: Float) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[GOAL_WEIGHT_KEY] = weight
            }
            
            // If no starting weight, use current weight as starting
            if (_uiState.value.startingWeight == null && _uiState.value.currentWeight != null) {
                setStartingWeight(_uiState.value.currentWeight!!)
            }
            
            _uiState.update { it.copy(
                goalWeight = weight,
                showGoalEditor = false
            )}
            
            checkMilestones()
        }
    }
    
    private fun setStartingWeight(weight: Float) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[STARTING_WEIGHT_KEY] = weight
            }
            _uiState.update { it.copy(startingWeight = weight) }
        }
    }
    
    fun clearGoal() {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs.remove(GOAL_WEIGHT_KEY)
                prefs.remove(STARTING_WEIGHT_KEY)
            }
            _uiState.update { it.copy(
                goalWeight = null,
                startingWeight = null,
                showGoalEditor = false
            )}
        }
    }
    
    // ============ MILESTONES ============
    
    private fun checkMilestones() {
        val state = _uiState.value
        val current = state.currentWeight ?: return
        val start = state.startingWeight ?: return
        val achieved = state.achievedMilestones.toMutableSet()
        var newMilestone: WeightMilestone? = null
        
        // First weigh-in
        if (state.allEntries.size == 1 && WeightMilestone.FIRST_WEIGH_IN !in achieved) {
            achieved.add(WeightMilestone.FIRST_WEIGH_IN)
            newMilestone = WeightMilestone.FIRST_WEIGH_IN
        }
        
        val lost = start - current
        
        // Weight loss milestones
        if (lost >= 5 && WeightMilestone.LOST_5_LBS !in achieved) {
            achieved.add(WeightMilestone.LOST_5_LBS)
            newMilestone = WeightMilestone.LOST_5_LBS
        }
        if (lost >= 10 && WeightMilestone.LOST_10_LBS !in achieved) {
            achieved.add(WeightMilestone.LOST_10_LBS)
            newMilestone = WeightMilestone.LOST_10_LBS
        }
        if (lost >= 15 && WeightMilestone.LOST_15_LBS !in achieved) {
            achieved.add(WeightMilestone.LOST_15_LBS)
            newMilestone = WeightMilestone.LOST_15_LBS
        }
        if (lost >= 20 && WeightMilestone.LOST_20_LBS !in achieved) {
            achieved.add(WeightMilestone.LOST_20_LBS)
            newMilestone = WeightMilestone.LOST_20_LBS
        }
        
        // Goal reached
        val goal = state.goalWeight
        if (goal != null && current <= goal && WeightMilestone.GOAL_REACHED !in achieved) {
            achieved.add(WeightMilestone.GOAL_REACHED)
            newMilestone = WeightMilestone.GOAL_REACHED
        }
        
        // Save new milestones
        if (achieved != state.achievedMilestones) {
            viewModelScope.launch {
                dataStore.edit { prefs ->
                    prefs[ACHIEVED_MILESTONES_KEY] = achieved.map { it.name }.toSet()
                }
            }
            _uiState.update { it.copy(
                achievedMilestones = achieved,
                newMilestone = newMilestone
            )}
        }
    }
    
    fun dismissMilestone() {
        _uiState.update { it.copy(newMilestone = null) }
    }
    
    fun refresh() {
        loadWeightData()
    }
    
    // ============ FACTORY ============
    
    class Factory(
        private val taskRepository: TaskRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WeightTrackerViewModel(taskRepository, context) as T
        }
    }
}

