package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FindInPage
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Share
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.LocalAppStrings
import com.example.data.model.CommunityComment
import com.example.data.model.CommunityPost
import com.example.data.model.LostFoundItem
import com.example.data.model.MurshidabadConstants
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorStateCard
import com.example.ui.components.ImageUrlInputField
import com.example.ui.components.ModuleTopBar
import com.example.ui.components.RemoteImageWithFallback
import com.example.ui.components.SkeletonLoadingList
import com.example.ui.components.UserAvatar
import com.example.ui.components.dialPhoneNumber
import com.example.ui.components.shareText
import com.example.ui.theme.AccentBlueBg
import com.example.ui.theme.AccentRose
import com.example.ui.theme.DarkGreen
import com.example.ui.theme.LightGreen
import com.example.ui.theme.PrimaryGreen
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    currentUserId: String,
    postsState: UiState<List<CommunityPost>>,
    observeComments: (String) -> Flow<List<CommunityComment>>,
    onCreatePost: (String, String, String, String, () -> Unit) -> Unit,
    onToggleLike: (CommunityPost) -> Unit,
    onAddComment: (String, String, () -> Unit) -> Unit,
    onSubmitReport: (String, String, String, String, () -> Unit) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var commentsPost by remember { mutableStateOf<CommunityPost?>(null) }
    var reportPost by remember { mutableStateOf<CommunityPost?>(null) }

    Scaffold(
        topBar = { ModuleTopBar(title = strings.communityLabel, onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = PrimaryGreen,
                contentColor = Color.White,
                modifier = Modifier.navigationBarsPadding().testTag("create_post_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = strings.createPost)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (postsState) {
                is UiState.Loading -> SkeletonLoadingList()
                is UiState.Error -> ErrorStateCard(postsState.message, onRetry)
                is UiState.Success -> {
                    if (postsState.data.isEmpty()) {
                        EmptyStateCard(title = "No community posts yet", subtitle = "Tap + to share a local update with Murshidabad residents.")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(postsState.data, key = { it.id }) { post ->
                                val isLiked = post.likedBy.contains(currentUserId)
                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            UserAvatar(photoUrl = post.authorPhotoUrl, name = post.authorName, size = 40.dp)
                                            Spacer(Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(post.authorName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                Text(
                                                    listOf(post.block, post.category).filter { it.isNotBlank() }.joinToString(" • "),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(onClick = { reportPost = post }) {
                                                Icon(Icons.Rounded.Flag, contentDescription = strings.report, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Spacer(Modifier.height(10.dp))
                                        Text(post.title, style = MaterialTheme.typography.titleMedium)
                                        Spacer(Modifier.height(4.dp))
                                        Text(post.description, style = MaterialTheme.typography.bodyMedium)
                                        if (post.imageUrl.isNotBlank()) {
                                            Spacer(Modifier.height(10.dp))
                                            RemoteImageWithFallback(
                                                imageUrl = post.imageUrl,
                                                contentDescription = post.title,
                                                height = 180.dp,
                                                cornerRadius = 14.dp
                                            )
                                        }
                                        Spacer(Modifier.height(10.dp))
                                        HorizontalDivider()
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(onClick = { onToggleLike(post) }) {
                                                    Icon(
                                                        imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                                                        contentDescription = "Like",
                                                        tint = if (isLiked) AccentRose else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Text("${post.likesCount}", style = MaterialTheme.typography.labelMedium)
                                                Spacer(Modifier.width(12.dp))
                                                IconButton(onClick = { commentsPost = post }) {
                                                    Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = "Comments")
                                                }
                                                Text("${post.commentsCount}", style = MaterialTheme.typography.labelMedium)
                                            }
                                            IconButton(onClick = { shareText(context, post.title, post.description) }) {
                                                Icon(Icons.Rounded.Share, contentDescription = strings.share, tint = PrimaryGreen)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var title by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var imageUrl by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Civic") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(strings.createPost) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Civic", "Local Help", "Events", "Discussion", "Agriculture").forEach { cat ->
                            FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat) })
                        }
                    }
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Write your message...") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                    ImageUrlInputField(imageUrl = imageUrl, onImageUrlChange = { imageUrl = it })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreatePost(title, desc, imageUrl, category) { showCreateDialog = false }
                    },
                    enabled = title.isNotBlank() && desc.isNotBlank() && MurshidabadConstants.isValidImageUrl(imageUrl),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("Publish", color = Color.White)
                }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text(strings.cancel) } }
        )
    }

    if (commentsPost != null) {
        val post = commentsPost!!
        val comments by observeComments(post.id).collectAsStateWithLifecycle(initialValue = emptyList())
        var commentText by remember { mutableStateOf("") }
        ModalBottomSheet(onDismissRequest = { commentsPost = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 8.dp)
                    .navigationBarsPadding()
            ) {
                Text("Comments (${comments.size})", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                comments.forEach { c ->
                    Row(modifier = Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
                        UserAvatar(photoUrl = c.authorPhotoUrl, name = c.authorName, size = 32.dp)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(c.authorName, style = MaterialTheme.typography.labelLarge)
                            Text(c.text, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Write a comment...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                onAddComment(post.id, commentText) { commentText = "" }
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send", tint = PrimaryGreen)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (reportPost != null) {
        val post = reportPost!!
        var reason by remember { mutableStateOf("Spam or misleading") }
        var details by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { reportPost = null },
            title = { Text("Report Post") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Reason") }, singleLine = true)
                    OutlinedTextField(value = details, onValueChange = { details = it }, label = { Text("Additional Details") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSubmitReport("community_posts", post.id, reason, details) { reportPost = null }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRose)
                ) {
                    Text(strings.report, color = Color.White)
                }
            },
            dismissButton = { TextButton(onClick = { reportPost = null }) { Text(strings.cancel) } }
        )
    }
}

@Composable
fun LostFoundScreen(
    itemsState: UiState<List<LostFoundItem>>,
    onCreateItem: (String, String, String, String, String, String, String, () -> Unit) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf("ALL") }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { ModuleTopBar(title = strings.lostFoundLabel, onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryGreen,
                contentColor = Color.White,
                modifier = Modifier.navigationBarsPadding().testTag("report_lost_found_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = strings.reportItem)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = selectedType == "ALL", onClick = { selectedType = "ALL" }, label = { Text("All") })
                FilterChip(selected = selectedType == "LOST", onClick = { selectedType = "LOST" }, label = { Text("Lost / হারানো") })
                FilterChip(selected = selectedType == "FOUND", onClick = { selectedType = "FOUND" }, label = { Text("Found / পাওয়া") })
            }

            when (itemsState) {
                is UiState.Loading -> SkeletonLoadingList()
                is UiState.Error -> ErrorStateCard(itemsState.message, onRetry)
                is UiState.Success -> {
                    val filtered = itemsState.data.filter { selectedType == "ALL" || it.category == selectedType }
                    if (filtered.isEmpty()) {
                        EmptyStateCard(title = "No lost or found items reported", icon = Icons.Rounded.FindInPage)
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 96.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filtered, key = { it.id }) { item ->
                                Card(shape = RoundedCornerShape(18.dp)) {
                                    Column {
                                        if (item.imageUrl.isNotBlank()) {
                                            RemoteImageWithFallback(
                                                imageUrl = item.imageUrl,
                                                contentDescription = item.title,
                                                height = 160.dp,
                                                cornerRadius = 18.dp
                                            )
                                        }
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                Surface(
                                                    color = if (item.category == "LOST") AccentRose.copy(alpha = 0.12f) else LightGreen,
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(
                                                        text = item.category,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (item.category == "LOST") AccentRose else DarkGreen,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                                if (item.dateText.isNotBlank()) {
                                                    Text(item.dateText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                            Spacer(Modifier.height(6.dp))
                                            Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Spacer(Modifier.height(4.dp))
                                            Text(item.description, style = MaterialTheme.typography.bodySmall)
                                            Spacer(Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(item.location, style = MaterialTheme.typography.labelMedium)
                                                }
                                                TextButton(onClick = { dialPhoneNumber(context, item.contactInfo) }) {
                                                    Icon(Icons.Rounded.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(item.contactInfo)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var imageUrl by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("LOST") }
        var location by remember { mutableStateOf("") }
        var contact by remember { mutableStateOf("") }
        var dateText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(strings.reportItem) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = category == "LOST", onClick = { category = "LOST" }, label = { Text("Lost") })
                        FilterChip(selected = category == "FOUND", onClick = { category = "FOUND" }, label = { Text("Found") })
                    }
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Item Title") }, singleLine = true)
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, minLines = 2)
                    OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location / Block") }, singleLine = true)
                    OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text("Contact Phone / Info") }, singleLine = true)
                    OutlinedTextField(value = dateText, onValueChange = { dateText = it }, label = { Text("Date (e.g. 26 Nov 2026)") }, singleLine = true)
                    ImageUrlInputField(imageUrl = imageUrl, onImageUrlChange = { imageUrl = it })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateItem(title, desc, imageUrl, category, location, contact, dateText) { showAddDialog = false }
                    },
                    enabled = title.isNotBlank() && desc.isNotBlank() && location.isNotBlank() && contact.length >= 3 && MurshidabadConstants.isValidImageUrl(imageUrl),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("Submit", color = Color.White)
                }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text(strings.cancel) } }
        )
    }
}
