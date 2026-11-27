package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Help
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.LocalAppStrings
import com.example.data.local.UserPrefsState
import com.example.data.model.AppUpdateConfig
import com.example.data.model.ContentItem
import com.example.data.model.DashboardCategory
import com.example.data.model.NotificationItem
import com.example.data.model.PublicSettings
import com.example.data.model.UserProfile
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorStateCard
import com.example.ui.components.ModuleTopBar
import com.example.ui.components.RemoteImageWithFallback
import com.example.ui.components.SkeletonLoadingList
import com.example.ui.components.UserAvatar
import com.example.ui.components.openExternalUrl
import com.example.ui.theme.AccentAmberBg
import com.example.ui.theme.AccentRose
import com.example.ui.theme.DarkGreen
import com.example.ui.theme.LightGreen
import com.example.ui.theme.PrimaryGreen
import com.example.ui.viewmodel.UiState

@Composable
fun NotificationsScreen(
    notificationsState: UiState<List<NotificationItem>>,
    announcementsState: UiState<List<ContentItem>>,
    onRetry: () -> Unit
) {
    val strings = LocalAppStrings.current
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Announcements", "Job", "Education", "Emergency", "General")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ModuleTopBar(title = strings.alertsTitle)
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { f ->
                FilterChip(selected = selectedFilter == f, onClick = { selectedFilter = f }, label = { Text(f) })
            }
        }

        val announcements = (announcementsState as? UiState.Success)?.data.orEmpty()
        when (notificationsState) {
            is UiState.Loading -> SkeletonLoadingList()
            is UiState.Error -> ErrorStateCard(notificationsState.message, onRetry)
            is UiState.Success -> {
                val notifs = notificationsState.data.filter {
                    selectedFilter == "All" || it.category.contains(selectedFilter, ignoreCase = true)
                }
                val showAnnouncements = selectedFilter == "All" || selectedFilter == "Announcements"
                if (notifs.isEmpty() && (!showAnnouncements || announcements.isEmpty())) {
                    EmptyStateCard(title = "No alerts yet", icon = Icons.Rounded.Notifications)
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 110.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (showAnnouncements) {
                            items(announcements, key = { "ann_${it.id}" }) { ann ->
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = AccentAmberBg)
                                ) {
                                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                                        Icon(Icons.Rounded.Campaign, contentDescription = null, tint = Color(0xFFD97706))
                                        Spacer(Modifier.width(10.dp))
                                        Column {
                                            Text(ann.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF78350F))
                                            Spacer(Modifier.height(2.dp))
                                            Text(ann.description, style = MaterialTheme.typography.bodySmall, color = Color(0xFF92400E))
                                        }
                                    }
                                }
                            }
                        }
                        items(notifs, key = { it.id }) { n ->
                            Card(shape = RoundedCornerShape(16.dp)) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    if (n.imageUrl.isNotBlank()) {
                                        RemoteImageWithFallback(
                                            imageUrl = n.imageUrl,
                                            contentDescription = n.title,
                                            height = 130.dp,
                                            cornerRadius = 12.dp
                                        )
                                        Spacer(Modifier.height(8.dp))
                                    }
                                    Surface(color = LightGreen, shape = RoundedCornerShape(8.dp)) {
                                        Text(
                                            "${n.category} • ${n.targetAudience}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DarkGreen,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(n.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(4.dp))
                                    Text(n.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    profile: UserProfile?,
    categories: List<DashboardCategory>,
    onEditProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val strings = LocalAppStrings.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 110.dp)
    ) {
        ModuleTopBar(
            title = strings.profileTitle,
            trailingAction = {
                TextButton(onClick = onOpenSettings) {
                    Icon(Icons.Rounded.Settings, contentDescription = strings.settingsTitle, tint = Color.White)
                }
            }
        )

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Identity Card with automatic Google DP
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    UserAvatar(
                        photoUrl = profile?.photoUrl,
                        name = profile?.name ?: "User",
                        size = 86.dp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = profile?.name?.ifBlank { "Murshidabad Resident" } ?: "Murshidabad Resident",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = profile?.email ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Surface(color = LightGreen, shape = RoundedCornerShape(10.dp)) {
                        Text(
                            text = "UID: ${profile?.uid?.take(10) ?: "..."}",
                            style = MaterialTheme.typography.labelSmall,
                            color = DarkGreen,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = onEditProfile,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("edit_profile_button")
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.editProfile, color = Color.White)
                    }
                }
            }

            // Location Details
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = PrimaryGreen)
                        Spacer(Modifier.width(8.dp))
                        Text("${strings.districtLabel}: ${profile?.district ?: "Murshidabad"}", style = MaterialTheme.typography.bodyMedium)
                    }
                    HorizontalDivider()
                    Text("${strings.blockLabel}: ${profile?.block?.ifBlank { "Not set" } ?: "Not set"}", style = MaterialTheme.typography.bodyMedium)
                    Text("${strings.villageTown}: ${profile?.village?.ifBlank { "Not set" } ?: "Not set"}", style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Selected Dashboards
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(strings.primaryDashboard, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val selected = profile?.selectedDashboards.orEmpty().ifEmpty { listOf("general") }
                        selected.forEach { slug ->
                            val cat = categories.find { it.slug == slug }
                            val label = when (strings.langCode) {
                                "bn" -> cat?.bengaliName ?: slug
                                else -> cat?.englishName ?: slug
                            }
                            Surface(
                                color = if (profile?.primaryDashboard == slug) PrimaryGreen else LightGreen,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = if (profile?.primaryDashboard == slug) Color.White else DarkGreen,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Settings & Logout
            Card(shape = RoundedCornerShape(18.dp)) {
                Column {
                    ProfileMenuRow(Icons.Rounded.Settings, strings.settingsTitle, onClick = onOpenSettings)
                    HorizontalDivider()
                    ProfileMenuRow(
                        Icons.AutoMirrored.Rounded.ExitToApp,
                        strings.logout,
                        tint = AccentRose,
                        onClick = onLogout,
                        modifier = Modifier.testTag("logout_button")
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileMenuRow(
    icon: ImageVector,
    label: String,
    tint: Color = PrimaryGreen,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.titleSmall, color = tint, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SettingsScreen(
    prefsState: UserPrefsState,
    appUpdateConfig: AppUpdateConfig?,
    publicSettings: PublicSettings?,
    onLanguageChange: (String) -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    var infoDialog by remember { mutableStateOf<Pair<String, String>?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        ModuleTopBar(title = strings.settingsTitle, onBack = onBack)

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Language selector
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Language, contentDescription = null, tint = PrimaryGreen)
                        Spacer(Modifier.width(10.dp))
                        Text(strings.languageSetting, style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("bn" to "বাংলা", "en" to "English", "hi" to "हिन्दी").forEach { (code, label) ->
                            FilterChip(
                                selected = prefsState.language == code,
                                onClick = { onLanguageChange(code) },
                                label = { Text(label) },
                                modifier = Modifier.testTag("lang_chip_$code")
                            )
                        }
                    }
                }
            }

            // Dark Mode & Notifications
            Card(shape = RoundedCornerShape(18.dp)) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.DarkMode, contentDescription = null, tint = PrimaryGreen)
                        Spacer(Modifier.width(12.dp))
                        Text(strings.darkMode, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Switch(
                            checked = prefsState.darkMode,
                            onCheckedChange = onDarkModeChange,
                            modifier = Modifier.testTag("dark_mode_switch")
                        )
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Notifications, contentDescription = null, tint = PrimaryGreen)
                        Spacer(Modifier.width(12.dp))
                        Text(strings.notificationSettings, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Switch(
                            checked = prefsState.notificationsEnabled,
                            onCheckedChange = onNotificationsChange
                        )
                    }
                }
            }

            // App Update Status
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(LightGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.RocketLaunch, contentDescription = null, tint = PrimaryGreen)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.appUpdate, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "Current: v1.0 • Latest: v${appUpdateConfig?.latestVersion ?: "1.0"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (!appUpdateConfig?.releaseNotes.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(appUpdateConfig?.releaseNotes ?: "", style = MaterialTheme.typography.bodySmall)
                    }
                    if (!appUpdateConfig?.updateUrl.isNullOrBlank() && (appUpdateConfig?.versionCode ?: 1L) > 1L) {
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { openExternalUrl(context, appUpdateConfig?.updateUrl ?: "") },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                        ) {
                            Text(strings.updateNow, color = Color.White)
                        }
                    }
                }
            }

            // Legal & Support
            Card(shape = RoundedCornerShape(18.dp)) {
                Column {
                    ProfileMenuRow(Icons.Rounded.PrivacyTip, strings.privacyPolicy) {
                        infoDialog = strings.privacyPolicy to (publicSettings?.privacyPolicy?.ifBlank {
                            "Murshidabad Connect authenticates users via Google Sign-In and stores profile preferences and community posts in Cloud Firestore. No passwords or binary image files are stored."
                        } ?: "Murshidabad Connect respects your privacy.")
                    }
                    HorizontalDivider()
                    ProfileMenuRow(Icons.Rounded.VerifiedUser, strings.termsConditions) {
                        infoDialog = strings.termsConditions to (publicSettings?.termsConditions?.ifBlank {
                            "Please keep community posts respectful and verify official dates on government portals."
                        } ?: "Standard community terms apply.")
                    }
                    HorizontalDivider()
                    ProfileMenuRow(Icons.Rounded.Help, strings.helpSupport) {
                        infoDialog = strings.helpSupport to "Email: ${publicSettings?.supportEmail ?: "support@slstrms.in"}\nPhone: ${publicSettings?.supportPhone ?: "112 / District Control"}"
                    }
                    HorizontalDivider()
                    ProfileMenuRow(Icons.Rounded.Info, strings.aboutApp) {
                        infoDialog = strings.aboutApp to "Murshidabad Connect v1.0\nDeveloped by Slstrms Technologies\nDedicated to the residents of Murshidabad, West Bengal, India."
                    }
                }
            }
        }
    }

    if (infoDialog != null) {
        AlertDialog(
            onDismissRequest = { infoDialog = null },
            title = { Text(infoDialog!!.first) },
            text = { Text(infoDialog!!.second) },
            confirmButton = {
                TextButton(onClick = { infoDialog = null }) { Text("OK") }
            }
        )
    }
}
