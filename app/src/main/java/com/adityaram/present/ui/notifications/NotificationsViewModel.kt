package com.adityaram.present.ui.notifications

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.adityaram.present.PresentApplication
import com.adityaram.present.data.UserPreferencesRepository
import com.adityaram.present.notifications.AlarmScheduler
import com.adityaram.present.notifications.NotificationHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificationsViewModel(
    private val appContext: Context,
    private val prefs: UserPreferencesRepository
) : ViewModel() {

    val notificationsEnabled = prefs.notificationsEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val upcomingClassEnabled = prefs.upcomingClassEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val upcomingClassTiming = prefs.upcomingClassTiming.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "15m")
    val attendanceCheckEnabled = prefs.attendanceCheckEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val attendanceCheckTiming = prefs.attendanceCheckTiming.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "10m after")
    val marginAlertsEnabled = prefs.marginAlertsEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val quietModeEnabled = prefs.quietModeEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val attendanceRequirement = prefs.attendanceRequirement.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 75)

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { 
            prefs.setNotificationsEnabled(enabled) 
            rescheduleAlarms()
        }
    }
    
    fun setUpcomingClassEnabled(enabled: Boolean) {
        viewModelScope.launch { 
            prefs.setUpcomingClassEnabled(enabled) 
            rescheduleAlarms()
        }
    }
    
    fun setUpcomingClassTiming(timing: String) {
        viewModelScope.launch { 
            prefs.setUpcomingClassTiming(timing) 
            rescheduleAlarms()
        }
    }
    
    fun setAttendanceCheckEnabled(enabled: Boolean) {
        viewModelScope.launch { 
            prefs.setAttendanceCheckEnabled(enabled) 
            rescheduleAlarms()
        }
    }
    
    fun setAttendanceCheckTiming(timing: String) {
        viewModelScope.launch { 
            prefs.setAttendanceCheckTiming(timing) 
            rescheduleAlarms()
        }
    }
    
    fun setMarginAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch { 
            prefs.setMarginAlertsEnabled(enabled) 
            rescheduleAlarms()
        }
    }
    
    fun setQuietModeEnabled(enabled: Boolean) {
        viewModelScope.launch { 
            prefs.setQuietModeEnabled(enabled) 
        }
    }

    fun sendTestNotification(context: Context) {
        NotificationHelper.setupChannel(context)
        NotificationHelper.sendNotification(context, "Test Notification", "This is a working local alert.")
    }

    fun openAppInfo(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }

    private suspend fun rescheduleAlarms() {
        AlarmScheduler.scheduleAlarms(appContext)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as PresentApplication)
                NotificationsViewModel(
                    appContext = application.applicationContext,
                    prefs = application.container.userPreferencesRepository
                )
            }
        }
    }
}
