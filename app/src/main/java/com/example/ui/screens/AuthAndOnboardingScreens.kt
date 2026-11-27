package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Agriculture
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Elderly
import androidx.compose.material.icons.rounded.Female
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.LocalAppStrings
import com.example.data.model.DashboardCategory
import com.example.data.model.MurshidabadConstants
import com.example.data.model.PublicSettings
import com.example.data.model.UserProfile
import com.example.data.repository.AuthHelper
import com.example.ui.components.MurshidabadLogoBadge
import com.example.ui.components.UserAvatar
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentAmberBg
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentBlueBg
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.AccentPurpleBg
import com.example.ui.theme.AccentRose
import com.example.ui.theme.AccentRoseBg
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.AccentTealBg
import com.example.ui.theme.DarkGreen
import com.example.ui.theme.LightGreen
import com.example.ui.theme.PrimaryGreen

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    publicSettings: PublicSettings? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { AuthHelper.createCredentialManager(context) }
    val strings = LocalAppStrings.current

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPolicyDialog by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkGreen, PrimaryGreen, MaterialTheme.colorScheme.background)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(36.dp))
            MurshidabadLogoBadge(size = 86.dp)
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Murshidabad Connect",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = strings.tagline,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.88f)
            )
            Spacer(Modifier.height(28.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = strings.welcomeTitle,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = strings.welcomeSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    if (errorMessage != null) {
                        Spacer(Modifier.height(16.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = {
                            isLoading = true
                            errorMessage = null
                            AuthHelper.onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isLoading = false
                                    errorMessage = msg
                                },
                                scope = scope,
                                onAuthCancelled = {
                                    isLoading = false
                                }
                            )
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("google_sign_in_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("Signing in...", color = Color.White)
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.AccountCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = strings.continueWithGoogle,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showPolicyDialog = "privacy" }) {
                            Text(strings.privacyPolicy, style = MaterialTheme.typography.labelMedium)
                        }
                        Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { showPolicyDialog = "terms" }) {
                            Text(strings.termsConditions, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            Text(
                text = "Powered by ${strings.developerBy}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showPolicyDialog != null) {
        val isPrivacy = showPolicyDialog == "privacy"
        val defaultPrivacy = "Murshidabad Connect uses Google Sign-In solely to verify your identity, personalize your district dashboard, and securely store your selected block, village, and community posts in Cloud Firestore. We do not collect passwords or sell your data."
        val defaultTerms = "By using Murshidabad Connect, you agree to share accurate civic information, respect community guidelines, and refrain from posting harmful or misleading content."
        AlertDialog(
            onDismissRequest = { showPolicyDialog = null },
            title = { Text(if (isPrivacy) strings.privacyPolicy else strings.termsConditions) },
            text = {
                Text(
                    if (isPrivacy) publicSettings?.privacyPolicy?.ifBlank { defaultPrivacy } ?: defaultPrivacy
                    else publicSettings?.termsConditions?.ifBlank { defaultTerms } ?: defaultTerms
                )
            },
            confirmButton = {
                TextButton(onClick = { showPolicyDialog = null }) {
                    Text("OK")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    profile: UserProfile?,
    categories: List<DashboardCategory>,
    currentLanguage: String,
    onLanguageChange: (String) -> Unit,
    onSaveOnboarding: (
        name: String,
        village: String,
        block: String,
        district: String,
        mobile: String,
        language: String,
        selectedDashboards: List<String>,
        primaryDashboard: String
    ) -> Unit
) {
    val strings = LocalAppStrings.current
    var name by remember(profile) { mutableStateOf(profile?.name ?: "") }
    var village by remember(profile) { mutableStateOf(profile?.village ?: "") }
    var block by remember(profile) { mutableStateOf(profile?.block?.ifBlank { "Berhampore" } ?: "Berhampore") }
    var district by remember(profile) { mutableStateOf(profile?.district?.ifBlank { "Murshidabad" } ?: "Murshidabad") }
    var mobile by remember(profile) { mutableStateOf(profile?.mobile ?: "") }
    var selectedLang by remember(currentLanguage) { mutableStateOf(currentLanguage) }
    val selectedDashboards = remember(profile) {
        mutableStateListOf<String>().apply {
            val initial = profile?.selectedDashboards.orEmpty().ifEmpty { listOf("general") }
            addAll(initial)
        }
    }
    var primaryDashboard by remember(profile) {
        mutableStateOf(profile?.primaryDashboard?.ifBlank { "general" } ?: "general")
    }
    var blockExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        Surface(
            color = PrimaryGreen,
            shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                UserAvatar(
                    photoUrl = profile?.photoUrl,
                    name = name,
                    size = 68.dp
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = strings.onboardingTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = strings.onboardingSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.88f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Language Chips
            Text(strings.preferredLanguage, style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("bn" to "বাংলা", "en" to "English", "hi" to "हिन्दी").forEach { (code, label) ->
                    FilterChip(
                        selected = selectedLang == code,
                        onClick = {
                            selectedLang = code
                            onLanguageChange(code)
                        },
                        leadingIcon = { Icon(Icons.Rounded.Language, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text(label) }
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(strings.fullName) },
                leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("onboarding_name_input")
            )

            OutlinedTextField(
                value = profile?.email ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text(strings.emailLabel) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = blockExpanded,
                onExpandedChange = { blockExpanded = !blockExpanded }
            ) {
                OutlinedTextField(
                    value = block,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(strings.blockLabel) },
                    leadingIcon = { Icon(Icons.Rounded.LocationOn, contentDescription = null) },
                    trailingIcon = { Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = blockExpanded,
                    onDismissRequest = { blockExpanded = false }
                ) {
                    MurshidabadConstants.BLOCKS.forEach { b ->
                        DropdownMenuItem(
                            text = { Text(b) },
                            onClick = {
                                block = b
                                blockExpanded = false
                            }
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = village,
                    onValueChange = { village = it },
                    label = { Text(strings.villageTown) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).testTag("onboarding_village_input")
                )
                OutlinedTextField(
                    value = district,
                    onValueChange = { district = it },
                    label = { Text(strings.districtLabel) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = mobile,
                onValueChange = { mobile = it },
                label = { Text("Mobile (Optional)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(4.dp))
            Text(strings.chooseDashboards, style = MaterialTheme.typography.titleMedium)

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                maxItemsInEachRow = 2
            ) {
                categories.forEachIndexed { idx, cat ->
                    val isSelected = selectedDashboards.contains(cat.slug)
                    val isPrimary = primaryDashboard == cat.slug
                    val (bgTint, iconTint, iconVec) = categoryStyleFor(cat.slug)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                if (isSelected) {
                                    if (selectedDashboards.size > 1) {
                                        selectedDashboards.remove(cat.slug)
                                        if (primaryDashboard == cat.slug) {
                                            primaryDashboard = selectedDashboards.first()
                                        }
                                    }
                                } else {
                                    selectedDashboards.add(cat.slug)
                                    primaryDashboard = cat.slug
                                }
                            }
                            .testTag("dashboard_card_${cat.slug}"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) LightGreen else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(bgTint),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(iconVec, contentDescription = null, tint = iconTint)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = when (selectedLang) {
                                    "bn" -> cat.bengaliName.ifBlank { cat.title }
                                    "hi" -> cat.hindiName.ifBlank { cat.title }
                                    else -> cat.englishName.ifBlank { cat.title }
                                },
                                style = MaterialTheme.typography.titleSmall,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = cat.englishName.ifBlank { cat.title },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            if (isSelected) {
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = PrimaryGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (isPrimary) "Primary" else "Selected",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PrimaryGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (selectedDashboards.size > 1) {
                Text(strings.primaryDashboard, style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    selectedDashboards.forEach { slug ->
                        val cat = categories.find { it.slug == slug }
                        FilterChip(
                            selected = primaryDashboard == slug,
                            onClick = { primaryDashboard = slug },
                            label = {
                                Text(
                                    when (selectedLang) {
                                        "bn" -> cat?.bengaliName ?: slug
                                        else -> cat?.englishName ?: slug
                                    }
                                )
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    onSaveOnboarding(
                        name.ifBlank { profile?.name ?: "Murshidabad Resident" },
                        village,
                        block,
                        district.ifBlank { "Murshidabad" },
                        mobile,
                        selectedLang,
                        selectedDashboards.toList(),
                        primaryDashboard
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("onboarding_finish_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text(strings.finishSetup, style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

fun categoryStyleFor(slug: String): Triple<Color, Color, ImageVector> = when (slug) {
    "student" -> Triple(AccentBlueBg, AccentBlue, Icons.Rounded.School)
    "farmer" -> Triple(LightGreen, PrimaryGreen, Icons.Rounded.Agriculture)
    "job_seeker" -> Triple(AccentAmberBg, AccentAmber, Icons.Rounded.Work)
    "business" -> Triple(AccentTealBg, AccentTeal, Icons.Rounded.Storefront)
    "women" -> Triple(AccentRoseBg, AccentRose, Icons.Rounded.Female)
    "senior" -> Triple(AccentPurpleBg, AccentPurple, Icons.Rounded.Elderly)
    else -> Triple(LightGreen, DarkGreen, Icons.Rounded.Groups)
}
