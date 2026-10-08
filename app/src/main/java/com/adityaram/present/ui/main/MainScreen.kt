package com.adityaram.present.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.material3.Scaffold
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.adityaram.present.ui.components.PresentBottomNavigation
import com.adityaram.present.ui.home.HomeScreen
import com.adityaram.present.ui.more.MoreScreen
import com.adityaram.present.ui.navigation.Screen
import com.adityaram.present.ui.subjects.SubjectsScreen
import com.adityaram.present.ui.theme.LocalPresentColors
import com.adityaram.present.ui.timetable.TimetableScreen
import com.adityaram.present.ui.importing.ImportTimetableScreen
import com.adityaram.present.ui.importing.ImportViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.adityaram.present.ui.subjects.SubjectDetailScreen
import androidx.compose.foundation.rememberScrollState
import com.adityaram.present.ui.notifications.NotificationsScreen

import androidx.compose.ui.platform.LocalContext
import com.adityaram.present.PresentApplication
@Composable
fun MainScreen(startDestination: String = Screen.Home.route) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val colors = LocalPresentColors.current

    val context = LocalContext.current
    val appContainer = (context.applicationContext as PresentApplication).container
    val onboardingCompleted by appContainer.userPreferencesRepository.onboardingCompleted.collectAsState(initial = true)

    val homeListState = rememberLazyListState()
    val timetableListState = rememberLazyListState()
    val subjectsListState = rememberLazyListState()
    val moreScrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = colors.background,
        bottomBar = {
            val isTopLevel = currentRoute in listOf(
                Screen.Home.route,
                Screen.Timetable.route,
                Screen.Subjects.route,
                Screen.More.route,
                Screen.Privacy.route,
                Screen.Terms.route,
                Screen.Help.route,
                Screen.Licenses.route
            )
            if (isTopLevel) {
                PresentBottomNavigation(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        if (route == currentRoute) {
                            scope.launch {
                                when(route) {
                                    Screen.Home.route -> homeListState.animateScrollToItem(0)
                                    Screen.Timetable.route -> timetableListState.animateScrollToItem(0)
                                    Screen.Subjects.route -> subjectsListState.animateScrollToItem(0)
                                    Screen.More.route -> moreScrollState.animateScrollTo(0)
                                }
                            }
                        } else {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(
                    animationSpec = tween(
                        durationMillis = 220,
                        delayMillis = 90,
                        easing = LinearOutSlowInEasing
                    )
                ) + slideInVertically(
                    animationSpec = tween(
                        durationMillis = 220,
                        delayMillis = 90,
                        easing = LinearOutSlowInEasing
                    ),
                    initialOffsetY = { 30 } // Subtle ~10dp shift natively
                )
            },
            exitTransition = {
                fadeOut(
                    animationSpec = tween(
                        durationMillis = 90,
                        easing = FastOutLinearInEasing
                    )
                )
            }
        ) {
            composable(Screen.Home.route) { 
                HomeScreen(
                    listState = homeListState,
                    onNavigateToTimetable = {
                        navController.navigate(Screen.Timetable.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                ) 
            }
            composable(Screen.Welcome.route) {
                com.adityaram.present.ui.onboarding.WelcomeScreen(
                    onNavigateNext = {
                        navController.navigate(Screen.ChooseAccount.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.ChooseAccount.route) {
                val authViewModel: com.adityaram.present.ui.auth.AuthViewModel = viewModel(factory = com.adityaram.present.ui.auth.AuthViewModel.Factory)
                com.adityaram.present.ui.onboarding.ChooseAccountModeScreen(
                    authViewModel = authViewModel,
                    onNavigateToSignIn = { navController.navigate(Screen.SignIn.route) },
                    onNavigateNext = {
                        navController.navigate(Screen.SetupTimetable.route) {
                            popUpTo(Screen.ChooseAccount.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.SetupTimetable.route) {
                com.adityaram.present.ui.onboarding.SetupTimetableScreen(
                    onNavigateToImport = { navController.navigate(Screen.Import.route) },
                    onNavigateToManual = {
                        navController.navigate(Screen.AddSubjects.route) {
                            popUpTo(Screen.SetupTimetable.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.AddSubjects.route) {
                val viewModel: com.adityaram.present.ui.onboarding.addsubjects.AddSubjectsViewModel = viewModel(factory = com.adityaram.present.ui.onboarding.addsubjects.AddSubjectsViewModel.Factory)
                com.adityaram.present.ui.onboarding.addsubjects.AddSubjectsScreen(
                    viewModel = viewModel,
                    onNavigateNext = {
                        navController.navigate(Screen.HistoricalAttendance.route) {
                            popUpTo(Screen.AddSubjects.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.HistoricalAttendance.route) {
                val viewModel: com.adityaram.present.ui.onboarding.OpeningBalanceViewModel = viewModel(factory = com.adityaram.present.ui.onboarding.OpeningBalanceViewModel.Factory)
                com.adityaram.present.ui.onboarding.HistoricalAttendanceScreen(
                    onEnterManually = { navController.navigate(Screen.OpeningBalance.route) },
                    onStartFresh = {
                        viewModel.startFresh {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
            composable(Screen.OpeningBalance.route) {
                val viewModel: com.adityaram.present.ui.onboarding.OpeningBalanceViewModel = viewModel(factory = com.adityaram.present.ui.onboarding.OpeningBalanceViewModel.Factory)
                com.adityaram.present.ui.onboarding.OpeningBalanceScreen(
                    viewModel = viewModel,
                    onFinished = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.Timetable.route) { 
                TimetableScreen(
                    listState = timetableListState,
                    onImportClick = { navController.navigate(Screen.Import.route) }
                ) 
            }
            composable(Screen.Subjects.route) { 
                SubjectsScreen(
                    listState = subjectsListState,
                    onNavigateToSubjectDetail = { subjectId ->
                        navController.navigate(Screen.SubjectDetail.createRoute(subjectId))
                    }
                ) 
            }
            composable(Screen.More.route) { 
                MoreScreen(
                    scrollState = moreScrollState,
                    onNavigateToCalendar = { navController.navigate(Screen.Calendar.route) },
                    onNavigateToAnalytics = { navController.navigate(Screen.Analytics.route) },
                    onNavigateToPlanner = { navController.navigate(Screen.Planner.route) },
                    onNavigateToSubjects = { 
                        navController.navigate(Screen.Subjects.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToTimetable = { 
                        navController.navigate(Screen.Timetable.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                    onNavigateToAppearance = { navController.navigate(Screen.Appearance.route) },
                    onNavigateToThreshold = { navController.navigate(Screen.Threshold.route) },
                    onNavigateToAccount = { navController.navigate(Screen.Account.route) },
                    onNavigateToAbout = { navController.navigate(Screen.About.route) },
                    onNavigateToLeavePlanner = { navController.navigate(Screen.LeavePlanner.route) },
                    onNavigateToTemporaryLectures = { navController.navigate(Screen.TemporaryLectures.route) }
                ) 
            }
            composable(Screen.Notifications.route) {
                com.adityaram.present.ui.notifications.NotificationsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Appearance.route) {
                com.adityaram.present.ui.appearance.AppearanceScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Threshold.route) {
                com.adityaram.present.ui.threshold.ThresholdScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Account.route) {
                val authViewModel: com.adityaram.present.ui.auth.AuthViewModel = viewModel(factory = com.adityaram.present.ui.auth.AuthViewModel.Factory)
                com.adityaram.present.ui.auth.AccountScreen(
                    viewModel = authViewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToSignIn = { navController.navigate(Screen.SignIn.route) }
                )
            }
            composable(Screen.SignIn.route) {
                val authViewModel: com.adityaram.present.ui.auth.AuthViewModel = viewModel(factory = com.adityaram.present.ui.auth.AuthViewModel.Factory)
                com.adityaram.present.ui.auth.SignInScreen(
                    viewModel = authViewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToCreateAccount = { navController.navigate(Screen.CreateAccount.route) },
                    onNavigateToResetPassword = { navController.navigate(Screen.ResetPassword.route) },
                    onNavigateToAccount = {
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.CreateAccount.route) {
                val authViewModel: com.adityaram.present.ui.auth.AuthViewModel = viewModel(factory = com.adityaram.present.ui.auth.AuthViewModel.Factory)
                com.adityaram.present.ui.auth.CreateAccountScreen(
                    viewModel = authViewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToSignIn = { navController.popBackStack() }
                )
            }
            composable(Screen.ResetPassword.route) {
                val authViewModel: com.adityaram.present.ui.auth.AuthViewModel = viewModel(factory = com.adityaram.present.ui.auth.AuthViewModel.Factory)
                com.adityaram.present.ui.auth.ResetPasswordScreen(
                    viewModel = authViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Calendar.route) {
                com.adityaram.present.ui.calendar.CalendarScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToBottomNav = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.Analytics.route) {
                com.adityaram.present.ui.analytics.AnalyticsScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToBottomNav = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.Planner.route) {
                com.adityaram.present.ui.planner.PlannerScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.LeavePlanner.route) {
                com.adityaram.present.ui.planner.LeavePlannerScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.TemporaryLectures.route) {
                com.adityaram.present.ui.temporary.TemporaryLecturesScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Import.route) {
                val importViewModel: ImportViewModel = viewModel(factory = ImportViewModel.Factory)
                val authViewModel: com.adityaram.present.ui.auth.AuthViewModel = viewModel(factory = com.adityaram.present.ui.auth.AuthViewModel.Factory)
                val authState by authViewModel.authState.collectAsState()
                
                when (authState) {
                    is com.adityaram.present.data.auth.AuthState.Idle,
                    is com.adityaram.present.data.auth.AuthState.Loading -> {
                        val fallbackColors = LocalPresentColors.current
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(fallbackColors.background),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(color = fallbackColors.accent)
                        }
                    }
                    is com.adityaram.present.data.auth.AuthState.SignedOut,
                    is com.adityaram.present.data.auth.AuthState.Error -> {
                        val fallbackColors = LocalPresentColors.current
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(fallbackColors.background)
                        ) {
                            androidx.compose.material3.AlertDialog(
                                onDismissRequest = { navController.popBackStack() },
                                title = {
                                    androidx.compose.material3.Text(
                                        text = "Sign in required", 
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                },
                                text = {
                                    androidx.compose.material3.Text("You need to be signed in to your Present account to automatically extract your timetable. Sign in to import your timetable securely.")
                                },
                                confirmButton = {
                                    androidx.compose.material3.TextButton(onClick = { 
                                        navController.navigate(Screen.SignIn.route)
                                    }) {
                                        androidx.compose.material3.Text("Sign in", color = fallbackColors.accent)
                                    }
                                },
                                dismissButton = {
                                    androidx.compose.material3.TextButton(onClick = { navController.popBackStack() }) {
                                        androidx.compose.material3.Text("Cancel", color = fallbackColors.primaryText)
                                    }
                                },
                                containerColor = fallbackColors.surface,
                                titleContentColor = fallbackColors.primaryText,
                                textContentColor = fallbackColors.secondaryText
                            )
                        }
                    }
                    is com.adityaram.present.data.auth.AuthState.SignedIn -> {
                    ImportTimetableScreen(
                        viewModel = importViewModel,
                        onBack = { navController.popBackStack() },
                        onNavigateToHome = {
                            if (onboardingCompleted == true) {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(navController.graph.findStartDestination().id)
                                    launchSingleTop = true
                                }
                            } else {
                                navController.navigate(Screen.HistoricalAttendance.route) {
                                    popUpTo(Screen.SetupTimetable.route) { inclusive = true }
                                }
                            }
                        },
                        debugContent = com.adityaram.present.ui.importing.debugImportContent(importViewModel)
                    )
                    }
                }
            }
            
            composable(
                route = Screen.SubjectDetail.route,
                arguments = listOf(navArgument("subjectId") { type = NavType.LongType })
            ) { backStackEntry ->
                val subjectId = backStackEntry.arguments?.getLong("subjectId") ?: return@composable
                SubjectDetailScreen(
                    subjectId = subjectId,
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable(Screen.About.route) {
                com.adityaram.present.ui.about.AboutScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToPrivacy = { navController.navigate(Screen.Privacy.route) },
                    onNavigateToTerms = { navController.navigate(Screen.Terms.route) },
                    onNavigateToHelp = { navController.navigate(Screen.Help.route) },
                    onNavigateToLicenses = { navController.navigate(Screen.Licenses.route) }
                )
            }
            composable(Screen.Privacy.route) {
                com.adityaram.present.ui.about.PrivacyPolicyScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Terms.route) {
                com.adityaram.present.ui.about.TermsOfUseScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Help.route) {
                com.adityaram.present.ui.about.HelpAndFeedbackScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Licenses.route) {
                com.adityaram.present.ui.about.OpenSourceLicensesScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
