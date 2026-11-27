package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.AuthHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = MurshidabadApplication::class)
class MainActivityStartupRobolectricTest {

    private fun ensureTestFirebase(): FirebaseApp {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val existing = FirebaseApp.getApps(context)
        if (existing.isNotEmpty()) return existing.first()
        val options = FirebaseOptions.Builder()
            .setApplicationId(context.getString(R.string.google_app_id))
            .setApiKey(context.getString(R.string.google_api_key))
            .setProjectId(context.getString(R.string.project_id))
            .setGcmSenderId(context.getString(R.string.gcm_defaultSenderId))
            .build()
        return FirebaseApp.initializeApp(context, options)
    }

    @Test
    fun `main activity starts safely even when firebase is uninitialized or initialized`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        assertNotNull(activity)
        assertEquals("com.murshidabad.connect", activity.packageName)
        controller.pause().stop().destroy()
    }

    @Test
    fun `firebase and named firestore database initialize without crashing`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val app = ensureTestFirebase()
        val state = MurshidabadApplication.ensureFirebaseInitialized(context)
        assertTrue(state is FirebaseInitState.Ready)
        val dbId = context.getString(R.string.firestore_database_id)
        assertEquals("ai-studio-android-murshida-a77068e6-7c4a-4a53-be40-aa155ef83318", dbId)
        val db = FirebaseFirestore.getInstance(app, dbId)
        assertNotNull(db)
    }

    @Test
    fun `auto sign in falls back cleanly to unauthenticated without crashing`() = runTest {
        ensureTestFirebase()
        val context = ApplicationProvider.getApplicationContext<Context>()
        var unauthenticatedCalled = false
        AuthHelper.attemptAutoSignIn(
            context = context,
            credentialManager = null,
            onAuthSuccess = {},
            onUnauthenticated = { unauthenticatedCalled = true },
            scope = this
        )
        assertTrue(unauthenticatedCalled)
    }

    @Test
    fun `interactive google sign in reports clean error when credential manager unavailable`() = runTest {
        ensureTestFirebase()
        val context = ApplicationProvider.getApplicationContext<Context>()
        var errorMsg: String? = null
        AuthHelper.onGoogleSignInClicked(
            context = context,
            credentialManager = null,
            onAuthSuccess = {},
            onAuthError = { errorMsg = it },
            scope = this
        )
        assertNotNull(errorMsg)
        assertTrue(errorMsg!!.isNotBlank())
    }
}
