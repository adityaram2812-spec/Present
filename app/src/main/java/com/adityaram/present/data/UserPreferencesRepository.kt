package com.adityaram.present.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.stringPreferencesKey
import org.json.JSONObject

data class OpeningBalance(val present: Int, val absent: Int) {
    val total get() = present + absent
}

fun parseOpeningBalances(json: String): Map<Long, OpeningBalance> {
    val map = mutableMapOf<Long, OpeningBalance>()
    try {
        if (json.isNotEmpty() && json != "{}") {
            val obj = JSONObject(json)
            obj.keys().forEach { key ->
                val subjectObj = obj.getJSONObject(key)
                map[key.toLong()] = OpeningBalance(
                    present = subjectObj.optInt("present", 0),
                    absent = subjectObj.optInt("absent", 0)
                )
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return map
}

fun createOpeningBalancesJson(balances: Map<Long, OpeningBalance>): String {
    val obj = JSONObject()
    balances.forEach { (subjectId, balance) ->
        val subjectObj = JSONObject()
        subjectObj.put("present", balance.present)
        subjectObj.put("absent", balance.absent)
        obj.put(subjectId.toString(), subjectObj)
    }
    return obj.toString()
}

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {
    private val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
    private val THEME_MODE = stringPreferencesKey("theme_mode")
    private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    private val ATTENDANCE_REQ = intPreferencesKey("attendance_req")
    private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    private val UPCOMING_CLASS_ENABLED = booleanPreferencesKey("upcoming_class_enabled")
    private val UPCOMING_CLASS_TIMING = stringPreferencesKey("upcoming_class_timing")
    private val ATTENDANCE_CHECK_ENABLED = booleanPreferencesKey("attendance_check_enabled")
    private val ATTENDANCE_CHECK_TIMING = stringPreferencesKey("attendance_check_timing")
    private val MARGIN_ALERTS_ENABLED = booleanPreferencesKey("margin_alerts_enabled")
    private val QUIET_MODE_ENABLED = booleanPreferencesKey("quiet_mode_enabled")
    private val TRACKING_START_DATE = androidx.datastore.preferences.core.longPreferencesKey("tracking_start_date")
    private val OPENING_BALANCES = stringPreferencesKey("opening_balances")

    val isDarkMode: Flow<Boolean> = dataStore.data.map { it[IS_DARK_MODE] ?: true } // Default to dark mode
    val themeMode: Flow<String> = dataStore.data.map { 
        it[THEME_MODE] ?: if (it[IS_DARK_MODE] == false) "light" else "dark" 
    }
    val onboardingCompleted: Flow<Boolean> = dataStore.data.map { it[ONBOARDING_COMPLETED] ?: false }
    val attendanceRequirement: Flow<Int> = dataStore.data.map { it[ATTENDANCE_REQ] ?: 75 }
    
    val notificationsEnabled: Flow<Boolean> = dataStore.data.map { it[NOTIFICATIONS_ENABLED] ?: true }
    val upcomingClassEnabled: Flow<Boolean> = dataStore.data.map { it[UPCOMING_CLASS_ENABLED] ?: true }
    val upcomingClassTiming: Flow<String> = dataStore.data.map { it[UPCOMING_CLASS_TIMING] ?: "15m" }
    val attendanceCheckEnabled: Flow<Boolean> = dataStore.data.map { it[ATTENDANCE_CHECK_ENABLED] ?: true }
    val attendanceCheckTiming: Flow<String> = dataStore.data.map { it[ATTENDANCE_CHECK_TIMING] ?: "10m_after" }
    val marginAlertsEnabled: Flow<Boolean> = dataStore.data.map { it[MARGIN_ALERTS_ENABLED] ?: true }
    val quietModeEnabled: Flow<Boolean> = dataStore.data.map { it[QUIET_MODE_ENABLED] ?: false }
    val trackingStartDate: Flow<Long> = dataStore.data.map { it[TRACKING_START_DATE] ?: 0L }
    val openingBalances: Flow<String> = dataStore.data.map { it[OPENING_BALANCES] ?: "{}" }

    suspend fun setDarkMode(isDark: Boolean) {
        dataStore.edit { prefs -> prefs[IS_DARK_MODE] = isDark }
    }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { prefs -> 
            prefs[THEME_MODE] = mode
            if (mode == "dark") prefs[IS_DARK_MODE] = true 
            else if (mode == "light") prefs[IS_DARK_MODE] = false
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { prefs -> prefs[ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setAttendanceRequirement(req: Int) {
        dataStore.edit { prefs -> prefs[ATTENDANCE_REQ] = req }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) { dataStore.edit { prefs -> prefs[NOTIFICATIONS_ENABLED] = enabled } }
    suspend fun setUpcomingClassEnabled(enabled: Boolean) { dataStore.edit { prefs -> prefs[UPCOMING_CLASS_ENABLED] = enabled } }
    suspend fun setUpcomingClassTiming(timing: String) { dataStore.edit { prefs -> prefs[UPCOMING_CLASS_TIMING] = timing } }
    suspend fun setAttendanceCheckEnabled(enabled: Boolean) { dataStore.edit { prefs -> prefs[ATTENDANCE_CHECK_ENABLED] = enabled } }
    suspend fun setAttendanceCheckTiming(timing: String) { dataStore.edit { prefs -> prefs[ATTENDANCE_CHECK_TIMING] = timing } }
    suspend fun setMarginAlertsEnabled(enabled: Boolean) { dataStore.edit { prefs -> prefs[MARGIN_ALERTS_ENABLED] = enabled } }
    suspend fun setQuietModeEnabled(enabled: Boolean) { dataStore.edit { prefs -> prefs[QUIET_MODE_ENABLED] = enabled } }
    suspend fun setTrackingStartDate(dateMillis: Long) { dataStore.edit { prefs -> prefs[TRACKING_START_DATE] = dateMillis } }
    suspend fun setOpeningBalances(jsonMap: String) { dataStore.edit { prefs -> prefs[OPENING_BALANCES] = jsonMap } }
}
