package com.trackfiercely.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.trackfiercely.data.model.Category
import com.trackfiercely.data.model.TaskInputType
import com.trackfiercely.data.model.ScheduleType
import com.trackfiercely.ui.theme.EmeraldPrimary
import com.trackfiercely.ui.theme.getCategoryColor
import com.trackfiercely.util.DateUtils
import com.trackfiercely.viewmodel.CreateTaskViewModel
import com.trackfiercely.viewmodel.MonthlyScheduleMode
import com.trackfiercely.viewmodel.YearlyScheduleMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskScreen(
    viewModel: CreateTaskViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = DateUtils.toUtcEpochMillis(uiState.scheduledDate)
    )

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onSaved()
        }
    }

    // Date picker dialog
    if (uiState.showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { viewModel.hideDatePicker() },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            viewModel.selectDate(DateUtils.fromUtcEpochMillis(millis))
                        }
                    }
                ) {
                    Text("Select", color = EmeraldPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDatePicker() }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = EmeraldPrimary,
                    todayDateBorderColor = EmeraldPrimary
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveTask() },
                        enabled = uiState.isValid && !uiState.isLoading
                    ) {
                        Text(
                            text = "Save",
                            color = if (uiState.isValid) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading && uiState.isEditMode) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = EmeraldPrimary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Task name input
                OutlinedTextField(
                    value = uiState.taskName,
                    onValueChange = { viewModel.updateName(it) },
                    label = { Text("Task name") },
                    placeholder = { Text("e.g., Morning workout") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = uiState.errorMessage != null && uiState.taskName.isBlank(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        cursorColor = EmeraldPrimary
                    )
                )

                // Error message
                AnimatedVisibility(visible = uiState.errorMessage != null) {
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Row with Category and Schedule dropdowns
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CategoryDropdown(
                        selectedCategory = uiState.selectedCategory,
                        onCategorySelected = { viewModel.selectCategory(it) },
                        modifier = Modifier.weight(1f)
                    )

                    ScheduleTypeDropdown(
                        selectedType = uiState.scheduleType,
                        onTypeSelected = { viewModel.setScheduleType(it) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Input type dropdown (only show if can edit and not checkbox)
                if (uiState.canEditInputType) {
                    InputTypeDropdown(
                        selectedType = uiState.inputType,
                        onTypeSelected = { viewModel.selectInputType(it) }
                    )
                }

                // Input configuration (based on type) - compact version
                AnimatedVisibility(
                    visible = uiState.inputType != TaskInputType.CHECKBOX && uiState.canEditInputType,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    InputConfigSectionCompact(
                        inputType = uiState.inputType,
                        sliderMin = uiState.sliderMin,
                        sliderMax = uiState.sliderMax,
                        starCount = uiState.starCount,
                        numberSuffix = uiState.numberSuffix,
                        numberIsInteger = uiState.numberIsInteger,
                        photoTimerSeconds = uiState.photoTimerSeconds,
                        onSliderRangeChange = { min, max -> viewModel.setSliderRange(min, max) },
                        onStarCountChange = { viewModel.setStarCount(it) },
                        onNumberConfigChange = { suffix, isInt -> viewModel.setNumberConfig(suffix, isInt) },
                        onPhotoTimerChange = { viewModel.setPhotoTimerSeconds(it) }
                    )
                }

                // Schedule-specific options
                when (uiState.scheduleType) {
                    ScheduleType.WEEKLY -> {
                        DaySelectorCompact(
                            selectedDays = uiState.selectedDays,
                            onDayToggle = { viewModel.toggleDay(it) },
                            onSelectAll = { viewModel.selectAllDays() },
                            onSelectWeekdays = { viewModel.selectWeekdays() },
                            onSelectWeekends = { viewModel.selectWeekends() }
                        )
                    }
                    ScheduleType.MONTHLY -> {
                        MonthlyScheduleSectionCompact(
                            scheduleMode = uiState.monthlyScheduleMode,
                            monthlyDay = uiState.monthlyDay,
                            monthlyWeek = uiState.monthlyWeek,
                            monthlyDayOfWeek = uiState.monthlyDayOfWeek,
                            onModeChange = { viewModel.setMonthlyScheduleMode(it) },
                            onDayChange = { viewModel.setMonthlyDay(it) },
                            onWeekChange = { viewModel.setMonthlyWeek(it) },
                            onDayOfWeekChange = { viewModel.setMonthlyDayOfWeek(it) }
                        )
                    }
                    ScheduleType.YEARLY -> {
                        YearlyScheduleSectionCompact(
                            yearlyMonth = uiState.yearlyMonth,
                            scheduleMode = uiState.yearlyScheduleMode,
                            yearlyDay = uiState.yearlyDay,
                            yearlyWeek = uiState.yearlyWeek,
                            yearlyDayOfWeek = uiState.yearlyDayOfWeek,
                            onMonthChange = { viewModel.setYearlyMonth(it) },
                            onModeChange = { viewModel.setYearlyScheduleMode(it) },
                            onDayChange = { viewModel.setYearlyDay(it) },
                            onWeekChange = { viewModel.setYearlyWeek(it) },
                            onDayOfWeekChange = { viewModel.setYearlyDayOfWeek(it) }
                        )
                    }
                    ScheduleType.ONE_TIME -> {
                        DateSelectorCompact(
                            selectedDate = uiState.scheduledDate,
                            autoRollover = uiState.autoRollover,
                            onSelectToday = { viewModel.selectToday() },
                            onSelectTomorrow = { viewModel.selectTomorrow() },
                            onOpenDatePicker = { viewModel.showDatePicker() },
                            onAutoRolloverChange = { viewModel.setAutoRollover(it) }
                        )
                    }
                }

                // Time dropdown
                TimeDropdown(
                    selectedHour = uiState.scheduledHour,
                    onHourSelected = { viewModel.setScheduledHour(it) },
                    onClear = { viewModel.clearScheduledHour() }
                )

                // Hide task toggle (only in edit mode)
                if (uiState.isEditMode) {
                    HideTaskToggle(
                        isHidden = uiState.isHidden,
                        onToggle = { viewModel.setTaskHidden(it) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    selectedCategory: Category,
    onCategorySelected: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val categoryColor = getCategoryColor(selectedCategory)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedCategory.displayName.split(" ").first(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            leadingIcon = {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(categoryColor, CircleShape)
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = categoryColor)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Category.entries.forEach { category ->
                val color = getCategoryColor(category)
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(category.displayName)
                        }
                    },
                    onClick = { onCategorySelected(category); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleTypeDropdown(
    selectedType: ScheduleType,
    onTypeSelected: (ScheduleType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val displayText = when (selectedType) {
        ScheduleType.WEEKLY -> "Weekly"
        ScheduleType.MONTHLY -> "Monthly"
        ScheduleType.YEARLY -> "Yearly"
        ScheduleType.ONE_TIME -> "One-time"
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text("Schedule") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ScheduleType.entries.forEach { type ->
                val text = when (type) {
                    ScheduleType.WEEKLY -> "Weekly"
                    ScheduleType.MONTHLY -> "Monthly"
                    ScheduleType.YEARLY -> "Yearly"
                    ScheduleType.ONE_TIME -> "One-time"
                }
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = { onTypeSelected(type); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InputTypeDropdown(
    selectedType: TaskInputType,
    onTypeSelected: (TaskInputType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val displayText = when (selectedType) {
        TaskInputType.CHECKBOX -> "Checkbox"
        TaskInputType.SLIDER -> "Slider"
        TaskInputType.STARS -> "Star rating"
        TaskInputType.NUMBER -> "Number input"
        TaskInputType.PHOTO -> "Photo capture"
        TaskInputType.BLOOD_PRESSURE -> "Blood pressure"
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text("Input type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TaskInputType.entries.forEach { type ->
                val text = when (type) {
                    TaskInputType.CHECKBOX -> "Checkbox"
                    TaskInputType.SLIDER -> "Slider"
                    TaskInputType.STARS -> "Star rating"
                    TaskInputType.NUMBER -> "Number input"
                    TaskInputType.PHOTO -> "Photo capture"
                    TaskInputType.BLOOD_PRESSURE -> "Blood pressure"
                }
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = { onTypeSelected(type); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun InputConfigSectionCompact(
    inputType: TaskInputType,
    sliderMin: Int,
    sliderMax: Int,
    starCount: Int,
    numberSuffix: String,
    numberIsInteger: Boolean,
    photoTimerSeconds: Int,
    onSliderRangeChange: (Int, Int) -> Unit,
    onStarCountChange: (Int) -> Unit,
    onNumberConfigChange: (String, Boolean) -> Unit,
    onPhotoTimerChange: (Int) -> Unit
) {
    when (inputType) {
        TaskInputType.SLIDER -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = sliderMin.toString(),
                    onValueChange = { it.toIntOrNull()?.let { min -> onSliderRangeChange(min, sliderMax) } },
                    label = { Text("Min") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = sliderMax.toString(),
                    onValueChange = { it.toIntOrNull()?.let { max -> onSliderRangeChange(sliderMin, max) } },
                    label = { Text("Max") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }
        TaskInputType.STARS -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Stars:", style = MaterialTheme.typography.bodyMedium)
                (3..5).forEach { count ->
                    FilterChip(
                        selected = starCount == count,
                        onClick = { onStarCountChange(count) },
                        label = { Text("$count") }
                    )
                }
            }
        }
        TaskInputType.NUMBER -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = numberSuffix,
                    onValueChange = { onNumberConfigChange(it, numberIsInteger) },
                    label = { Text("Unit (e.g., lbs)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = numberIsInteger,
                        onCheckedChange = { onNumberConfigChange(numberSuffix, it) }
                    )
                    Text("Integer", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        TaskInputType.PHOTO -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Timer:", style = MaterialTheme.typography.bodyMedium)
                listOf(0 to "Off", 3 to "3s", 5 to "5s", 10 to "10s").forEach { (seconds, label) ->
                    FilterChip(
                        selected = photoTimerSeconds == seconds,
                        onClick = { onPhotoTimerChange(seconds) },
                        label = { Text(label) }
                    )
                }
            }
        }
        else -> {}
    }
}

@Composable
private fun DaySelectorCompact(
    selectedDays: Set<DayOfWeek>,
    onDayToggle: (DayOfWeek) -> Unit,
    onSelectAll: () -> Unit,
    onSelectWeekdays: () -> Unit,
    onSelectWeekends: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Repeat on",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onSelectAll, contentPadding = PaddingValues(4.dp)) {
                    Text("All", style = MaterialTheme.typography.labelSmall)
                }
                TextButton(onClick = onSelectWeekdays, contentPadding = PaddingValues(4.dp)) {
                    Text("Wkdays", style = MaterialTheme.typography.labelSmall)
                }
                TextButton(onClick = onSelectWeekends, contentPadding = PaddingValues(4.dp)) {
                    Text("Wkends", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            DayOfWeek.entries.forEach { day ->
                val isSelected = selectedDays.contains(day)
                Surface(
                    modifier = Modifier.size(36.dp).clip(CircleShape).clickable { onDayToggle(day) },
                    color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = CircleShape
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = day.name.take(1),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (selectedDays.isEmpty()) {
            Text(
                text = "No days selected - task won't appear in daily view",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthlyScheduleSectionCompact(
    scheduleMode: MonthlyScheduleMode,
    monthlyDay: Int,
    monthlyWeek: Int,
    monthlyDayOfWeek: DayOfWeek,
    onModeChange: (MonthlyScheduleMode) -> Unit,
    onDayChange: (Int) -> Unit,
    onWeekChange: (Int) -> Unit,
    onDayOfWeekChange: (DayOfWeek) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = scheduleMode == MonthlyScheduleMode.DAY_OF_MONTH,
                onClick = { onModeChange(MonthlyScheduleMode.DAY_OF_MONTH) },
                label = { Text("Day X") }
            )
            FilterChip(
                selected = scheduleMode == MonthlyScheduleMode.NTH_WEEKDAY,
                onClick = { onModeChange(MonthlyScheduleMode.NTH_WEEKDAY) },
                label = { Text("Nth weekday") }
            )
        }

        if (scheduleMode == MonthlyScheduleMode.DAY_OF_MONTH) {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = "Day $monthlyDay",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Day of month") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    (1..31).forEach { day ->
                        DropdownMenuItem(text = { Text("Day $day") }, onClick = { onDayChange(day); expanded = false })
                    }
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                var weekExp by remember { mutableStateOf(false) }
                var dayExp by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(expanded = weekExp, onExpandedChange = { weekExp = it }, modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = getOrdinal(monthlyWeek),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Week") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(weekExp) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = weekExp, onDismissRequest = { weekExp = false }) {
                        (1..5).forEach { week ->
                            DropdownMenuItem(text = { Text(getOrdinal(week)) }, onClick = { onWeekChange(week); weekExp = false })
                        }
                    }
                }

                ExposedDropdownMenuBox(expanded = dayExp, onExpandedChange = { dayExp = it }, modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = monthlyDayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Day") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(dayExp) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = dayExp, onDismissRequest = { dayExp = false }) {
                        DayOfWeek.entries.forEach { day ->
                            DropdownMenuItem(text = { Text(day.name.lowercase().replaceFirstChar { it.uppercase() }) }, onClick = { onDayOfWeekChange(day); dayExp = false })
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YearlyScheduleSectionCompact(
    yearlyMonth: Month,
    scheduleMode: YearlyScheduleMode,
    yearlyDay: Int,
    yearlyWeek: Int,
    yearlyDayOfWeek: DayOfWeek,
    onMonthChange: (Month) -> Unit,
    onModeChange: (YearlyScheduleMode) -> Unit,
    onDayChange: (Int) -> Unit,
    onWeekChange: (Int) -> Unit,
    onDayOfWeekChange: (DayOfWeek) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        var monthExp by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = monthExp, onExpandedChange = { monthExp = it }) {
            OutlinedTextField(
                value = yearlyMonth.name.lowercase().replaceFirstChar { it.uppercase() },
                onValueChange = {},
                readOnly = true,
                label = { Text("Month") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(monthExp) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = monthExp, onDismissRequest = { monthExp = false }) {
                Month.entries.forEach { month ->
                    DropdownMenuItem(text = { Text(month.name.lowercase().replaceFirstChar { it.uppercase() }) }, onClick = { onMonthChange(month); monthExp = false })
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = scheduleMode == YearlyScheduleMode.SPECIFIC_DATE,
                onClick = { onModeChange(YearlyScheduleMode.SPECIFIC_DATE) },
                label = { Text("Day X") }
            )
            FilterChip(
                selected = scheduleMode == YearlyScheduleMode.NTH_WEEKDAY,
                onClick = { onModeChange(YearlyScheduleMode.NTH_WEEKDAY) },
                label = { Text("Nth weekday") }
            )
        }

        if (scheduleMode == YearlyScheduleMode.SPECIFIC_DATE) {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = "Day $yearlyDay",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Day of month") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    (1..31).forEach { day ->
                        DropdownMenuItem(text = { Text("Day $day") }, onClick = { onDayChange(day); expanded = false })
                    }
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                var weekExp by remember { mutableStateOf(false) }
                var dayExp by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(expanded = weekExp, onExpandedChange = { weekExp = it }, modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = getOrdinal(yearlyWeek),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Week") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(weekExp) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = weekExp, onDismissRequest = { weekExp = false }) {
                        (1..5).forEach { week ->
                            DropdownMenuItem(text = { Text(getOrdinal(week)) }, onClick = { onWeekChange(week); weekExp = false })
                        }
                    }
                }

                ExposedDropdownMenuBox(expanded = dayExp, onExpandedChange = { dayExp = it }, modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = yearlyDayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Day") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(dayExp) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = dayExp, onDismissRequest = { dayExp = false }) {
                        DayOfWeek.entries.forEach { day ->
                            DropdownMenuItem(text = { Text(day.name.lowercase().replaceFirstChar { it.uppercase() }) }, onClick = { onDayOfWeekChange(day); dayExp = false })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateSelectorCompact(
    selectedDate: LocalDate,
    autoRollover: Boolean,
    onSelectToday: () -> Unit,
    onSelectTomorrow: () -> Unit,
    onOpenDatePicker: () -> Unit,
    onAutoRolloverChange: (Boolean) -> Unit
) {
    val today = LocalDate.now()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Date display that opens picker
        Surface(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenDatePicker),
            shape = RoundedCornerShape(8.dp),
            color = EmeraldPrimary.copy(alpha = 0.1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = DateUtils.formatFull(selectedDate),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = EmeraldPrimary,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.Edit, contentDescription = "Change", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            }
        }

        // Quick select chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedDate == today,
                onClick = onSelectToday,
                label = { Text("Today") }
            )
            FilterChip(
                selected = selectedDate == today.plusDays(1),
                onClick = onSelectTomorrow,
                label = { Text("Tomorrow") }
            )
        }

        // Auto-rollover toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Autorenew,
                contentDescription = null,
                tint = if (autoRollover) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Auto-rollover", style = MaterialTheme.typography.bodyMedium)
                Text("Move to next day if not done", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = autoRollover,
                onCheckedChange = onAutoRolloverChange,
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = EmeraldPrimary)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDropdown(
    selectedHour: Int?,
    onHourSelected: (Int) -> Unit,
    onClear: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val displayText = selectedHour?.let { hour ->
        when {
            hour == 0 -> "12 AM"
            hour < 12 -> "$hour AM"
            hour == 12 -> "12 PM"
            else -> "${hour - 12} PM"
        }
    } ?: "No time (first in list)"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text("Time (for ordering)") },
            trailingIcon = {
                Row {
                    if (selectedHour != null) {
                        IconButton(onClick = { onClear() }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                }
            },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("No time (first in list)") },
                onClick = { onClear(); expanded = false }
            )
            HorizontalDivider()
            // Morning 6-11
            DropdownMenuItem(text = { Text("Morning", fontWeight = FontWeight.Bold) }, onClick = {}, enabled = false)
            (6..11).forEach { hour ->
                val text = "$hour AM"
                DropdownMenuItem(
                    text = { Text("  $text") },
                    onClick = { onHourSelected(hour); expanded = false }
                )
            }
            // Afternoon 12-17
            DropdownMenuItem(text = { Text("Afternoon", fontWeight = FontWeight.Bold) }, onClick = {}, enabled = false)
            (12..17).forEach { hour ->
                val text = if (hour == 12) "12 PM" else "${hour - 12} PM"
                DropdownMenuItem(
                    text = { Text("  $text") },
                    onClick = { onHourSelected(hour); expanded = false }
                )
            }
            // Evening 18-23
            DropdownMenuItem(text = { Text("Evening", fontWeight = FontWeight.Bold) }, onClick = {}, enabled = false)
            (18..23).forEach { hour ->
                val text = "${hour - 12} PM"
                DropdownMenuItem(
                    text = { Text("  $text") },
                    onClick = { onHourSelected(hour); expanded = false }
                )
            }
            // Night 0-5
            DropdownMenuItem(text = { Text("Night", fontWeight = FontWeight.Bold) }, onClick = {}, enabled = false)
            (0..5).forEach { hour ->
                val text = if (hour == 0) "12 AM" else "$hour AM"
                DropdownMenuItem(
                    text = { Text("  $text") },
                    onClick = { onHourSelected(hour); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun HideTaskToggle(
    isHidden: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = if (isHidden) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = null,
                tint = if (isHidden) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Hide this task",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Won't appear in daily view",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isHidden,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.error
                )
            )
        }
    }
}

private fun getOrdinal(n: Int): String {
    return when (n) {
        1 -> "1st"
        2 -> "2nd"
        3 -> "3rd"
        else -> "${n}th"
    }
}
