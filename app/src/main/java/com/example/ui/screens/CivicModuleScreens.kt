package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bloodtype
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.LocalAppStrings
import com.example.data.model.BloodDonorItem
import com.example.data.model.BusinessReview
import com.example.data.model.ContentItem
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorStateCard
import com.example.ui.components.ModuleTopBar
import com.example.ui.components.RemoteImageWithFallback
import com.example.ui.components.SkeletonLoadingList
import com.example.ui.components.dialPhoneNumber
import com.example.ui.components.openExternalUrl
import com.example.ui.components.shareText
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentRose
import com.example.ui.theme.AccentRoseBg
import com.example.ui.theme.DarkGreen
import com.example.ui.theme.LightGreen
import com.example.ui.theme.PrimaryGreen
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.flow.Flow
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CivicModuleScreen(
    moduleKey: String,
    primaryState: UiState<List<ContentItem>>,
    secondaryState: UiState<List<ContentItem>>? = null,
    bloodDonorsState: UiState<List<BloodDonorItem>>? = null,
    observeBusinessReviews: (String) -> Flow<List<BusinessReview>>,
    onSubmitBusinessReview: (ContentItem, Int, String, () -> Unit) -> Unit,
    onCreateBloodEntry: (String, String, String, String, String, String, String, () -> Unit) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    var search by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf("All") }
    var selectedItem by remember { mutableStateOf<ContentItem?>(null) }
    var showBloodDialog by remember { mutableStateOf(false) }

    val (screenTitle, tabs) = remember(moduleKey, strings) {
        when (moduleKey) {
            "news" -> strings.latestNews to listOf("All", "District", "Block", "State", "Country")
            "jobs" -> strings.jobsLabel to listOf("All", "Government", "Private", "Railway", "Bank", "School", "Local")
            "education" -> strings.educationLabel to listOf("All", "Schools", "Colleges", "Study Materials", "Exams", "Results", "Scholarships")
            "scholarships" -> strings.scholarshipsLabel to listOf("All", "State", "Central", "Merit")
            "healthcare" -> strings.healthcareLabel to listOf("All", "Hospitals", "Clinics", "Doctors", "Blood Donors")
            "businesses" -> strings.businessLabel to listOf("All", "Shops", "Services", "Food", "Electronics", "Medical")
            "schemes" -> strings.schemesLabel to listOf("All", "Central", "West Bengal")
            "agriculture" -> strings.agricultureLabel to listOf("All", "Farming", "Crops", "Schemes", "Market Prices", "Weather")
            "market_prices" -> strings.marketPricesLabel to listOf("All", "Vegetables", "Grains", "Spices", "Fish")
            "transport" -> strings.transportLabel to listOf("All", "Bus", "Train", "Local", "Notices")
            "tourism" -> strings.tourismLabel to listOf("All", "Historical", "Heritage", "Temples", "Travel")
            else -> strings.allServices to listOf("All")
        }
    }

    Scaffold(
        topBar = { ModuleTopBar(title = screenTitle, onBack = onBack) },
        floatingActionButton = {
            if (moduleKey == "healthcare" && selectedTab == "Blood Donors") {
                FloatingActionButton(
                    onClick = { showBloodDialog = true },
                    containerColor = AccentRose,
                    contentColor = Color.White,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add Blood Donor/Request")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search box
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text(strings.searchPlaceholder) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )

            // Filter tabs
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEach { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab) }
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            if (moduleKey == "healthcare" && selectedTab == "Blood Donors" && bloodDonorsState != null) {
                BloodDonorsList(bloodDonorsState, search, onRetry)
            } else {
                val combinedList = remember(primaryState, secondaryState, selectedTab, moduleKey) {
                    val primary = (primaryState as? UiState.Success)?.data.orEmpty()
                    val secondary = (secondaryState as? UiState.Success)?.data.orEmpty()
                    if ((moduleKey == "education" && selectedTab == "Scholarships") ||
                        (moduleKey == "agriculture" && selectedTab == "Market Prices")
                    ) {
                        secondary
                    } else if (selectedTab == "All" && secondary.isNotEmpty()) {
                        primary + secondary
                    } else {
                        primary
                    }
                }

                when (primaryState) {
                    is UiState.Loading -> SkeletonLoadingList()
                    is UiState.Error -> ErrorStateCard(primaryState.message, onRetry)
                    is UiState.Success -> {
                        val filtered = combinedList.filter { item ->
                            val tabOk = selectedTab == "All" ||
                                (moduleKey == "education" && selectedTab == "Scholarships") ||
                                (moduleKey == "agriculture" && selectedTab == "Market Prices") ||
                                item.category.equals(selectedTab, ignoreCase = true) ||
                                item.subCategory.equals(selectedTab, ignoreCase = true)
                            val searchOk = search.isBlank() ||
                                item.title.contains(search, ignoreCase = true) ||
                                item.description.contains(search, ignoreCase = true) ||
                                item.location.contains(search, ignoreCase = true) ||
                                item.block.contains(search, ignoreCase = true)
                            tabOk && searchOk
                        }

                        if (filtered.isEmpty()) {
                            EmptyStateCard()
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(
                                    items = filtered,
                                    key = { idx, item -> "${moduleKey}_${item.category}_${item.id}_$idx" }
                                ) { _, item ->
                                    ContentCard(
                                        item = item,
                                        moduleKey = moduleKey,
                                        onClick = { selectedItem = item }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedItem != null) {
        val current = selectedItem!!
        ModalBottomSheet(onDismissRequest = { selectedItem = null }) {
            ContentDetailSheet(
                item = current,
                moduleKey = moduleKey,
                allItems = (primaryState as? UiState.Success)?.data.orEmpty(),
                observeBusinessReviews = observeBusinessReviews,
                onSubmitBusinessReview = onSubmitBusinessReview,
                onSelectRelated = { selectedItem = it },
                onClose = { selectedItem = null }
            )
        }
    }

    if (showBloodDialog) {
        AddBloodEntryDialog(
            onDismiss = { showBloodDialog = false },
            onSubmit = { title, desc, group, cat, loc, phone, avail ->
                onCreateBloodEntry(title, desc, group, cat, loc, phone, avail) {
                    showBloodDialog = false
                }
            }
        )
    }
}

@Composable
private fun ContentCard(
    item: ContentItem,
    moduleKey: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("content_card_${item.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            if (item.imageUrl.isNotBlank()) {
                RemoteImageWithFallback(
                    imageUrl = item.imageUrl,
                    contentDescription = item.title,
                    height = 155.dp,
                    cornerRadius = 18.dp
                )
            }
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(color = LightGreen, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = item.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = DarkGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    if (item.badgeText.isNotBlank()) {
                        Text(item.badgeText, style = MaterialTheme.typography.labelSmall, color = AccentAmber, fontWeight = FontWeight.Bold)
                    } else if (moduleKey == "businesses" && item.ratingCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Star, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(
                                "${String.format(Locale.US, "%.1f", item.ratingAvg)} (${item.ratingCount})",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (item.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (item.location.isNotBlank() || item.metaPrimary.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (item.location.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(3.dp))
                                Text(item.location, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (item.metaPrimary.isNotBlank()) {
                            Text("• ${item.metaPrimary}", style = MaterialTheme.typography.labelSmall, color = PrimaryGreen)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContentDetailSheet(
    item: ContentItem,
    moduleKey: String,
    allItems: List<ContentItem>,
    observeBusinessReviews: (String) -> Flow<List<BusinessReview>>,
    onSubmitBusinessReview: (ContentItem, Int, String, () -> Unit) -> Unit,
    onSelectRelated: (ContentItem) -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        if (item.imageUrl.isNotBlank()) {
            RemoteImageWithFallback(
                imageUrl = item.imageUrl,
                contentDescription = item.title,
                height = 190.dp,
                cornerRadius = 18.dp
            )
            Spacer(Modifier.height(12.dp))
        }

        Surface(color = LightGreen, shape = RoundedCornerShape(8.dp)) {
            Text(
                item.category,
                style = MaterialTheme.typography.labelMedium,
                color = DarkGreen,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(item.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(item.description, style = MaterialTheme.typography.bodyMedium)

        if (item.metaPrimary.isNotBlank() || item.metaSecondary.isNotBlank() || item.metaTertiary.isNotBlank() || item.location.isNotBlank()) {
            Spacer(Modifier.height(14.dp))
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (item.location.isNotBlank()) DetailRow("Location", item.location)
                    if (item.block.isNotBlank()) DetailRow("Block", item.block)
                    if (item.metaPrimary.isNotBlank()) DetailRow(
                        when (moduleKey) {
                            "jobs" -> "Salary / Eligibility"
                            "schemes" -> "Benefits"
                            "market_prices" -> "Price"
                            else -> "Details"
                        },
                        item.metaPrimary
                    )
                    if (item.metaSecondary.isNotBlank()) DetailRow(
                        when (moduleKey) {
                            "jobs" -> "Qualification"
                            "schemes" -> "Eligibility"
                            else -> "Info"
                        },
                        item.metaSecondary
                    )
                    if (item.metaTertiary.isNotBlank()) DetailRow(
                        when (moduleKey) {
                            "jobs" -> "Last Date"
                            "schemes" -> "Required Documents"
                            else -> "Schedule / Timing"
                        },
                        item.metaTertiary
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            if (item.actionUrl.isNotBlank()) {
                Button(
                    onClick = { openExternalUrl(context, item.actionUrl) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.applyNow, color = Color.White)
                }
            }
            if (item.contact.isNotBlank()) {
                Button(
                    onClick = { dialPhoneNumber(context, item.contact) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkGreen)
                ) {
                    Icon(Icons.Rounded.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.callNow, color = Color.White)
                }
            }
            IconButton(onClick = { shareText(context, item.title, item.description) }) {
                Icon(Icons.Rounded.Share, contentDescription = strings.share, tint = PrimaryGreen)
            }
        }

        // Business Ratings & Reviews
        if (moduleKey == "businesses") {
            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))
            val reviews by observeBusinessReviews(item.id).collectAsStateWithLifecycle(initialValue = emptyList())
            var rating by remember { mutableIntStateOf(5) }
            var comment by remember { mutableStateOf("") }

            Text("Ratings & Reviews (${reviews.size})", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                (1..5).forEach { star ->
                    IconButton(onClick = { rating = star }, modifier = Modifier.size(34.dp)) {
                        Icon(
                            Icons.Rounded.Star,
                            contentDescription = "$star star",
                            tint = if (star <= rating) AccentAmber else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    placeholder = { Text("Write your review...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Button(
                    onClick = {
                        if (comment.isNotBlank()) {
                            onSubmitBusinessReview(item, rating, comment) { comment = "" }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("Post", color = Color.White)
                }
            }
            Spacer(Modifier.height(10.dp))
            reviews.forEach { rev ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(rev.authorName, style = MaterialTheme.typography.titleSmall)
                            Text("★ ${rev.rating}", color = AccentAmber, fontWeight = FontWeight.Bold)
                        }
                        Text(rev.comment, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Related News
        if (moduleKey == "news") {
            val related = allItems.filter { it.id != item.id && it.category == item.category }.take(3)
            if (related.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))
                Text("Related News", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                related.forEach { rel ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onSelectRelated(rel) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(rel.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) {
            Text(strings.cancel)
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BloodDonorsList(
    state: UiState<List<BloodDonorItem>>,
    search: String,
    onRetry: () -> Unit
) {
    val context = LocalContext.current
    when (state) {
        is UiState.Loading -> SkeletonLoadingList()
        is UiState.Error -> ErrorStateCard(state.message, onRetry)
        is UiState.Success -> {
            val filtered = state.data.filter {
                search.isBlank() ||
                    it.bloodGroup.contains(search, ignoreCase = true) ||
                    it.location.contains(search, ignoreCase = true) ||
                    it.title.contains(search, ignoreCase = true)
            }
            if (filtered.isEmpty()) {
                EmptyStateCard(title = "No blood donors or requests yet", icon = Icons.Rounded.Bloodtype)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(
                        items = filtered,
                        key = { idx, donor -> "donor_${donor.id}_$idx" }
                    ) { _, donor ->
                        Card(shape = RoundedCornerShape(16.dp)) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = AccentRoseBg,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, AccentRose.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        donor.bloodGroup,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = AccentRose,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${donor.category}: ${donor.title}", style = MaterialTheme.typography.titleSmall)
                                    Text(donor.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (donor.availability.isNotBlank()) {
                                        Text(donor.availability, style = MaterialTheme.typography.labelSmall, color = PrimaryGreen)
                                    }
                                }
                                IconButton(onClick = { dialPhoneNumber(context, donor.contactPhone) }) {
                                    Icon(Icons.Rounded.Phone, contentDescription = "Call", tint = PrimaryGreen)
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
private fun AddBloodEntryDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var bloodGroup by remember { mutableStateOf("O+") }
    var category by remember { mutableStateOf("DONOR") }
    var location by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var availability by remember { mutableStateOf("Available") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Blood Donor / Request") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = category == "DONOR", onClick = { category = "DONOR" }, label = { Text("Donor") })
                    FilterChip(selected = category == "REQUEST", onClick = { category = "REQUEST" }, label = { Text("Request") })
                }
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-").forEach { bg ->
                        FilterChip(selected = bloodGroup == bg, onClick = { bloodGroup = bg }, label = { Text(bg) })
                    }
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Name / Patient Name") }, singleLine = true)
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Hospital / Block / Village") }, singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Contact Phone") }, singleLine = true)
                OutlinedTextField(value = availability, onValueChange = { availability = it }, label = { Text("Availability / Urgency") }, singleLine = true)
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Notes") })
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(title, description, bloodGroup, category, location, phone, availability) },
                enabled = title.isNotBlank() && location.isNotBlank() && phone.length >= 5,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text("Submit", color = Color.White)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
