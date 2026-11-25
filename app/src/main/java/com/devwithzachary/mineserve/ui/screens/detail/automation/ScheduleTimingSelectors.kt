package com.devwithzachary.mineserve.ui.screens.detail.automation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeMonoFontFamily
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimeOfDaySelector(
    dailyHour: Int,
    dailyMinute: Int,
    onTimeSelected: (Int, Int) -> Unit
) {
    val monoFont = LocalMineServeMonoFontFamily.current
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        labelColor = MaterialTheme.colorScheme.onSurface
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Execution Time",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val commonTimes = listOf(
                Pair(2, 0) to "02:00 AM",
                Pair(4, 0) to "04:00 AM",
                Pair(6, 0) to "06:00 AM",
                Pair(12, 0) to "12:00 PM",
                Pair(23, 0) to "11:00 PM"
            )
            commonTimes.forEach { (time, label) ->
                FilterChip(
                    selected = dailyHour == time.first && dailyMinute == time.second,
                    onClick = { onTimeSelected(time.first, time.second) },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall, fontFamily = monoFont) },
                    border = null,
                    colors = chipColors
                )
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = ShapeDefaults.Small,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = String.format(Locale.US, "Time: %02d:%02d", dailyHour, dailyMinute),
                    fontWeight = FontWeight.Bold,
                    fontFamily = monoFont,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            val prevHour = if (dailyHour > 0) dailyHour - 1 else 23
                            onTimeSelected(prevHour, dailyMinute)
                        }
                    ) {
                        Text("-1h", fontFamily = monoFont, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(
                        onClick = {
                            val nextHour = (dailyHour + 1) % 24
                            onTimeSelected(nextHour, dailyMinute)
                        }
                    ) {
                        Text("+1h", fontFamily = monoFont, color = MaterialTheme.colorScheme.primary)
                    }
                    TextButton(
                        onClick = {
                            val nextMin = (dailyMinute + 15) % 60
                            onTimeSelected(dailyHour, nextMin)
                        }
                    ) {
                        Text("+15m", fontFamily = monoFont, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IntervalScheduleSection(
    intervalValueText: String,
    intervalIsHours: Boolean,
    onValueChange: (String) -> Unit,
    onUnitChange: (Boolean) -> Unit
) {
    val monoFont = LocalMineServeMonoFontFamily.current
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        labelColor = MaterialTheme.colorScheme.onSurface
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Repeat Every",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = intervalValueText,
                onValueChange = { onValueChange(it.filter { ch -> ch.isDigit() }) },
                label = { Text("Value") },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            FilterChip(
                selected = !intervalIsHours,
                onClick = { onUnitChange(false) },
                label = { Text("Minutes") },
                border = null,
                colors = chipColors
            )

            FilterChip(
                selected = intervalIsHours,
                onClick = { onUnitChange(true) },
                label = { Text("Hours") },
                border = null,
                colors = chipColors
            )
        }

        Text(
            text = "Quick Presets",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val intervalPresets = listOf(
                "15m" to Pair("15", false),
                "30m" to Pair("30", false),
                "1h" to Pair("1", true),
                "2h" to Pair("2", true),
                "4h" to Pair("4", true),
                "6h" to Pair("6", true),
                "12h" to Pair("12", true),
                "24h" to Pair("24", true)
            )
            intervalPresets.forEach { (label, config) ->
                val isCurrent = intervalValueText == config.first && intervalIsHours == config.second
                FilterChip(
                    selected = isCurrent,
                    onClick = {
                        onValueChange(config.first)
                        onUnitChange(config.second)
                    },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall, fontFamily = monoFont) },
                    border = null,
                    colors = chipColors
                )
            }
        }
    }
}

