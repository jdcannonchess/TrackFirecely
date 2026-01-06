package com.trackfiercely.data.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * Blood pressure classification categories based on AHA guidelines
 */
enum class BPClassification(val label: String, val colorName: String) {
    NORMAL("Normal", "green"),
    ELEVATED("Elevated", "yellow"),
    HIGH_STAGE_1("High Stage 1", "orange"),
    HIGH_STAGE_2("High Stage 2", "red"),
    CRISIS("Hypertensive Crisis", "darkred");
    
    companion object {
        fun classify(systolic: Int, diastolic: Int): BPClassification {
            return when {
                systolic > 180 || diastolic > 120 -> CRISIS
                systolic >= 140 || diastolic >= 90 -> HIGH_STAGE_2
                systolic >= 130 || diastolic >= 80 -> HIGH_STAGE_1
                systolic >= 120 && diastolic < 80 -> ELEVATED
                else -> NORMAL
            }
        }
    }
}

/**
 * Represents a single blood pressure reading
 */
data class BloodPressureReading(
    val systolic: Int,      // Top number (90-180+)
    val diastolic: Int,     // Bottom number (60-120+)
    val heartRate: Int,     // BPM (40-200)
    val timestamp: Long = System.currentTimeMillis()
) {
    val classification: BPClassification
        get() = BPClassification.classify(systolic, diastolic)
    
    val displayString: String
        get() = "$systolic/$diastolic"
    
    val fullDisplayString: String
        get() = "$systolic/$diastolic  HR: $heartRate"
    
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("systolic", systolic)
            put("diastolic", diastolic)
            put("heartRate", heartRate)
            put("timestamp", timestamp)
        }
    }
    
    companion object {
        fun fromJson(json: JSONObject): BloodPressureReading {
            return BloodPressureReading(
                systolic = json.optInt("systolic", 0),
                diastolic = json.optInt("diastolic", 0),
                heartRate = json.optInt("heartRate", 0),
                timestamp = json.optLong("timestamp", System.currentTimeMillis())
            )
        }
    }
}

/**
 * Container for blood pressure data stored in TaskCompletion
 * Supports multiple readings per day
 */
data class BloodPressureData(
    val readings: List<BloodPressureReading> = emptyList()
) {
    val latestReading: BloodPressureReading?
        get() = readings.maxByOrNull { it.timestamp }
    
    val averageSystolic: Float?
        get() = if (readings.isNotEmpty()) readings.map { it.systolic }.average().toFloat() else null
    
    val averageDiastolic: Float?
        get() = if (readings.isNotEmpty()) readings.map { it.diastolic }.average().toFloat() else null
    
    val averageHeartRate: Float?
        get() = if (readings.isNotEmpty()) readings.map { it.heartRate }.average().toFloat() else null
    
    val averageClassification: BPClassification?
        get() {
            val avgSys = averageSystolic?.toInt() ?: return null
            val avgDia = averageDiastolic?.toInt() ?: return null
            return BPClassification.classify(avgSys, avgDia)
        }
    
    fun toJson(): String {
        val obj = JSONObject()
        val readingsArray = JSONArray()
        readings.forEach { reading ->
            readingsArray.put(reading.toJson())
        }
        obj.put("readings", readingsArray)
        return obj.toString()
    }
    
    fun addReading(reading: BloodPressureReading): BloodPressureData {
        return copy(readings = readings + reading)
    }
    
    companion object {
        fun fromJson(json: String): BloodPressureData {
            return try {
                val obj = JSONObject(json)
                val readingsArray = obj.optJSONArray("readings") ?: JSONArray()
                val readings = (0 until readingsArray.length()).map { index ->
                    BloodPressureReading.fromJson(readingsArray.getJSONObject(index))
                }
                BloodPressureData(readings)
            } catch (e: Exception) {
                BloodPressureData()
            }
        }
        
        fun empty() = BloodPressureData()
    }
}

