package com.adityaram.present.domain.import

import java.time.DayOfWeek

object TimeNormalizer {
    private val timeRegex = "([0-1]?[0-9]|2[0-3])[:.]([0-5][0-9])\\s*(am|pm|a\\.m\\.|p\\.m\\.)?".toRegex(RegexOption.IGNORE_CASE)
    
    fun extractTimePairs(text: String): Pair<Int, Int>? {
        val matches = timeRegex.findAll(text).toList()
        if (matches.size >= 2) {
            val start = parseTimeMatched(matches[0])
            val end = parseTimeMatched(matches[1])
            if (start != null && end != null) return start to end
        }
        
        // Single times or dashed patterns like "9-10" without standard separators
        val simpleDashRegex = "(\\d+)\\s*-\\s*(\\d+)".toRegex()
        val match = simpleDashRegex.find(text)
        if (match != null) {
            val startHour = match.groupValues[1].toIntOrNull()
            val endHour = match.groupValues[2].toIntOrNull()
            if (startHour != null && endHour != null) {
                return (processHour(startHour) * 60) to (processHour(endHour) * 60)
            }
        }
        
        return null
    }

    private fun processHour(hour: Int): Int {
        if (hour in 1..7) return hour + 12 // Assumes pm for 1-7 times commonly found in timetables (e.g., 2 is 14:00)
        return hour
    }

    private fun parseTimeMatched(match: MatchResult): Int? {
        val groups = match.groupValues
        var hour = groups[1].toIntOrNull() ?: return null
        val min = groups[2].toIntOrNull() ?: return null
        val amPm = groups[3].lowercase().replace(".", "")

        if (amPm == "pm" && hour < 12) {
            hour += 12
        } else if (amPm == "am" && hour == 12) {
            hour = 0
        }
        
        return (hour * 60) + min
    }
}

object DayNormalizer {
    fun parseDay(text: String): DayOfWeek? {
        val normalized = text.trim().lowercase()
        return when {
            normalized.startsWith("mon") -> DayOfWeek.MONDAY
            normalized.startsWith("tue") -> DayOfWeek.TUESDAY
            normalized.startsWith("wed") -> DayOfWeek.WEDNESDAY
            normalized.startsWith("thu") -> DayOfWeek.THURSDAY
            normalized.startsWith("fri") -> DayOfWeek.FRIDAY
            normalized.startsWith("sat") -> DayOfWeek.SATURDAY
            normalized.startsWith("sun") -> DayOfWeek.SUNDAY
            else -> null
        }
    }
}
