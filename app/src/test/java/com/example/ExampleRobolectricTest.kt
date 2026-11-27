package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LocalAppStrings
import com.example.data.local.stringsFor
import com.example.data.model.MurshidabadConstants
import com.example.ui.components.FloatingBottomNavBar
import com.example.ui.screens.AuthScreen
import com.example.ui.theme.MurshidabadConnectTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `app name matches Murshidabad Connect`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Murshidabad Connect", context.getString(R.string.app_name))
    }

    @Test
    fun `localized strings support Bengali English and Hindi`() {
        val bn = stringsFor("bn")
        val en = stringsFor("en")
        val hi = stringsFor("hi")
        assertEquals("মুর্শিদাবাদ কানেক্ট", bn.appName)
        assertEquals("Murshidabad Connect", en.appName)
        assertEquals("मुर्शिदाबाद कनेक्ट", hi.appName)
    }

    @Test
    fun `url validation accepts public https image urls and rejects invalid schemes`() {
        assertTrue(MurshidabadConstants.isValidImageUrl("https://i.ibb.co/example/photo.jpg"))
        assertTrue(MurshidabadConstants.isValidImageUrl(""))
        assertFalse(MurshidabadConstants.isValidImageUrl("file:///sdcard/photo.jpg"))
        assertFalse(MurshidabadConstants.isValidImageUrl("javascript:alert(1)"))
    }

    @Test
    fun `default dashboard categories include all seven citizen roles`() {
        assertEquals(7, MurshidabadConstants.DEFAULT_DASHBOARDS.size)
        assertEquals(26, MurshidabadConstants.BLOCKS.size)
    }

    @Test
    fun `auth screen renders Google Sign-In button`() {
        composeTestRule.setContent {
            MurshidabadConnectTheme {
                AuthScreen(onAuthSuccess = {})
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed()
    }

    @Test
    fun `floating bottom navigation bar renders all five items and dispatches clicks`() {
        var navigatedTo = ""
        composeTestRule.setContent {
            MurshidabadConnectTheme {
                FloatingBottomNavBar(
                    currentRoute = "home",
                    onNavigate = { navigatedTo = it }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_home").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_services").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_help").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_alerts").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_profile").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_services").performClick()
        assertEquals("services", navigatedTo)
        composeTestRule.onNodeWithTag("nav_help").performClick()
        assertEquals("help", navigatedTo)
    }
}
