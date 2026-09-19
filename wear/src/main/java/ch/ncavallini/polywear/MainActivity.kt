package ch.ncavallini.polywear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import ch.ncavallini.polywear.ui.DetailScreen
import ch.ncavallini.polywear.ui.ScheduleScreen
import ch.ncavallini.polywear.ui.ScheduleUiState
import ch.ncavallini.polywear.ui.ScheduleViewModel
import ch.ncavallini.polywear.ui.theme.PolyWearTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val container = (application as PolyApp).container

        setContent {
            PolyWearTheme {
                val viewModel: ScheduleViewModel = viewModel(
                    factory = ScheduleViewModel.factory(container),
                )
                val state by viewModel.state.collectAsState()
                val navController = rememberSwipeDismissableNavController()

                SwipeDismissableNavHost(
                    navController = navController,
                    startDestination = ROUTE_SCHEDULE,
                ) {
                    composable(ROUTE_SCHEDULE) {
                        ScheduleScreen(
                            state = state,
                            onRetry = viewModel::refresh,
                            onEventClick = { eventId ->
                                navController.navigate("$ROUTE_DETAIL/$eventId")
                            },
                            onPreviousWeek = viewModel::previousWeek,
                            onNextWeek = viewModel::nextWeek,
                            onResendFromPhone = viewModel::requestCredentialFromPhone,
                        )
                    }
                    composable("$ROUTE_DETAIL/{eventId}") { entry ->
                        val eventId = entry.arguments?.getString("eventId")
                        val event = (state as? ScheduleUiState.Content)
                            ?.days
                            ?.flatMap { it.events }
                            ?.firstOrNull { it.id == eventId }
                        if (event != null) {
                            DetailScreen(event)
                        } else {
                            // Content changed out from under us (e.g. refresh) — pop back.
                            navController.popBackStack()
                        }
                    }
                }
            }
        }
    }

    private companion object {
        const val ROUTE_SCHEDULE = "schedule"
        const val ROUTE_DETAIL = "detail"
    }
}
