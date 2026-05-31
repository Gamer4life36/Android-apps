package com.mj.screenslayer

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mj.screenslayer.ui.screens.HomeScreen
import com.mj.screenslayer.ui.screens.LockScreenSettingsScreen
import com.mj.screenslayer.ui.screens.PreviewScreen
import com.mj.screenslayer.ui.screens.WallpaperSettingsScreen
import com.mj.screenslayer.viewmodel.MainViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController,
    mainViewModel: MainViewModel
) {
    NavHost(navController = navController, startDestination = "home") {

        composable("home") {
            HomeScreen(
                navController = navController,
                viewModel     = mainViewModel
            )
        }

        composable(
            route     = "preview/{index}",
            arguments = listOf(navArgument("index") { type = NavType.IntType })
        ) { backStackEntry ->
            val index = backStackEntry.arguments?.getInt("index") ?: return@composable
            PreviewScreen(
                imageIndex    = index,
                navController = navController,
                viewModel     = mainViewModel
            )
        }

        composable("wallpapersettings") {
            WallpaperSettingsScreen(
                navController = navController,
                viewModel     = mainViewModel
            )
        }

        composable("lockscreensettings") {
            LockScreenSettingsScreen(
                navController = navController,
                viewModel     = mainViewModel
            )
        }

    }
}