@Composable
fun EveryXDaysScheduleSection(
    dayIntervalText: String,
    dailyHour: Int,
    dailyMinute: Int,
    onDayIntervalChange: (String) -> Unit,
    onTimeSelected: (Int, Int) -> Unit
) {
    val monoFont = LocalMineServeMonoFontFamily.current
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        labelColor = MaterialTheme.colorScheme.onSurface
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Day Interval",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = dayIntervalText,
                onValueChange = { onDayIntervalChange(it.filter { ch -> ch.isDigit() }) },
                label = { Text("Every N Days") },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            FilterChip(
                selected = dayIntervalText == "1",
                onClick = { onDayIntervalChange("1") },
                label = { Text("Daily (1)") },
                border = null,
                colors = chipColors
            )

            FilterChip(
                selected = dayIntervalText == "2",
                onClick = { onDayIntervalChange("2") },
                label = { Text("Every 2d") },
                border = null,
                colors = chipColors
            )
        }

        TimeOfDaySelector(
            dailyHour = dailyHour,
            dailyMinute = dailyMinute,
            onTimeSelected = onTimeSelected
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WeeklyDaysScheduleSection(
    selectedDays: Set<Int>,
    weekIntervalText: String,
    dailyHour: Int,
    dailyMinute: Int,
    onDaysChange: (Set<Int>) -> Unit,
    onWeekIntervalChange: (String) -> Unit,
    onTimeSelected: (Int, Int) -> Unit
) {
    val monoFont = LocalMineServeMonoFontFamily.current
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        labelColor = MaterialTheme.colorScheme.onSurface
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Days of Week",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val days = listOf(
            Calendar.SUNDAY to "Sun",
            Calendar.MONDAY to "Mon",
            Calendar.TUESDAY to "Tue",
            Calendar.WEDNESDAY to "Wed",
            Calendar.THURSDAY to "Thu",
            Calendar.FRIDAY to "Fri",
            Calendar.SATURDAY to "Sat"
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            days.forEach { (calDay, dayName) ->
                val isSelected = selectedDays.contains(calDay)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        val updated = if (isSelected) {
                            if (selectedDays.size > 1) selectedDays - calDay else selectedDays
                        } else {
                            selectedDays + calDay
                        }
                        onDaysChange(updated)
                    },
                    label = { Text(dayName, style = MaterialTheme.typography.labelSmall) },
                    border = null,
                    colors = chipColors
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = selectedDays == setOf(Calendar.SATURDAY, Calendar.SUNDAY),
                onClick = { onDaysChange(setOf(Calendar.SATURDAY, Calendar.SUNDAY)) },
                label = { Text("Weekends", style = MaterialTheme.typography.labelSmall) },
                border = null,
                colors = chipColors
            )
            FilterChip(
                selected = selectedDays == setOf(
                    Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY
                ),
                onClick = {
                    onDaysChange(
                        setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)
                    )
                },
                label = { Text("Weekdays", style = MaterialTheme.typography.labelSmall) },
                border = null,
                colors = chipColors
            )
        }

        Text(
            text = "Week Cadence",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = weekIntervalText,
                onValueChange = { onWeekIntervalChange(it.filter { ch -> ch.isDigit() }) },
                label = { Text("Every N Weeks") },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            FilterChip(
                selected = weekIntervalText == "1",
                onClick = { onWeekIntervalChange("1") },
                label = { Text("Weekly (1)") },
                border = null,
                colors = chipColors
            )

            FilterChip(
                selected = weekIntervalText == "2",
                onClick = { onWeekIntervalChange("2") },
                label = { Text("Bi-Weekly (2)") },
                border = null,
                colors = chipColors
            )
        }

        TimeOfDaySelector(
            dailyHour = dailyHour,
            dailyMinute = dailyMinute,
            onTimeSelected = onTimeSelected
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomCronScheduleSection(
    cronExpressionText: String,
    isCronValid: Boolean,
    onCronChange: (String) -> Unit
) {
    val monoFont = LocalMineServeMonoFontFamily.current
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
        selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        labelColor = MaterialTheme.colorScheme.onSurface
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Cron Expression",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = cronExpressionText,
            onValueChange = onCronChange,
            label = { Text("5-Field Cron") },
            placeholder = { Text("0 4 * * *", fontFamily = monoFont) },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
            isError = !isCronValid,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = if (isCronValid) "Valid cron expression" else "Invalid syntax: minute hour day month weekday",
            color = if (isCronValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )

        Text(
            text = "Common Cron Presets",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val cronPresets = listOf(
                "Every 30m" to "*/30 * * * *",
                "Nightly 4 AM" to "0 4 * * *",
                "Weekly Sun 4 AM" to "0 4 * * 0",
                "Twice Daily" to "0 4,16 * * *",
                "Weekdays 2 AM" to "0 2 * * 1-5"
            )
            cronPresets.forEach { (label, expr) ->
                FilterChip(
                    selected = cronExpressionText == expr,
                    onClick = { onCronChange(expr) },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    border = null,
                    colors = chipColors
                )
            }
        }
    }
}
