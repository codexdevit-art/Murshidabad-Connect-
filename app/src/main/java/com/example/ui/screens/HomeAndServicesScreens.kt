package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Agriculture
import androidx.compose.material.icons.rounded.Bloodtype
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Emergency
import androidx.compose.material.icons.rounded.FindInPage
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.LocalPolice
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tour
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Woman
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.LocalAppStrings
import com.example.data.model.ContentItem
import com.example.data.model.DashboardCategory
import com.example.data.model.UserProfile
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ModuleTopBar
import com.example.ui.components.MurshidabadLogoBadge
import com.example.ui.components.RemoteImageWithFallback
import com.example.ui.components.SkeletonLoadingList
import com.example.ui.components.UserAvatar
import com.example.ui.components.dialPhoneNumber
import com.example.ui.components.openExternalUrl
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
import com.example.ui.viewmodel.UiState

data class ServiceTile(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val tint: Color,
    val bgTint: Color
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    profile: UserProfile?,
    categories: List<DashboardCategory>,
    bannersState: UiState<List<ContentItem>>,
    announcementsState: UiState<List<ContentItem>>,
    newsState: UiState<List<ContentItem>>,
    jobsState: UiState<List<ContentItem>>,
    educationState: UiState<List<ContentItem>>,
    healthcareState: UiState<List<ContentItem>>,
    businessesState: UiState<List<ContentItem>>,
    tourismState: UiState<List<ContentItem>>,
    adsState: UiState<List<ContentItem>>,
    searchQuery: String,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onSearchChange: (String) -> Unit,
    onSwitchDashboard: (String) -> Unit,
    onNavigate: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    var showDashboardSwitcher by remember { mutableStateOf(false) }

    val quickServices = remember(strings) {
        listOf(
            ServiceTile("module/news", strings.latestNews, Icons.Rounded.Newspaper, PrimaryGreen, LightGreen),
            ServiceTile("module/jobs", strings.jobsLabel, Icons.Rounded.Work, AccentBlue, AccentBlueBg),
            ServiceTile("module/education", strings.educationLabel, Icons.Rounded.School, AccentPurple, AccentPurpleBg),
            ServiceTile("module/healthcare", strings.healthcareLabel, Icons.Rounded.LocalHospital, AccentRose, AccentRoseBg),
            ServiceTile("module/businesses", strings.businessLabel, Icons.Rounded.Storefront, AccentTeal, AccentTealBg),
            ServiceTile("module/schemes", strings.schemesLabel, Icons.Rounded.AccountBalance, AccentAmber, AccentAmberBg),
            ServiceTile("module/agriculture", strings.agricultureLabel, Icons.Rounded.Agriculture, PrimaryGreen, LightGreen),
            ServiceTile("module/tourism", strings.tourismLabel, Icons.Rounded.Tour, AccentBlue, AccentBlueBg)
        )
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 110.dp)
        ) {
            // Emerald Curved Top Header
            item {
                Surface(
                    color = PrimaryGreen,
                    shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                    shadowElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MurshidabadLogoBadge(size = 42.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Murshidabad Connect",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = profile?.let {
                                        val loc = listOf(it.village, it.block, it.district).filter { s -> s.isNotBlank() }.joinToString(", ")
                                        "${it.name} • ${loc.ifBlank { "Murshidabad" }}"
                                    } ?: strings.tagline,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.88f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { onNavigate("alerts") }) {
                                Icon(Icons.Rounded.Notifications, contentDescription = strings.navAlerts, tint = Color.White)
                            }
                            UserAvatar(
                                photoUrl = profile?.photoUrl,
                                name = profile?.name ?: "M",
                                size = 40.dp,
                                modifier = Modifier
                                    .clickable { onNavigate("profile") }
                                    .testTag("home_profile_avatar")
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        // Active Dashboard Switcher Chip
                        val activeCat = categories.find { it.slug == profile?.primaryDashboard } ?: categories.lastOrNull()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { showDashboardSwitcher = true }
                                    .testTag("dashboard_switcher_chip")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = when (strings.langCode) {
                                            "bn" -> activeCat?.bengaliName ?: "সাধারণ নাগরিক"
                                            "hi" -> activeCat?.hindiName ?: "आम नागरिक"
                                            else -> activeCat?.englishName ?: "General Citizen"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Icon(
                                        Icons.Rounded.SwapHoriz,
                                        contentDescription = "Switch Dashboard",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = AccentRose,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { onNavigate("help") }
                                    .testTag("home_emergency_chip")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Emergency, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(strings.emergencyLabel, style = MaterialTheme.typography.labelMedium, color = Color.White)
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchChange,
                            placeholder = { Text(strings.searchPlaceholder, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = PrimaryGreen) },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { onSearchChange("") }) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("home_search_input")
                        )
                    }
                }
            }

            // Admin Banners Carousel
            item {
                val banners = (bannersState as? UiState.Success)?.data.orEmpty()
                if (banners.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(banners, key = { it.id }) { banner ->
                            Card(
                                modifier = Modifier
                                    .width(308.dp)
                                    .clickable {
                                        if (banner.actionUrl.isNotBlank()) openExternalUrl(context, banner.actionUrl)
                                    },
                                shape = RoundedCornerShape(20.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                            ) {
                                Box(modifier = Modifier.height(155.dp)) {
                                    RemoteImageWithFallback(
                                        imageUrl = banner.imageUrl,
                                        contentDescription = banner.title,
                                        height = 155.dp,
                                        cornerRadius = 20.dp
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                                                )
                                            )
                                            .padding(14.dp),
                                        contentAlignment = Alignment.BottomStart
                                    ) {
                                        Column {
                                            Text(
                                                text = banner.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (banner.description.isNotBlank()) {
                                                Text(
                                                    text = banner.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.White.copy(alpha = 0.9f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Important Announcements Strip
            item {
                val announcements = (announcementsState as? UiState.Success)?.data.orEmpty()
                if (announcements.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clickable { onNavigate("alerts") },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = AccentAmberBg),
                        border = BorderStroke(1.dp, AccentAmber.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Campaign, contentDescription = null, tint = AccentAmber)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = announcements.first().title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color(0xFF78350F),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = announcements.first().description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF92400E),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Quick Services Grid
            item {
                Spacer(Modifier.height(18.dp))
                SectionHeader(
                    title = strings.quickServices,
                    actionLabel = strings.seeAll,
                    onAction = { onNavigate("services") }
                )
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachRow = 4
                ) {
                    quickServices.forEach { tile ->
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onNavigate(tile.route) }
                                .testTag("quick_tile_${tile.route}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(tile.bgTint),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(tile.icon, contentDescription = tile.label, tint = tile.tint, modifier = Modifier.size(22.dp))
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = tile.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Community & Lost/Found Shortcuts
            item {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigate("community") }
                            .testTag("home_community_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = LightGreen)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Forum, contentDescription = null, tint = PrimaryGreen)
                            Spacer(Modifier.width(8.dp))
                            Text(strings.communityLabel, style = MaterialTheme.typography.titleSmall, color = DarkGreen)
                        }
                    }
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigate("lost_found") }
                            .testTag("home_lost_found_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = AccentBlueBg)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.FindInPage, contentDescription = null, tint = AccentBlue)
                            Spacer(Modifier.width(8.dp))
                            Text(strings.lostFoundLabel, style = MaterialTheme.typography.titleSmall, color = AccentBlue)
                        }
                    }
                }
            }

            // Latest News Section
            item {
                Spacer(Modifier.height(18.dp))
                SectionHeader(
                    title = strings.latestNews,
                    actionLabel = strings.seeAll,
                    onAction = { onNavigate("module/news") }
                )
                HomeSectionContent(
                    state = newsState,
                    searchQuery = searchQuery,
                    onItemClick = { onNavigate("module/news") }
                )
            }

            // Jobs Section
            item {
                Spacer(Modifier.height(18.dp))
                SectionHeader(
                    title = strings.jobsLabel,
                    actionLabel = strings.seeAll,
                    onAction = { onNavigate("module/jobs") }
                )
                HomeHorizontalCards(
                    state = jobsState,
                    searchQuery = searchQuery,
                    onItemClick = { onNavigate("module/jobs") }
                )
            }

            // Education Section
            item {
                Spacer(Modifier.height(18.dp))
                SectionHeader(
                    title = strings.educationLabel,
                    actionLabel = strings.seeAll,
                    onAction = { onNavigate("module/education") }
                )
                HomeHorizontalCards(
                    state = educationState,
                    searchQuery = searchQuery,
                    onItemClick = { onNavigate("module/education") }
                )
            }

            // Healthcare & Businesses & Tourism
            item {
                Spacer(Modifier.height(18.dp))
                SectionHeader(
                    title = strings.healthcareLabel,
                    actionLabel = strings.seeAll,
                    onAction = { onNavigate("module/healthcare") }
                )
                HomeHorizontalCards(
                    state = healthcareState,
                    searchQuery = searchQuery,
                    onItemClick = { onNavigate("module/healthcare") }
                )
            }

            item {
                Spacer(Modifier.height(18.dp))
                SectionHeader(
                    title = strings.businessLabel,
                    actionLabel = strings.seeAll,
                    onAction = { onNavigate("module/businesses") }
                )
                HomeHorizontalCards(
                    state = businessesState,
                    searchQuery = searchQuery,
                    onItemClick = { onNavigate("module/businesses") }
                )
            }

            item {
                Spacer(Modifier.height(18.dp))
                SectionHeader(
                    title = strings.tourismLabel,
                    actionLabel = strings.seeAll,
                    onAction = { onNavigate("module/tourism") }
                )
                HomeHorizontalCards(
                    state = tourismState,
                    searchQuery = searchQuery,
                    onItemClick = { onNavigate("module/tourism") }
                )
            }

            // Sponsored / Local Ads
            item {
                val ads = (adsState as? UiState.Success)?.data.orEmpty()
                if (ads.isNotEmpty()) {
                    Spacer(Modifier.height(18.dp))
                    SectionHeader(title = strings.recentUpdates, actionLabel = "", onAction = {})
                    ads.take(2).forEach { ad ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clickable {
                                    if (ad.actionUrl.isNotBlank()) openExternalUrl(context, ad.actionUrl)
                                },
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                RemoteImageWithFallback(
                                    imageUrl = ad.imageUrl,
                                    contentDescription = ad.title,
                                    height = 70.dp,
                                    cornerRadius = 12.dp,
                                    modifier = Modifier.width(96.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ad.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(ad.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDashboardSwitcher) {
        AlertDialog(
            onDismissRequest = { showDashboardSwitcher = false },
            title = { Text(strings.primaryDashboard) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { cat ->
                        val isCurrent = profile?.primaryDashboard == cat.slug
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onSwitchDashboard(cat.slug)
                                    showDashboardSwitcher = false
                                },
                            color = if (isCurrent) LightGreen else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val (_, tint, icon) = categoryStyleFor(cat.slug)
                                Icon(icon, contentDescription = null, tint = tint)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = when (strings.langCode) {
                                        "bn" -> "${cat.bengaliName} (${cat.englishName})"
                                        else -> cat.englishName
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDashboardSwitcher = false }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String, actionLabel: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (actionLabel.isNotBlank()) {
            TextButton(onClick = onAction) {
                Text(actionLabel, color = PrimaryGreen, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(2.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun HomeSectionContent(
    state: UiState<List<ContentItem>>,
    searchQuery: String,
    onItemClick: (ContentItem) -> Unit
) {
    when (state) {
        is UiState.Loading -> SkeletonLoadingList(count = 1)
        is UiState.Error -> EmptyStateCard(title = state.message)
        is UiState.Success -> {
            val filtered = state.data.filter {
                searchQuery.isBlank() ||
                    it.title.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true) ||
                    it.category.contains(searchQuery, ignoreCase = true)
            }
            if (filtered.isEmpty()) {
                EmptyStateCard()
            } else {
                filtered.take(3).forEach { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 5.dp)
                            .clickable { onItemClick(item) },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column {
                            if (item.imageUrl.isNotBlank()) {
                                RemoteImageWithFallback(
                                    imageUrl = item.imageUrl,
                                    contentDescription = item.title,
                                    height = 150.dp,
                                    cornerRadius = 18.dp
                                )
                            }
                            Column(modifier = Modifier.padding(14.dp)) {
                                Surface(
                                    color = LightGreen,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = item.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DarkGreen,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    item.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHorizontalCards(
    state: UiState<List<ContentItem>>,
    searchQuery: String,
    onItemClick: (ContentItem) -> Unit
) {
    when (state) {
        is UiState.Loading -> {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                repeat(2) {
                    Card(modifier = Modifier.width(220.dp).height(120.dp), shape = RoundedCornerShape(16.dp)) {}
                }
            }
        }
        is UiState.Error -> EmptyStateCard(title = state.message)
        is UiState.Success -> {
            val filtered = state.data.filter {
                searchQuery.isBlank() ||
                    it.title.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true)
            }
            if (filtered.isEmpty()) {
                EmptyStateCard()
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtered.take(8), key = { it.id }) { item ->
                        Card(
                            modifier = Modifier
                                .width(230.dp)
                                .clickable { onItemClick(item) },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (item.imageUrl.isNotBlank()) {
                                    RemoteImageWithFallback(
                                        imageUrl = item.imageUrl,
                                        contentDescription = item.title,
                                        height = 95.dp,
                                        cornerRadius = 12.dp
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }
                                Text(
                                    item.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PrimaryGreen
                                )
                                Text(
                                    item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (item.location.isNotBlank()) {
                                    Text(
                                        item.location,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ServicesScreen(onNavigate: (String) -> Unit) {
    val strings = LocalAppStrings.current
    var query by remember { mutableStateOf("") }

    val allModules = remember(strings) {
        listOf(
            ServiceTile("module/schemes", strings.schemesLabel, Icons.Rounded.AccountBalance, AccentBlue, AccentBlueBg),
            ServiceTile("module/agriculture", strings.agricultureLabel, Icons.Rounded.Agriculture, PrimaryGreen, LightGreen),
            ServiceTile("module/market_prices", strings.marketPricesLabel, Icons.Rounded.TrendingUp, AccentAmber, AccentAmberBg),
            ServiceTile("module/healthcare", strings.healthcareLabel, Icons.Rounded.LocalHospital, AccentRose, AccentRoseBg),
            ServiceTile("module/education", strings.educationLabel, Icons.Rounded.School, AccentPurple, AccentPurpleBg),
            ServiceTile("module/jobs", strings.jobsLabel, Icons.Rounded.Work, AccentTeal, AccentTealBg),
            ServiceTile("module/businesses", strings.businessLabel, Icons.Rounded.Storefront, PrimaryGreen, LightGreen),
            ServiceTile("module/news", strings.latestNews, Icons.Rounded.Newspaper, AccentBlue, AccentBlueBg),
            ServiceTile("module/transport", strings.transportLabel, Icons.Rounded.DirectionsBus, AccentAmber, AccentAmberBg),
            ServiceTile("module/tourism", strings.tourismLabel, Icons.Rounded.Tour, AccentTeal, AccentTealBg),
            ServiceTile("community", strings.communityLabel, Icons.Rounded.Forum, PrimaryGreen, LightGreen),
            ServiceTile("lost_found", strings.lostFoundLabel, Icons.Rounded.FindInPage, AccentPurple, AccentPurpleBg),
            ServiceTile("help", strings.emergencyLabel, Icons.Rounded.Emergency, AccentRose, AccentRoseBg),
            ServiceTile("module/scholarships", strings.scholarshipsLabel, Icons.Rounded.WorkspacePremium, AccentBlue, AccentBlueBg)
        )
    }

    val filtered = allModules.filter {
        query.isBlank() || it.label.contains(query, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ModuleTopBar(title = strings.allServices)
        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(strings.searchPlaceholder) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 110.dp)
            ) {
                items(filtered, key = { it.route }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate(item.route) }
                            .testTag("service_row_${item.route}"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(item.bgTint),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(item.icon, contentDescription = item.label, tint = item.tint)
                            }
                            Spacer(Modifier.width(14.dp))
                            Text(item.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HelpAndEmergencyScreen(
    emergencyState: UiState<List<ContentItem>>,
    onNavigateSettings: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current

    val standardHelplines = remember {
        listOf(
            Triple("Police / পুলিশ", "100", Icons.Rounded.LocalPolice),
            Triple("Ambulance / অ্যাম্বুলেন্স", "102", Icons.Rounded.LocalHospital),
            Triple("Fire Service / ফায়ার সার্ভিস", "101", Icons.Rounded.LocalFireDepartment),
            Triple("Women Helpline / নারী সহায়তা", "1091", Icons.Rounded.Woman),
            Triple("National Emergency", "112", Icons.Rounded.Emergency)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ModuleTopBar(title = "${strings.emergencyLabel} & ${strings.helpSupport}")
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 110.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = AccentRoseBg),
                    border = BorderStroke(1.dp, AccentRose.copy(alpha = 0.35f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Emergency, contentDescription = null, tint = AccentRose, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = strings.emergencyNumbers,
                            style = MaterialTheme.typography.titleMedium,
                            color = AccentRose,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            items(standardHelplines) { (title, number, icon) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { dialPhoneNumber(context, number) },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(AccentRoseBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = null, tint = AccentRose)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, style = MaterialTheme.typography.titleSmall)
                            Text(number, style = MaterialTheme.typography.titleMedium, color = AccentRose, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { dialPhoneNumber(context, number) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.callNow, color = Color.White)
                        }
                    }
                }
            }

            // Admin-managed Firestore emergency contacts
            val firestoreContacts = (emergencyState as? UiState.Success)?.data.orEmpty()
            if (firestoreContacts.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(6.dp))
                    Text("District Emergency Directory", style = MaterialTheme.typography.titleMedium)
                }
                items(firestoreContacts, key = { it.id }) { contactItem ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (contactItem.contact.isNotBlank()) dialPhoneNumber(context, contactItem.contact)
                            },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(contactItem.title, style = MaterialTheme.typography.titleSmall)
                                Text(contactItem.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (contactItem.contact.isNotBlank()) {
                                    Text(contactItem.contact, style = MaterialTheme.typography.labelLarge, color = PrimaryGreen)
                                }
                            }
                            if (contactItem.contact.isNotBlank()) {
                                IconButton(onClick = { dialPhoneNumber(context, contactItem.contact) }) {
                                    Icon(Icons.Rounded.Phone, contentDescription = strings.callNow, tint = PrimaryGreen)
                                }
                            }
                        }
                    }
                }
            }

            // Help & Support Card
            item {
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = LightGreen)
                ) {
                    Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.SupportAgent, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(strings.helpSupport, style = MaterialTheme.typography.titleMedium, color = DarkGreen)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Need help with Murshidabad Connect, reporting inaccurate listings, or updating local info?",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = DarkGreen
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onNavigateSettings,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                        ) {
                            Text(strings.settingsTitle, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
