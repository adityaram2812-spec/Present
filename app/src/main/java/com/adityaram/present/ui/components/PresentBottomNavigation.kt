package com.adityaram.present.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adityaram.present.ui.navigation.Screen
import com.adityaram.present.ui.theme.LocalPresentColors
import com.adityaram.present.ui.theme.Typography

data class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun PresentBottomNavigation(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val colors = LocalPresentColors.current

    val items = listOf(
        BottomNavItem("Home", Icons.Rounded.Home, Screen.Home.route),
        BottomNavItem("Timetable", Icons.Rounded.DateRange, Screen.Timetable.route),
        BottomNavItem("Subjects", Icons.Rounded.List, Screen.Subjects.route),
        BottomNavItem("More", Icons.Rounded.Settings, Screen.More.route)
    )

    NavigationBar(
        containerColor = colors.elevatedSurface,
        contentColor = colors.mutedText,
        tonalElevation = 0.dp
    ) {
        items.forEach { item ->
            val isMoreSubScreen = currentRoute in listOf(
                Screen.Notifications.route, // Reminders
                Screen.Appearance.route,
                Screen.Threshold.route,
                Screen.Account.route,
                Screen.TemporaryLectures.route,
                Screen.LeavePlanner.route,
                Screen.About.route,
                Screen.Privacy.route,
                Screen.Terms.route,
                Screen.Help.route,
                Screen.Licenses.route
            )
            val isSelected = currentRoute == item.route || (isMoreSubScreen && item.route == Screen.More.route)
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        style = Typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.accent,
                    selectedTextColor = colors.accent,
                    indicatorColor = colors.softAccent,
                    unselectedIconColor = colors.mutedText,
                    unselectedTextColor = colors.mutedText
                )
            )
        }
    }
}
