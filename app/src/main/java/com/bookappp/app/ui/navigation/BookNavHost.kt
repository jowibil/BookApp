package com.bookappp.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.bookappp.app.ui.help.HelpScreen
import com.bookappp.app.ui.legal.LegalDocument
import com.bookappp.app.ui.legal.LegalScreen
import com.bookappp.app.ui.licenses.LicensesScreen
import com.bookappp.app.ui.library.LibraryScreen
import com.bookappp.app.ui.reader.ReaderScreen
import com.bookappp.app.ui.settings.SettingsScreen
import kotlinx.serialization.Serializable

// Type-safe routes (Navigation 2.10 style).
@Serializable
data object LibraryRoute

@Serializable
data class ReaderRoute(val bookId: Long)

@Serializable
data object SettingsRoute

@Serializable
data object HelpRoute

@Serializable
data object TermsRoute

@Serializable
data object PrivacyRoute

@Serializable
data object LicensesRoute

@Composable
fun BookNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = LibraryRoute,
        modifier = modifier,
    ) {
        composable<LibraryRoute> {
            LibraryScreen(
                onBookOpen = { bookId -> navController.navigate(ReaderRoute(bookId)) },
                onSettingsOpen = { navController.navigate(SettingsRoute) },
            )
        }
        composable<SettingsRoute> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onHelpOpen = { navController.navigate(HelpRoute) },
                onTermsOpen = { navController.navigate(TermsRoute) },
                onPrivacyOpen = { navController.navigate(PrivacyRoute) },
                onLicensesOpen = { navController.navigate(LicensesRoute) },
            )
        }
        composable<HelpRoute> {
            HelpScreen(onBack = { navController.popBackStack() })
        }
        composable<TermsRoute> {
            LegalScreen(
                document = LegalDocument.Terms,
                onBack = { navController.popBackStack() },
            )
        }
        composable<PrivacyRoute> {
            LegalScreen(
                document = LegalDocument.Privacy,
                onBack = { navController.popBackStack() },
            )
        }
        composable<LicensesRoute> {
            LicensesScreen(onBack = { navController.popBackStack() })
        }
        composable<ReaderRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ReaderRoute>()
            ReaderScreen(
                bookId = route.bookId,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
