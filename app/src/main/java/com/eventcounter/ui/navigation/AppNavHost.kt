package com.eventcounter.ui.navigation

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.eventcounter.ui.archive.ArchiveScreen
import com.eventcounter.ui.event.EventEditorScreen
import com.eventcounter.ui.group.GroupEditorScreen
import com.eventcounter.ui.home.HomeScreen
import com.eventcounter.ui.settings.SettingsScreen

private object Routes {
    const val HOME = "home"
    const val FOLDER = "folder/{groupId}"
    const val ARCHIVE = "archive"
    const val SETTINGS = "settings"
    const val EVENT = "event?eventId={eventId}&parentId={parentId}"
    const val GROUP = "group?groupId={groupId}&parentId={parentId}"

    fun folder(id: Long) = "folder/$id"

    fun event(eventId: Long = -1L, parentId: Long = -1L) = "event?eventId=$eventId&parentId=$parentId"

    fun group(groupId: Long = -1L, parentId: Long = -1L) = "group?groupId=$groupId&parentId=$parentId"
}

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    openEventId: Long? = null,
    openEventToken: Int = 0,
) {
    LaunchedEffect(openEventToken) {
        val eventId = openEventId ?: return@LaunchedEffect
        if (openEventToken == 0) return@LaunchedEffect
        navController.navigate(Routes.event(eventId = eventId)) {
            launchSingleTop = true
        }
    }
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeRoute(parentId = null, navController = navController)
        }
        composable(
            route = Routes.FOLDER,
            arguments = listOf(navArgument("groupId") { type = NavType.LongType }),
        ) { entry ->
            HomeRoute(
                parentId = entry.arguments?.getLong("groupId"),
                navController = navController,
            )
        }
        composable(
            route = Routes.EVENT,
            arguments = listOf(
                navArgument("eventId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument("parentId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) { entry ->
            EventEditorScreen(
                eventId = entry.arguments.optionalId("eventId"),
                parentId = entry.arguments.optionalId("parentId"),
                onDone = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.GROUP,
            arguments = listOf(
                navArgument("groupId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument("parentId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) { entry ->
            GroupEditorScreen(
                groupId = entry.arguments.optionalId("groupId"),
                parentId = entry.arguments.optionalId("parentId"),
                onDone = { navController.popBackStack() },
            )
        }
        composable(Routes.ARCHIVE) {
            ArchiveScreen(
                onBack = { navController.popBackStack() },
                onOpenEvent = { navController.navigate(Routes.event(eventId = it)) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun HomeRoute(parentId: Long?, navController: NavHostController) {
    HomeScreen(
        parentId = parentId,
        onBack = { navController.popBackStack() },
        onOpenGroup = { navController.navigate(Routes.folder(it)) },
        onOpenEvent = { navController.navigate(Routes.event(eventId = it)) },
        onCreateEvent = { navController.navigate(Routes.event(parentId = parentId ?: -1L)) },
        onCreateGroup = { navController.navigate(Routes.group(parentId = parentId ?: -1L)) },
        onOpenArchive = { navController.navigate(Routes.ARCHIVE) { launchSingleTop = true } },
        onSettings = { navController.navigate(Routes.SETTINGS) { launchSingleTop = true } },
    )
}

private fun Bundle?.optionalId(key: String): Long? {
    val value = this?.getLong(key, -1L) ?: -1L
    return value.takeIf { it >= 0L }
}
