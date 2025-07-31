package com.yourcompany.worklisten.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.yourcompany.worklisten.R

/**
 * 导航项
 */
sealed class NavItem(
    val route: String,
    val icon: ImageVector,
    val titleResId: Int
) {
    object Player : NavItem("player", Icons.Default.Headset, R.string.nav_player)
    object Review : NavItem("review", Icons.Default.Book, R.string.nav_review)
    object Library : NavItem("library", Icons.AutoMirrored.Filled.LibraryBooks, R.string.nav_library)
    object Group : NavItem("group", Icons.AutoMirrored.Filled.List, R.string.select_groups)
}

/**
 * 底部导航栏
 */
@Composable
fun BottomNavigationBar(
    navController: NavController,
    items: List<NavItem>
) {
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = stringResource(item.titleResId)) },
                label = { Text(stringResource(item.titleResId)) },
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            // 避免堆栈中的重复项
                            popUpTo(navController.graph.startDestinationId) {
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
} 