package com.example

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.local.LocalAppStrings
import com.example.data.local.UserPrefsState
import com.example.data.local.stringsFor
import com.example.data.model.BloodDonorItem
import com.example.data.model.CommunityPost
import com.example.data.model.ContentItem
import com.example.data.model.LostFoundItem
import com.example.data.model.MurshidabadConstants
import com.example.data.model.NotificationItem
import com.example.data.model.UserProfile
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
import com.example.ui.theme.MurshidabadConnectTheme
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StartupAndScreensRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleProfile = UserProfile(
        uid = "test_uid",
        name = "Rahim Sheikh",
        email = "rahim@gmail.com",
        block = "Berhampore",
        village = "Khagra",
        district = "Murshidabad",
        selectedDashboards = listOf("general", "student"),
        primaryDashboard = "general"
    )
    private val sampleItems = listOf(
        ContentItem(id = "1", title = "Murshidabad Update", description = "Details", category = "District"),
        ContentItem(id = "2", title = "Hazarduari Heritage", description = "Tourism", category = "Heritage")
    )

    @Test
    fun `onboarding screen renders`() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor("bn")) {
                MurshidabadConnectTheme {
                    OnboardingScreen(
                        profile = sampleProfile,
                        categories = MurshidabadConstants.DEFAULT_DASHBOARDS,
                        currentLanguage = "bn",
                        onLanguageChange = {},
                        onSaveOnboarding = { _, _, _, _, _, _, _, _ -> }
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `home screen renders`() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor("bn")) {
                MurshidabadConnectTheme {
                    HomeScreen(
                        profile = sampleProfile,
                        categories = MurshidabadConstants.DEFAULT_DASHBOARDS,
                        bannersState = UiState.Success(sampleItems),
                        announcementsState = UiState.Success(sampleItems),
                        newsState = UiState.Success(sampleItems),
                        jobsState = UiState.Success(sampleItems),
                        educationState = UiState.Success(sampleItems),
                        healthcareState = UiState.Success(sampleItems),
                        businessesState = UiState.Success(sampleItems),
                        tourismState = UiState.Success(sampleItems),
                        adsState = UiState.Success(sampleItems),
                        searchQuery = "",
                        isRefreshing = false,
                        onRefresh = {},
                        onSearchChange = {},
                        onSwitchDashboard = {},
                        onNavigate = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `civic module screen renders`() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor("en")) {
                MurshidabadConnectTheme {
                    CivicModuleScreen(
                        moduleKey = "education",
                        primaryState = UiState.Success(sampleItems),
                        secondaryState = UiState.Success(sampleItems),
                        bloodDonorsState = UiState.Success(listOf(BloodDonorItem(id = "b1", title = "Donor", bloodGroup = "O+"))),
                        observeBusinessReviews = { flowOf(emptyList()) },
                        onSubmitBusinessReview = { _, _, _, _ -> },
                        onCreateBloodEntry = { _, _, _, _, _, _, _, _ -> },
                        onRetry = {},
                        onBack = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `services and help screens render`() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor("en")) {
                MurshidabadConnectTheme {
                    ServicesScreen(onNavigate = {})
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `help emergency screen renders`() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor("en")) {
                MurshidabadConnectTheme {
                    HelpAndEmergencyScreen(emergencyState = UiState.Success(sampleItems), onNavigateSettings = {})
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `community screen renders`() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor("en")) {
                MurshidabadConnectTheme {
                    CommunityScreen(
                        currentUserId = "u1",
                        postsState = UiState.Success(listOf(CommunityPost(id = "p1", title = "Post", description = "Body"))),
                        observeComments = { flowOf(emptyList()) },
                        onCreatePost = { _, _, _, _, _ -> },
                        onToggleLike = {},
                        onAddComment = { _, _, _ -> },
                        onSubmitReport = { _, _, _, _, _ -> },
                        onRetry = {},
                        onBack = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `lost found screen renders`() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor("en")) {
                MurshidabadConnectTheme {
                    LostFoundScreen(
                        itemsState = UiState.Success(listOf(LostFoundItem(id = "l1", title = "Wallet", description = "Black"))),
                        onCreateItem = { _, _, _, _, _, _, _, _ -> },
                        onRetry = {},
                        onBack = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `notifications screen renders`() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor("en")) {
                MurshidabadConnectTheme {
                    NotificationsScreen(
                        notificationsState = UiState.Success(listOf(NotificationItem(id = "n1", title = "Alert", description = "Msg"))),
                        announcementsState = UiState.Success(sampleItems),
                        onRetry = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `profile and settings screens render`() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor("en")) {
                MurshidabadConnectTheme {
                    ProfileScreen(
                        profile = UserProfile(uid = "u1", name = "Rahim"),
                        categories = MurshidabadConstants.DEFAULT_DASHBOARDS,
                        onEditProfile = {},
                        onOpenSettings = {},
                        onLogout = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `settings screen renders`() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor("en")) {
                MurshidabadConnectTheme {
                    SettingsScreen(
                        prefsState = UserPrefsState(),
                        appUpdateConfig = null,
                        publicSettings = null,
                        onLanguageChange = {},
                        onDarkModeChange = {},
                        onNotificationsChange = {},
                        onBack = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
    }
}
