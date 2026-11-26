package com.devwithzachary.mineserve.engine

import java.util.Calendar

/**
 * Lightweight 5-field cron expression parser and evaluator.
 * Fields: minute (0-59) hour (0-23) day-of-month (1-31) month (1-12) day-of-week (0-7, 0/7=Sun).
 * Supports *, lists (1,2,3), ranges (1-5), and steps (* /15, 1-5/2).
 */
class CronExpression private constructor(
    val rawExpression: String,
    private val minutePart: FieldMatcher,
    private val hourPart: FieldMatcher,
    private val dayOfMonthPart: FieldMatcher,
    private val monthPart: FieldMatcher,
    private val dayOfWeekPart: FieldMatcher
) {
    companion object {
        fun isValid(expression: String): Boolean {
            return parse(expression) != null
        }

        fun parse(expression: String): CronExpression? {
            val parts = expression.trim().split("\\s+".toRegex())
            if (parts.size != 5) return null

            val minute = FieldMatcher.parse(parts[0], 0, 59) ?: return null
            val hour = FieldMatcher.parse(parts[1], 0, 23) ?: return null
            val dom = FieldMatcher.parse(parts[2], 1, 31) ?: return null
            val month = FieldMatcher.parse(parts[3], 1, 12) ?: return null
            val dow = FieldMatcher.parse(parts[4], 0, 7) ?: return null

            return CronExpression(
                rawExpression = expression.trim(),
                minutePart = minute,
                hourPart = hour,
                dayOfMonthPart = dom,
                monthPart = month,
                dayOfWeekPart = dow
            )
        }
    }

    fun matches(cal: Calendar): Boolean {
        val minute = cal.get(Calendar.MINUTE)
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val dom = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH) + 1
        val calDow = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> 0
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            else -> 0
        }

        if (!minutePart.matches(minute)) return false
        if (!hourPart.matches(hour)) return false
        if (!monthPart.matches(month)) return false

        val domWildcard = dayOfMonthPart.isWildcard
        val dowWildcard = dayOfWeekPart.isWildcard

        val domMatch = dayOfMonthPart.matches(dom)
        val dowMatch = dayOfWeekPart.matches(calDow) || (calDow == 0 && dayOfWeekPart.matches(7))

        return when {
            domWildcard && dowWildcard -> true
            !domWildcard && dowWildcard -> domMatch
            domWildcard && !dowWildcard -> dowMatch
            else -> domMatch || dowMatch
        }
    }

    /**
     * Calculates the next timestamp (in millis) after referenceTimeInMillis that matches this cron expression.
     * Returns 0L if no match found within a 1-year horizon.
     */
    fun next(referenceTimeInMillis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = referenceTimeInMillis
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, 1)
        }

        val maxHorizon = Calendar.getInstance().apply {
            timeInMillis = referenceTimeInMillis
            add(Calendar.YEAR, 1)
        }

        while (cal.before(maxHorizon)) {
            val month = cal.get(Calendar.MONTH) + 1
            if (!monthPart.matches(month)) {
                cal.add(Calendar.MONTH, 1)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                continue
            }

            val dom = cal.get(Calendar.DAY_OF_MONTH)
            val calDow = when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SUNDAY -> 0
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4
                Calendar.FRIDAY -> 5
                Calendar.SATURDAY -> 6
                else -> 0
            }

            val domWildcard = dayOfMonthPart.isWildcard
            val dowWildcard = dayOfWeekPart.isWildcard
            val domMatch = dayOfMonthPart.matches(dom)
            val dowMatch = dayOfWeekPart.matches(calDow) || (calDow == 0 && dayOfWeekPart.matches(7))

            val dayMatches = when {
                domWildcard && dowWildcard -> true
                !domWildcard && dowWildcard -> domMatch
                domWildcard && !dowWildcard -> dowMatch
                else -> domMatch || dowMatch
            }

            if (!dayMatches) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                continue
            }

            val hour = cal.get(Calendar.HOUR_OF_DAY)
            if (!hourPart.matches(hour)) {
                cal.add(Calendar.HOUR_OF_DAY, 1)
                cal.set(Calendar.MINUTE, 0)
                continue
            }

            val minute = cal.get(Calendar.MINUTE)
            if (!minutePart.matches(minute)) {
                cal.add(Calendar.MINUTE, 1)
                continue
            }

            return cal.timeInMillis
        }

        return 0L
    }
}

class FieldMatcher(
    val isWildcard: Boolean,
    private val allowedValues: Set<Int>
) {
    fun matches(value: Int): Boolean {
        return isWildcard || allowedValues.contains(value)
    }

    companion object {
        fun parse(part: String, min: Int, max: Int): FieldMatcher? {
            if (part == "*") {
                return FieldMatcher(isWildcard = true, allowedValues = emptySet())
            }

            val values = mutableSetOf<Int>()
            val subParts = part.split(",")

            for (sub in subParts) {
                if (sub.isEmpty()) return null

                if (sub.startsWith("*/")) {
                    val stepStr = sub.removePrefix("*/")
                    val step = stepStr.toIntOrNull() ?: return null
                    if (step <= 0) return null
                    var curr = min
                    while (curr <= max) {
                        values.add(curr)
                        curr += step
                    }
                } else if (sub.contains("/")) {
                    val splitStep = sub.split("/")
                    if (splitStep.size != 2) return null
                    val rangeStr = splitStep[0]
                    val step = splitStep[1].toIntOrNull() ?: return null
                    if (step <= 0) return null

                    val (rangeStart, rangeEnd) = if (rangeStr == "*") {
                        Pair(min, max)
                    } else if (rangeStr.contains("-")) {
                        val bounds = rangeStr.split("-")
                        if (bounds.size != 2) return null
                        val s = bounds[0].toIntOrNull() ?: return null
                        val e = bounds[1].toIntOrNull() ?: return null
                        Pair(s, e)
                    } else {
                        val s = rangeStr.toIntOrNull() ?: return null
                        Pair(s, max)
                    }

                    if (rangeStart < min || rangeEnd > max || rangeStart > rangeEnd) return null
                    var curr = rangeStart
                    while (curr <= rangeEnd) {
                        values.add(curr)
                        curr += step
                    }
                } else if (sub.contains("-")) {
                    val bounds = sub.split("-")
                    if (bounds.size != 2) return null
                    val start = bounds[0].toIntOrNull() ?: return null
                    val end = bounds[1].toIntOrNull() ?: return null
                    if (start < min || end > max || start > end) return null
                    for (v in start..end) {
                        values.add(v)
                    }
                } else {
                    val single = sub.toIntOrNull() ?: return null
                    if (single < min || single > max) return null
                    values.add(single)
                }
            }

            return FieldMatcher(isWildcard = false, allowedValues = values)
        }
    }
}
