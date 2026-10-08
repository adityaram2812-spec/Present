package com.adityaram.present.notifications

enum class UpcomingClassTiming(val label: String, val offsetMinutes: Int) {
    MIN_5("5m", 5),
    MIN_10("10m", 10),
    MIN_15("15m", 15),
    MIN_30("30m", 30),
    HOUR_1("1h", 60);

    companion object {
        fun fromLabel(label: String): UpcomingClassTiming = values().find { it.label == label } ?: MIN_15
    }
}

enum class AttendanceCheckTiming(val label: String, val offsetMinutes: Int, val isEvening: Boolean = false) {
    AT_END("At end", 0),
    MIN_10_AFTER("10m after", 10),
    MIN_20_AFTER("20m", 20),
    EVENING("Evening", 0, isEvening = true);

    companion object {
        fun fromLabel(label: String): AttendanceCheckTiming = values().find { it.label == label } ?: MIN_10_AFTER
    }
}
