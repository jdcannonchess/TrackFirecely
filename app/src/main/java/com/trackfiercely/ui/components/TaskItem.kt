package com.trackfiercely.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.trackfiercely.data.model.BloodPressureData
import com.trackfiercely.data.model.BPClassification
import com.trackfiercely.data.model.TaskInputType
import com.trackfiercely.ui.theme.*
import com.trackfiercely.viewmodel.TaskWithStatus
import org.json.JSONObject

@Composable
fun TaskItem(
    taskWithStatus: TaskWithStatus,
    onToggleCompletion: (Long) -> Unit,
    onEditTask: (Long) -> Unit = {},
    onSetNumericValue: (Long, Float) -> Unit = { _, _ -> },
    onSetStarValue: (Long, Int) -> Unit = { _, _ -> },
    onCapturePhoto: (Long) -> Unit = {},
    onSetBpReading: (Long, Int, Int, Int) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val task = taskWithStatus.task

    when (task.inputType) {
        TaskInputType.CHECKBOX -> CheckboxTaskItem(
            taskWithStatus = taskWithStatus,
            onToggleCompletion = onToggleCompletion,
            onEditTask = onEditTask,
            modifier = modifier
        )
        TaskInputType.SLIDER -> SliderTaskItem(
            taskWithStatus = taskWithStatus,
            onValueChange = { onSetNumericValue(task.id, it) },
            onEditTask = onEditTask,
            modifier = modifier
        )
        TaskInputType.STARS -> StarsTaskItem(
            taskWithStatus = taskWithStatus,
            onValueChange = { onSetStarValue(task.id, it) },
            onEditTask = onEditTask,
            modifier = modifier
        )
        TaskInputType.NUMBER -> NumberTaskItem(
            taskWithStatus = taskWithStatus,
            onValueChange = { onSetNumericValue(task.id, it) },
            onEditTask = onEditTask,
            modifier = modifier
        )
        TaskInputType.PHOTO -> PhotoTaskItem(
            taskWithStatus = taskWithStatus,
            onCapturePhoto = { onCapturePhoto(task.id) },
            onEditTask = onEditTask,
            modifier = modifier
        )
        TaskInputType.BLOOD_PRESSURE -> BloodPressureTaskItem(
            taskWithStatus = taskWithStatus,
            onSetReading = { sys, dia, hr -> onSetBpReading(task.id, sys, dia, hr) },
            onEditTask = onEditTask,
            modifier = modifier
        )
    }
}

