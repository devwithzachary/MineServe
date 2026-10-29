package com.devwithzachary.mineserve.ui.screens.detail.automation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.ui.theme.DiamondCyan
import com.devwithzachary.mineserve.ui.theme.EmeraldLight
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.ObsidianCardBorder
import com.devwithzachary.mineserve.ui.theme.RedstoneRed
import com.devwithzachary.mineserve.ui.theme.Slate400
import com.devwithzachary.mineserve.ui.theme.Slate900
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimeOfDaySelector(
    dailyHour: Int,
    dailyMinute: Int,
    onTimeSelected: (Int, Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = "Execution Time", color = Slate400, fontSize = 12.sp)

        // Time quick preset chips
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
                    label = { Text(label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldPrimary,
                        selectedLabelColor = Color.Black,
                        containerColor = Slate900,
                        labelColor = Color.White
                    )
                )
            }
        }

        // Stepper / manual adjustment row
        Surface(
            color = Slate900,
            shape = RoundedCornerShape(8.dp),
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
                    color = Color.White,
                    fontSize = 14.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            val prevHour = if (dailyHour > 0) dailyHour - 1 else 23
                            onTimeSelected(prevHour, dailyMinute)
                        }
                    ) {
                        Text("-1h", color = Slate400, fontSize = 12.sp)
                    }
                    TextButton(
                        onClick = {
                            val nextHour = (dailyHour + 1) % 24
                            onTimeSelected(nextHour, dailyMinute)
                        }
                    ) {
                        Text("+1h", color = EmeraldLight, fontSize = 12.sp)
                    }
                    TextButton(
                        onClick = {
                            val nextMin = (dailyMinute + 15) % 60
                            onTimeSelected(dailyHour, nextMin)
                        }
                    ) {
                        Text("+15m", color = DiamondCyan, fontSize = 12.sp)
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
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = "Repeat Every", color = Slate400, fontSize = 12.sp)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = intervalValueText,
                onValueChange = { onValueChange(it.filter { ch -> ch.isDigit() }) },
                label = { Text("Value") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = ObsidianCardBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.weight(1f)
            )

            FilterChip(
                selected = !intervalIsHours,
                onClick = { onUnitChange(false) },
                label = { Text("Minutes") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldPrimary,
                    selectedLabelColor = Color.Black,
                    containerColor = Slate900,
                    labelColor = Color.White
                )
            )

            FilterChip(
                selected = intervalIsHours,
                onClick = { onUnitChange(true) },
                label = { Text("Hours") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldPrimary,
                    selectedLabelColor = Color.Black,
                    containerColor = Slate900,
                    labelColor = Color.White
                )
            )
        }

        // Quick interval preset chips
        Text(text = "Quick Presets", color = Slate400, fontSize = 11.sp)
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
                    label = { Text(label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DiamondCyan,
                        selectedLabelColor = Color.Black,
                        containerColor = Slate900,
                        labelColor = Color.White
                    )
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
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = "Day Interval", color = Slate400, fontSize = 12.sp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = dayIntervalText,
                onValueChange = { onDayIntervalChange(it.filter { ch -> ch.isDigit() }) },
                label = { Text("Every N Days") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = ObsidianCardBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.weight(1f)
            )

            FilterChip(
                selected = dayIntervalText == "1",
                onClick = { onDayIntervalChange("1") },
                label = { Text("Daily (1)") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldPrimary,
                    selectedLabelColor = Color.Black,
                    containerColor = Slate900,
                    labelColor = Color.White
                )
            )

            FilterChip(
                selected = dayIntervalText == "2",
                onClick = { onDayIntervalChange("2") },
                label = { Text("Every 2d") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldPrimary,
                    selectedLabelColor = Color.Black,
                    containerColor = Slate900,
                    labelColor = Color.White
                )
            )
        }

        // Time of Day selector
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
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = "Days of Week", color = Slate400, fontSize = 12.sp)

        // Day of week chips
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
                    label = { Text(dayName, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldPrimary,
                        selectedLabelColor = Color.Black,
                        containerColor = Slate900,
                        labelColor = Color.White
                    )
                )
            }
        }

        // Day quick presets
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = selectedDays == setOf(Calendar.SATURDAY, Calendar.SUNDAY),
                onClick = { onDaysChange(setOf(Calendar.SATURDAY, Calendar.SUNDAY)) },
                label = { Text("Weekends", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = DiamondCyan,
                    selectedLabelColor = Color.Black,
                    containerColor = Slate900,
                    labelColor = Color.White
                )
            )
            FilterChip(
                selected = selectedDays == setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY),
                onClick = {
                    onDaysChange(setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY))
                },
                label = { Text("Weekdays", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = DiamondCyan,
                    selectedLabelColor = Color.Black,
                    containerColor = Slate900,
                    labelColor = Color.White
                )
            )
        }

        // Week interval
        Text(text = "Week Cadence", color = Slate400, fontSize = 12.sp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = weekIntervalText,
                onValueChange = { onWeekIntervalChange(it.filter { ch -> ch.isDigit() }) },
                label = { Text("Every N Weeks") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = ObsidianCardBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.weight(1f)
            )

            FilterChip(
                selected = weekIntervalText == "1",
                onClick = { onWeekIntervalChange("1") },
                label = { Text("Weekly (1)") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldPrimary,
                    selectedLabelColor = Color.Black,
                    containerColor = Slate900,
                    labelColor = Color.White
                )
            )

            FilterChip(
                selected = weekIntervalText == "2",
                onClick = { onWeekIntervalChange("2") },
                label = { Text("Bi-Weekly (2)") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldPrimary,
                    selectedLabelColor = Color.Black,
                    containerColor = Slate900,
                    labelColor = Color.White
                )
            )
        }

        // Time of Day selector
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Cron Expression", color = Slate400, fontSize = 12.sp)
        OutlinedTextField(
            value = cronExpressionText,
            onValueChange = onCronChange,
            label = { Text("5-Field Cron") },
            placeholder = { Text("0 4 * * *") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isCronValid) EmeraldPrimary else RedstoneRed,
                unfocusedBorderColor = if (isCronValid) ObsidianCardBorder else RedstoneRed.copy(alpha = 0.5f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Validation and format hint
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = if (isCronValid) "✓ Valid cron expression" else "⚠️ Invalid syntax: minute hour day month weekday",
                color = if (isCronValid) EmeraldLight else RedstoneRed,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Cron presets
        Text(text = "Common Cron Presets", color = Slate400, fontSize = 11.sp)
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
                    label = { Text(label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DiamondCyan,
                        selectedLabelColor = Color.Black,
                        containerColor = Slate900,
                        labelColor = Color.White
                    )
                )
            }
        }
    }
}

