package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
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
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefsRepo = remember { UserPreferences(context.applicationContext) }
            val prefsState by prefsRepo.prefsFlow.collectAsStateWithLifecycle(initialValue = UserPrefsState())

            CompositionLocalProvider(LocalAppStrings provides stringsFor(prefsState.language)) {
                MurshidabadConnectTheme(darkTheme = prefsState.darkMode) {
                    AppRoot(prefsRepo = prefsRepo)
                }
            }
        }
    }
}

@Composable
private fun AppRoot(prefsRepo: UserPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val auth = remember { Firebase.auth }
    val currentUser by AuthHelper.authStateFlow(auth)
        .collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var checkingAutoSignIn by remember { mutableStateOf(auth.currentUser == null) }

    // Request POST_NOTIFICATIONS runtime permission on Android 13+
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { _ -> }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (auth.currentUser == null) {
            delay(500)
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

    if (checkingAutoSignIn) {
        SplashView()
    } else if (currentUser == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        val user = currentUser!!
        val uid = user.uid
        key(uid) {
            val appViewModel: AppViewModel = viewModel(
                key = uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        AppViewModel(
                            repository = MurshidabadRepository(db, FirebaseAuth.getInstance()),
                            preferences = prefsRepo,
                            currentUserId = uid,
                            firebaseUser = user
                        )
                    }
                }
            )
            MurshidabadMainApp(
                viewModel = appViewModel,
                currentUserId = uid,
                onLogout = {
                    AuthHelper.signOut(credentialManager = credentialManager, scope = scope)
                }
            )
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