@Composable
private fun CheckboxTaskItem(
    taskWithStatus: TaskWithStatus,
    onToggleCompletion: (Long) -> Unit,
    onEditTask: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val task = taskWithStatus.task
    val isCompleted = taskWithStatus.isCompleted

    val checkScale by animateFloatAsState(
        targetValue = if (isCompleted) 1f else 0f,
        label = "checkScale"
    )

    val textAlpha by animateFloatAsState(
        targetValue = if (isCompleted) 0.5f else 1f,
        label = "textAlpha"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox - separate touch target
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable { onToggleCompletion(task.id) }
                    .background(
                        if (isCompleted) CompletedColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = Color.White,
                        modifier = Modifier
                            .size(16.dp)
                            .scale(checkScale)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Task name - clickable for edit
            Text(
                text = task.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha),
                textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onEditTask(task.id) }
            )

            // Edit icon
            IconButton(
                onClick = { onEditTask(task.id) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Edit task",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SliderTaskItem(
    taskWithStatus: TaskWithStatus,
    onValueChange: (Float) -> Unit,
    onEditTask: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val task = taskWithStatus.task
    val config = parseInputConfig(task.inputConfig)
    val currentValue = taskWithStatus.numericValue
    
    // Track if user has interacted with the slider
    var hasInteracted by remember(currentValue) { mutableStateOf(currentValue != null) }

    // Start at middle only for display purposes, but don't save until user interacts
    var sliderValue by remember(currentValue) {
        mutableFloatStateOf(currentValue ?: ((config.minValue + config.maxValue) / 2f))
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onEditTask(task.id) }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = if (hasInteracted) "${sliderValue.toInt()}" else "--",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (hasInteracted) EmeraldPrimary else MaterialTheme.colorScheme.outline
                )

                IconButton(
                    onClick = { onEditTask(task.id) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Slider(
                value = sliderValue,
                onValueChange = { 
                    sliderValue = it
                    hasInteracted = true
                },
                onValueChangeFinished = { onValueChange(sliderValue) },
                valueRange = config.minValue.toFloat()..config.maxValue.toFloat(),
                steps = (config.maxValue - config.minValue - 1).coerceAtLeast(0),
                colors = SliderDefaults.colors(
                    thumbColor = EmeraldPrimary,
                    activeTrackColor = EmeraldPrimary,
                    inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                modifier = Modifier.height(24.dp)
            )
        }
    }
}

@Composable
private fun StarsTaskItem(
    taskWithStatus: TaskWithStatus,
    onValueChange: (Int) -> Unit,
    onEditTask: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val task = taskWithStatus.task
    val config = parseInputConfig(task.inputConfig)
    // Start with 0 stars (all empty) if no value has been set
    val currentValue = taskWithStatus.starValue ?: 0

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = task.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onEditTask(task.id) }
            )

            Row(modifier = Modifier.padding(start = 4.dp)) {
                (1..config.starCount).forEach { star ->
                    Icon(
                        imageVector = if (star <= currentValue) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "$star stars",
                        tint = if (star <= currentValue) EmeraldPrimary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { onValueChange(star) }
                            .padding(2.dp)
                    )
                }
            }

            IconButton(
                onClick = { onEditTask(task.id) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun NumberTaskItem(
    taskWithStatus: TaskWithStatus,
    onValueChange: (Float) -> Unit,
    onEditTask: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val task = taskWithStatus.task
    val config = parseInputConfig(task.inputConfig)
    val currentValue = taskWithStatus.numericValue

    var textValue by remember(currentValue) {
        mutableStateOf(
            currentValue?.let {
                if (config.isInteger) it.toInt().toString() else it.toString()
            } ?: ""
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = task.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onEditTask(task.id) }
            )

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = textValue,
                onValueChange = { text ->
                    val regex = if (config.isInteger) Regex("^\\d*$") else Regex("^\\d*\\.?\\d*$")
                    if (text.isEmpty() || text.matches(regex)) {
                        textValue = text
                        text.toFloatOrNull()?.let { onValueChange(it) }
                    }
                },
                modifier = Modifier
                    .width(100.dp)
                    .heightIn(min = 48.dp),
                suffix = if (config.suffix.isNotEmpty()) {{ Text(config.suffix, style = MaterialTheme.typography.labelSmall) }} else null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (config.isInteger) KeyboardType.Number else KeyboardType.Decimal
                ),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
            )

            IconButton(
                onClick = { onEditTask(task.id) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun PhotoTaskItem(
    taskWithStatus: TaskWithStatus,
    onCapturePhoto: () -> Unit,
    onEditTask: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val task = taskWithStatus.task
    val photoUri = taskWithStatus.photoUri
    val isCompleted = taskWithStatus.isCompleted

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCapturePhoto() }, // Entire row triggers photo capture
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Camera icon on left
            Icon(
                imageVector = if (isCompleted) Icons.Default.Check else Icons.Default.CameraAlt,
                contentDescription = "Take photo",
                tint = if (isCompleted) CompletedColor else EmeraldPrimary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = task.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            if (photoUri != null) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = "Photo",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Edit icon - separate touch target
            IconButton(
                onClick = { onEditTask(task.id) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun BloodPressureTaskItem(
    taskWithStatus: TaskWithStatus,
    onSetReading: (Int, Int, Int) -> Unit,
    onEditTask: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val task = taskWithStatus.task
    val bpData = taskWithStatus.completion?.bpData?.let { 
        BloodPressureData.fromJson(it) 
    }
    val latestReading = bpData?.latestReading
    val isCompleted = taskWithStatus.isCompleted
    
    var showInputDialog by remember { mutableStateOf(false) }
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showInputDialog = true },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // BP icon with classification color
            val classification = latestReading?.classification
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Blood Pressure",
                tint = when (classification) {
                    BPClassification.NORMAL -> SageGreen
                    BPClassification.ELEVATED -> GoldenYellow
                    BPClassification.HIGH_STAGE_1 -> FallPrimary
                    BPClassification.HIGH_STAGE_2 -> HealthFitnessColor
                    BPClassification.CRISIS -> DangerRed
                    null -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                },
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Task name
            Text(
                text = task.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            
            // BP reading display
            if (latestReading != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${latestReading.systolic}/${latestReading.diastolic}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                    Text(
                        text = "HR: ${latestReading.heartRate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            // Edit icon
            IconButton(
                onClick = { onEditTask(task.id) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
    
    // Input dialog
    if (showInputDialog) {
        BloodPressureInputDialog(
            onDismiss = { showInputDialog = false },
            onSave = { sys, dia, hr ->
                onSetReading(sys, dia, hr)
                showInputDialog = false
            }
        )
    }
}

@Composable
private fun BloodPressureInputDialog(
    onDismiss: () -> Unit,
    onSave: (systolic: Int, diastolic: Int, heartRate: Int) -> Unit
) {
    var systolic by remember { mutableStateOf("") }
    var diastolic by remember { mutableStateOf("") }
    var heartRate by remember { mutableStateOf("") }
    
    val isValid = systolic.toIntOrNull() != null && 
                  diastolic.toIntOrNull() != null && 
                  heartRate.toIntOrNull() != null
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Log Blood Pressure",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Systolic
                OutlinedTextField(
                    value = systolic,
                    onValueChange = { if (it.all { c -> c.isDigit() }) systolic = it },
                    label = { Text("Systolic (top)") },
                    placeholder = { Text("120") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Diastolic
                OutlinedTextField(
                    value = diastolic,
                    onValueChange = { if (it.all { c -> c.isDigit() }) diastolic = it },
                    label = { Text("Diastolic (bottom)") },
                    placeholder = { Text("80") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Heart Rate
                OutlinedTextField(
                    value = heartRate,
                    onValueChange = { if (it.all { c -> c.isDigit() }) heartRate = it },
                    label = { Text("Heart Rate (BPM)") },
                    placeholder = { Text("72") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Classification preview
                val sys = systolic.toIntOrNull() ?: 0
                val dia = diastolic.toIntOrNull() ?: 0
                if (sys > 0 && dia > 0) {
                    val classification = BPClassification.classify(sys, dia)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (classification) {
                            BPClassification.NORMAL -> SageGreen
                            BPClassification.ELEVATED -> GoldenYellow
                            BPClassification.HIGH_STAGE_1 -> FallPrimary
                            BPClassification.HIGH_STAGE_2 -> HealthFitnessColor
                            BPClassification.CRISIS -> DangerRed
                        }.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = classification.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = when (classification) {
                                BPClassification.NORMAL -> SageGreen
                                BPClassification.ELEVATED -> GoldenYellow
                                BPClassification.HIGH_STAGE_1 -> FallPrimary
                                BPClassification.HIGH_STAGE_2 -> HealthFitnessColor
                                BPClassification.CRISIS -> DangerRed
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val sys = systolic.toIntOrNull() ?: return@TextButton
                    val dia = diastolic.toIntOrNull() ?: return@TextButton
                    val hr = heartRate.toIntOrNull() ?: return@TextButton
                    onSave(sys, dia, hr)
                },
                enabled = isValid
            ) {
                Text("Save", color = if (isValid) EmeraldPrimary else Color.Gray)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private data class InputConfigData(
    val minValue: Int = 1,
    val maxValue: Int = 10,
    val starCount: Int = 5,
    val suffix: String = "",
    val isInteger: Boolean = false
)

private fun parseInputConfig(json: String): InputConfigData {
    return try {
        val obj = JSONObject(json)
        InputConfigData(
            minValue = obj.optInt("minValue", 1),
            maxValue = obj.optInt("maxValue", 10),
            starCount = obj.optInt("starCount", 5),
            suffix = obj.optString("suffix", ""),
            isInteger = obj.optBoolean("isInteger", false)
        )
    } catch (e: Exception) {
        InputConfigData()
    }
}
