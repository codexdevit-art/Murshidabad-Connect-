package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.LocalAppStrings
import com.example.data.local.UserPreferences
import com.example.data.local.UserPrefsState
import com.example.data.local.stringsFor
import com.example.data.repository.AuthHelper
import com.example.data.repository.MurshidabadRepository
import com.example.ui.components.MurshidabadLogoBadge
import com.example.ui.navigation.MurshidabadMainApp
import com.example.ui.screens.AuthScreen
import com.example.ui.theme.DarkGreen
import com.example.ui.theme.MurshidabadConnectTheme
import com.example.ui.theme.PrimaryGreen
import com.example.ui.viewmodel.AppViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "MainActivity.onCreate started")
        runCatching { enableEdgeToEdge() }
            .onFailure { Log.w(TAG, "enableEdgeToEdge failed cleanly: ${it.message}", it) }

        setContent {
            val context = LocalContext.current
            val prefsRepo = remember(context) {
                (context.applicationContext as? MurshidabadApplication)?.userPreferences
                    ?: UserPreferences(context.applicationContext)
            }
            val prefsState by prefsRepo.prefsFlow.collectAsStateWithLifecycle(initialValue = UserPrefsState())

            CompositionLocalProvider(LocalAppStrings provides stringsFor(prefsState.language)) {
                MurshidabadConnectTheme(darkTheme = prefsState.darkMode) {
                    AppRoot(prefsRepo = prefsRepo)
                }
            }
        }
    }

    companion object {
        private const val TAG = "MurshidabadMain"
    }
}

@Composable
private fun AppRoot(prefsRepo: UserPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var initRetryTick by remember { mutableIntStateOf(0) }

    val firebaseInitState = remember(initRetryTick) {
        MurshidabadApplication.ensureFirebaseInitialized(context)
    }

    if (firebaseInitState is FirebaseInitState.Failed) {
        StartupErrorScreen(
            message = firebaseInitState.message,
            onRetry = { initRetryTick += 1 }
        )
        return
    }

    val auth = remember(initRetryTick) { AuthHelper.getSafeFirebaseAuth(context) }
    if (auth == null) {
        StartupErrorScreen(
            message = context.getString(R.string.error_firebase_init),
            onRetry = { initRetryTick += 1 }
        )
        return
    }

    val credentialManager = remember(context) { AuthHelper.createCredentialManager(context) }
    val currentUser by AuthHelper.authStateFlow(auth)
        .collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var checkingAutoSignIn by remember { mutableStateOf(auth.currentUser == null) }
    var notificationPrompted by remember { mutableStateOf(false) }

    // Request POST_NOTIFICATIONS runtime permission on Android 13+ only after splash/auto-auth settles
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            Log.i("MurshidabadPerms", "POST_NOTIFICATIONS granted=$granted")
        }
    )

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            delay(350)
            AuthHelper.attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { checkingAutoSignIn = false },
                onUnauthenticated = { checkingAutoSignIn = false },
                scope = scope
            )
        } else {
            checkingAutoSignIn = false
        }
    }

    LaunchedEffect(checkingAutoSignIn) {
        if (!checkingAutoSignIn && !notificationPrompted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPrompted = true
            runCatching {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }.onFailure { e ->
                Log.w("MurshidabadPerms", "Notification permission prompt skipped: ${e.message}", e)
            }
        }
    }

    val activeUser = currentUser
    if (checkingAutoSignIn) {
        SplashView()
    } else if (activeUser == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        val uid = activeUser.uid
        val firestoreInitResult = remember(uid, initRetryTick) {
            runCatching {
                val app = FirebaseApp.getInstance()
                val databaseId = context.applicationContext.getString(R.string.firestore_database_id)
                Log.i("MurshidabadMain", "Creating Firestore instance for databaseId=$databaseId uid=$uid")
                FirebaseFirestore.getInstance(app, databaseId)
            }
        }

        val db = firestoreInitResult.getOrNull()
        if (db == null) {
            val err = firestoreInitResult.exceptionOrNull()
            Log.e("MurshidabadMain", "Firestore initialization error for user=$uid", err)
            StartupErrorScreen(
                message = err?.localizedMessage ?: "Unable to connect to Cloud Firestore.",
                onRetry = { initRetryTick += 1 }
            )
            return
        }

        key(uid) {
            val appViewModel: AppViewModel = viewModel(
                key = uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        Log.i("MurshidabadMain", "Initializing AppViewModel for uid=$uid")
                        AppViewModel(
                            repository = MurshidabadRepository(db, auth),
                            preferences = (app as? MurshidabadApplication)?.userPreferences ?: prefsRepo,
                            currentUserId = uid,
                            firebaseUser = activeUser
                        )
                    }
                }
            )
            MurshidabadMainApp(
                viewModel = appViewModel,
                currentUserId = uid,
                onLogout = {
                    AuthHelper.signOut(
                        context = context,
                        credentialManager = credentialManager,
                        scope = scope
                    )
                }
            )
        }
    }
}

@Composable
private fun StartupErrorScreen(
    message: String,
    onRetry: () -> Unit
) {
    val strings = LocalAppStrings.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Murshidabad Connect",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text(strings.retry, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun SplashView() {
    val strings = LocalAppStrings.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkGreen, PrimaryGreen))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            MurshidabadLogoBadge(size = 92.dp)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Murshidabad Connect",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = strings.tagline,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(26.dp))
            CircularProgressIndicator(color = Color.White)
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Slstrms Technologies",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.78f)
            )
        }
    }
}
