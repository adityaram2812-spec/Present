package com.adityaram.present.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Timetable : Screen("timetable")
    object Subjects : Screen("subjects")
    object More : Screen("more")
    object Notifications : Screen("notifications")
    object Appearance : Screen("appearance")
    object Threshold : Screen("threshold")
    object Calendar : Screen("calendar")
    object Analytics : Screen("analytics")
    object Planner : Screen("planner")
    object LeavePlanner : Screen("leave_planner")
    object TemporaryLectures : Screen("temporary_lectures")
    object Import : Screen("import")
    object Account : Screen("account")
    object SignIn : Screen("sign_in")
    object CreateAccount : Screen("create_account")
    object ResetPassword : Screen("reset_password")
    
    object SubjectDetail : Screen("subject_detail/{subjectId}") {
        fun createRoute(subjectId: Long) = "subject_detail/$subjectId"
    }
    
    // About Flow
    object About : Screen("about")
    object Privacy : Screen("privacy")
    object Terms : Screen("terms")
    object Help : Screen("help")
    object Licenses : Screen("licenses")

    // Onboarding Flow
    object Welcome : Screen("welcome")
    object ChooseAccount : Screen("choose_account")
    object SetupTimetable : Screen("setup_timetable")
    object AddSubjects : Screen("add_subjects")
    object HistoricalAttendance : Screen("historical_attendance")
    object OpeningBalance : Screen("opening_balance")
}
