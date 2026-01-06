package com.trackfiercely.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.trackfiercely.data.model.BloodPressureData
import com.trackfiercely.data.model.BloodPressureReading
import com.trackfiercely.data.model.BPClassification
import com.trackfiercely.data.repository.TaskRepository
import com.trackfiercely.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Represents a daily blood pressure entry for display
 */
data class BPDayEntry(
    val date: LocalDate,
    val readings: List<BloodPressureReading>,
    val avgSystolic: Float,
    val avgDiastolic: Float,
    val avgHeartRate: Float,
    val classification: BPClassification
)

/**
 * Statistics for blood pressure over a period
 */
data class BPStats(
    val avgSystolic: Float,
    val avgDiastolic: Float,
    val avgHeartRate: Float,
    val minSystolic: Int,
    val maxSystolic: Int,
    val minDiastolic: Int,
    val maxDiastolic: Int,
    val minHeartRate: Int,
    val maxHeartRate: Int,
    val readingCount: Int,
    val classification: BPClassification
)

/**
 * Time period for viewing BP data
 */
enum class BPTimePeriod(val label: String, val days: Int) {
    WEEK("Week", 7),
    MONTH("Month", 30),
    THREE_MONTHS("3 Months", 90)
}

data class BloodPressureUiState(
    val isLoading: Boolean = true,
    val entries: List<BPDayEntry> = emptyList(),
    val allReadings: List<BloodPressureReading> = emptyList(),
    val selectedPeriod: BPTimePeriod = BPTimePeriod.WEEK,
    val stats: BPStats? = null,
    val latestReading: BloodPressureReading? = null,
    val graphData: List<BPDayEntry> = emptyList(),
    val hasData: Boolean = false
)

class BloodPressureViewModel(
    private val taskRepository: TaskRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(BloodPressureUiState())
    val uiState: StateFlow<BloodPressureUiState> = _uiState.asStateFlow()
    
    init {
        loadBloodPressureData()
    }
    
    private fun loadBloodPressureData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val bpTask = taskRepository.getBloodPressureTask()
            if (bpTask == null) {
                _uiState.update { it.copy(isLoading = false, hasData = false) }
                return@launch
            }
            
            // Get all BP entries
            val completions = taskRepository.completionDao.getAllBpEntries(bpTask.id)
            
            if (completions.isEmpty()) {
                _uiState.update { it.copy(isLoading = false, hasData = false) }
                return@launch
            }
            
            // Parse all entries into daily entries
            val entries = completions.mapNotNull { completion ->
                val bpData = completion.bpData?.let { BloodPressureData.fromJson(it) }
                    ?: return@mapNotNull null
                
                if (bpData.readings.isEmpty()) return@mapNotNull null
                
                val date = DateUtils.fromEpochMillis(completion.date)
                val avgSys = bpData.averageSystolic ?: return@mapNotNull null
                val avgDia = bpData.averageDiastolic ?: return@mapNotNull null
                val avgHr = bpData.averageHeartRate ?: 0f
                
                BPDayEntry(
                    date = date,
                    readings = bpData.readings,
                    avgSystolic = avgSys,
                    avgDiastolic = avgDia,
                    avgHeartRate = avgHr,
                    classification = BPClassification.classify(avgSys.toInt(), avgDia.toInt())
                )
            }.sortedByDescending { it.date }
            
            // Collect all individual readings
            val allReadings = entries.flatMap { it.readings }.sortedByDescending { it.timestamp }
            
            val latestReading = allReadings.firstOrNull()
            
            _uiState.update {
                it.copy(
                    isLoading = false,
                    entries = entries,
                    allReadings = allReadings,
                    latestReading = latestReading,
                    hasData = entries.isNotEmpty()
                )
            }
            
            // Calculate stats and graph data for selected period
            updatePeriodData(_uiState.value.selectedPeriod)
        }
    }
    
    fun selectPeriod(period: BPTimePeriod) {
        _uiState.update { it.copy(selectedPeriod = period) }
        updatePeriodData(period)
    }
    
    private fun updatePeriodData(period: BPTimePeriod) {
        val entries = _uiState.value.entries
        if (entries.isEmpty()) return
        
        val cutoffDate = LocalDate.now().minusDays(period.days.toLong())
        val periodEntries = entries.filter { it.date >= cutoffDate }
        
        if (periodEntries.isEmpty()) {
            _uiState.update { it.copy(graphData = emptyList(), stats = null) }
            return
        }
        
        // Calculate stats from all readings in this period
        val periodReadings = periodEntries.flatMap { it.readings }
        
        if (periodReadings.isEmpty()) {
            _uiState.update { it.copy(graphData = emptyList(), stats = null) }
            return
        }
        
        val avgSys = periodReadings.map { it.systolic }.average().toFloat()
        val avgDia = periodReadings.map { it.diastolic }.average().toFloat()
        val avgHr = periodReadings.map { it.heartRate }.average().toFloat()
        
        val stats = BPStats(
            avgSystolic = avgSys,
            avgDiastolic = avgDia,
            avgHeartRate = avgHr,
            minSystolic = periodReadings.minOf { it.systolic },
            maxSystolic = periodReadings.maxOf { it.systolic },
            minDiastolic = periodReadings.minOf { it.diastolic },
            maxDiastolic = periodReadings.maxOf { it.diastolic },
            minHeartRate = periodReadings.minOf { it.heartRate },
            maxHeartRate = periodReadings.maxOf { it.heartRate },
            readingCount = periodReadings.size,
            classification = BPClassification.classify(avgSys.toInt(), avgDia.toInt())
        )
        
        // Graph data: entries sorted by date ascending
        val graphData = periodEntries.sortedBy { it.date }
        
        _uiState.update {
            it.copy(
                graphData = graphData,
                stats = stats
            )
        }
    }
    
    fun refresh() {
        loadBloodPressureData()
    }
    
    class Factory(
        private val taskRepository: TaskRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BloodPressureViewModel(taskRepository) as T
        }
    }
}

