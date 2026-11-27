package com.example.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.LocalAppStrings
import com.example.data.model.MurshidabadConstants
import com.example.ui.components.FloatingBottomNavBar
import com.example.ui.components.SuspendedAccountScreen
import com.example.ui.components.openExternalUrl
import com.example.ui.screens.CivicModuleScreen
import com.example.ui.screens.CommunityScreen
import com.example.ui.screens.HelpAndEmergencyScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LostFoundScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ServicesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.PrimaryGreen
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.UiState

@Composable
fun MurshidabadMainApp(
    viewModel: AppViewModel,
    currentUserId: String,
    onLogout: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val profileState by viewModel.userProfile.collectAsStateWithLifecycle()
    val needsOnboarding by viewModel.needsOnboarding.collectAsStateWithLifecycle()
    val prefsState by viewModel.prefsState.collectAsStateWithLifecycle()
    val categoriesState by viewModel.categoriesState.collectAsStateWithLifecycle()
    val bannersState by viewModel.bannersState.collectAsStateWithLifecycle()
    val adsState by viewModel.adsState.collectAsStateWithLifecycle()
    val announcementsState by viewModel.announcementsState.collectAsStateWithLifecycle()
    val newsState by viewModel.newsState.collectAsStateWithLifecycle()
    val jobsState by viewModel.jobsState.collectAsStateWithLifecycle()
    val educationState by viewModel.educationState.collectAsStateWithLifecycle()
    val scholarshipsState by viewModel.scholarshipsState.collectAsStateWithLifecycle()
    val healthcareState by viewModel.healthcareState.collectAsStateWithLifecycle()
    val businessesState by viewModel.businessesState.collectAsStateWithLifecycle()
    val schemesState by viewModel.schemesState.collectAsStateWithLifecycle()
    val agricultureState by viewModel.agricultureState.collectAsStateWithLifecycle()
    val marketPricesState by viewModel.marketPricesState.collectAsStateWithLifecycle()
    val transportState by viewModel.transportState.collectAsStateWithLifecycle()
    val tourismState by viewModel.tourismState.collectAsStateWithLifecycle()
    val emergencyState by viewModel.emergencyState.collectAsStateWithLifecycle()
    val notificationsState by viewModel.notificationsState.collectAsStateWithLifecycle()
    val communityPostsState by viewModel.communityPostsState.collectAsStateWithLifecycle()
    val lostFoundState by viewModel.lostFoundState.collectAsStateWithLifecycle()
    val bloodDonorsState by viewModel.bloodDonorsState.collectAsStateWithLifecycle()
    val appUpdateConfig by viewModel.appUpdateConfig.collectAsStateWithLifecycle()
    val publicSettings by viewModel.publicSettings.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionMessage()
        }
    }

    val profile = (profileState as? UiState.Success)?.data
    val categories = (categoriesState as? UiState.Success)?.data ?: MurshidabadConstants.DEFAULT_DASHBOARDS

    // 1. Check Maintenance Mode or Force Update
    val currentVersionCode = 1L
    if (appUpdateConfig?.maintenanceMode == true) {
        BlockingStatusScreen(
            icon = Icons.Rounded.Build,
            title = strings.maintenanceTitle,
            message = appUpdateConfig?.maintenanceMessage?.ifBlank { "Murshidabad Connect is undergoing scheduled maintenance. Please check back shortly." }
                ?: "Maintenance in progress."
        )
        return
    }
    if (appUpdateConfig?.forceUpdate == true && (appUpdateConfig?.minSupportedVersion ?: 1L) > currentVersionCode) {
        BlockingStatusScreen(
            icon = Icons.Rounded.SystemUpdate,
            title = strings.updateRequiredTitle,
            message = appUpdateConfig?.releaseNotes?.ifBlank { "A required update is available." } ?: "Please update the app to continue.",
            actionLabel = strings.updateNow,
            onAction = {
                val url = appUpdateConfig?.updateUrl.orEmpty()
                if (url.isNotBlank()) openExternalUrl(context, url)
            }
        )
        return
    }

    // 2. Check Suspended Account
    if (profile?.status == "SUSPENDED") {
        SuspendedAccountScreen(onLogout = onLogout)
        return
    }

    // 3. Wait for initial profile resolution or show Onboarding
    if (needsOnboarding == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryGreen)
        }
        return
    }

    if (needsOnboarding == true) {
        OnboardingScreen(
            profile = profile,
            categories = categories,
            currentLanguage = prefsState.language,
            onLanguageChange = viewModel::setLanguage,
            onSaveOnboarding = { name, village, block, district, mobile, lang, selected, primary ->
                viewModel.completeOnboardingOrUpdateProfile(
                    name = name,
                    village = village,
                    block = block,
                    district = district,
                    mobile = mobile,
                    preferredLanguage = lang,
                    selectedDashboards = selected,
                    primaryDashboard = primary
                )
            }
        )
        return
    }

    // 4. Main Authenticated Navigation Graph
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "home"
    val topLevelRoutes = setOf("home", "services", "help", "alerts", "profile")

    val unreadCount = (notificationsState as? UiState.Success)?.data?.size ?: 0

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (currentRoute in topLevelRoutes) {
                FloatingBottomNavBar(
                    currentRoute = currentRoute,
                    unreadAlertCount = unreadCount,
                    onNavigate = { target ->
                        navController.navigate(target) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { _ ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.fillMaxSize()
        ) {
            composable("home") {
                HomeScreen(
                    profile = profile,
                    categories = categories,
                    bannersState = bannersState,
                    announcementsState = announcementsState,
                    newsState = newsState,
                    jobsState = jobsState,
                    educationState = educationState,
                    healthcareState = healthcareState,
                    businessesState = businessesState,
                    tourismState = tourismState,
                    adsState = adsState,
                    searchQuery = searchQuery,
                    isRefreshing = isRefreshing,
                    onRefresh = viewModel::refreshAll,
                    onSearchChange = viewModel::updateSearchQuery,
                    onSwitchDashboard = viewModel::switchPrimaryDashboard,
                    onNavigate = { route ->
                        if (route in topLevelRoutes) {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        } else {
                            navController.navigate(route)
                        }
                    }
                )
            }

            composable("services") {
                ServicesScreen(onNavigate = { route ->
                    if (route in topLevelRoutes) {
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    } else {
                        navController.navigate(route)
                    }
                })
            }

            composable("help") {
                HelpAndEmergencyScreen(
                    emergencyState = emergencyState,
                    onNavigateSettings = { navController.navigate("settings") }
                )
            }

            composable("alerts") {
                NotificationsScreen(
                    notificationsState = notificationsState,
                    announcementsState = announcementsState,
                    onRetry = viewModel::refreshAll
                )
            }

            composable("profile") {
                ProfileScreen(
                    profile = profile,
                    categories = categories,
                    onEditProfile = { navController.navigate("edit_profile") },
                    onOpenSettings = { navController.navigate("settings") },
                    onLogout = onLogout
                )
            }

            composable("edit_profile") {
                OnboardingScreen(
                    profile = profile,
                    categories = categories,
                    currentLanguage = prefsState.language,
                    onLanguageChange = viewModel::setLanguage,
                    onSaveOnboarding = { name, village, block, district, mobile, lang, selected, primary ->
                        viewModel.completeOnboardingOrUpdateProfile(
                            name = name,
                            village = village,
                            block = block,
                            district = district,
                            mobile = mobile,
                            preferredLanguage = lang,
                            selectedDashboards = selected,
                            primaryDashboard = primary,
                            onDone = { navController.popBackStack() }
                        )
                    }
                )
            }

            composable("settings") {
                SettingsScreen(
                    prefsState = prefsState,
                    appUpdateConfig = appUpdateConfig,
                    publicSettings = publicSettings,
                    onLanguageChange = viewModel::setLanguage,
                    onDarkModeChange = viewModel::setDarkMode,
                    onNotificationsChange = viewModel::setNotificationsEnabled,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("community") {
                CommunityScreen(
                    currentUserId = currentUserId,
                    postsState = communityPostsState,
                    observeComments = viewModel::observeComments,
                    onCreatePost = viewModel::createCommunityPost,
                    onToggleLike = viewModel::toggleLike,
                    onAddComment = viewModel::addComment,
                    onSubmitReport = viewModel::submitReport,
                    onRetry = viewModel::refreshAll,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("lost_found") {
                LostFoundScreen(
                    itemsState = lostFoundState,
                    onCreateItem = viewModel::createLostFound,
                    onRetry = viewModel::refreshAll,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "module/{moduleKey}",
                arguments = listOf(navArgument("moduleKey") { type = NavType.StringType })
            ) { entry ->
                val moduleKey = entry.arguments?.getString("moduleKey") ?: "news"
                val (primary, secondary) = when (moduleKey) {
                    "news" -> newsState to null
                    "jobs" -> jobsState to null
                    "education" -> educationState to scholarshipsState
                    "scholarships" -> scholarshipsState to null
                    "healthcare" -> healthcareState to null
                    "businesses" -> businessesState to null
                    "schemes" -> schemesState to null
                    "agriculture" -> agricultureState to marketPricesState
                    "market_prices" -> marketPricesState to null
                    "transport" -> transportState to null
                    "tourism" -> tourismState to null
                    else -> newsState to null
                }
                CivicModuleScreen(
                    moduleKey = moduleKey,
                    primaryState = primary,
                    secondaryState = secondary,
                    bloodDonorsState = if (moduleKey == "healthcare") bloodDonorsState else null,
                    observeBusinessReviews = viewModel::observeBusinessReviews,
                    onSubmitBusinessReview = viewModel::submitBusinessReview,
                    onCreateBloodEntry = viewModel::createBloodEntry,
                    onRetry = viewModel::refreshAll,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun BlockingStatusScreen(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(shape = RoundedCornerShape(24.dp)) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(icon, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(52.dp))
                Spacer(Modifier.height(14.dp))
                Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                if (actionLabel != null && onAction != null) {
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = onAction,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Text(actionLabel, color = Color.White)
                    }
                }
            }
        }
    }
}
