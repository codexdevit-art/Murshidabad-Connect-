package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserPreferences
import com.example.data.local.UserPrefsState
import com.example.data.model.AppUpdateConfig
import com.example.data.model.BloodDonorItem
import com.example.data.model.BusinessReview
import com.example.data.model.CommunityComment
import com.example.data.model.CommunityPost
import com.example.data.model.ContentItem
import com.example.data.model.DashboardCategory
import com.example.data.model.LostFoundItem
import com.example.data.model.MurshidabadConstants
import com.example.data.model.NotificationItem
import com.example.data.model.PublicSettings
import com.example.data.model.UserProfile
import com.example.data.repository.MurshidabadRepository
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(
    private val repository: MurshidabadRepository,
    private val preferences: UserPreferences,
    private val currentUserId: String,
    private val firebaseUser: FirebaseUser?
) : ViewModel() {

    private val refreshTrigger = MutableStateFlow(0)

    private val _needsOnboarding = MutableStateFlow<Boolean?>(null)
    val needsOnboarding: StateFlow<Boolean?> = _needsOnboarding.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    val prefsState: StateFlow<UserPrefsState> = preferences.prefsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UserPrefsState())

    val userProfile: StateFlow<UiState<UserProfile?>> = refreshTrigger
        .flatMapLatest { repository.observeUserProfile(currentUserId) }
        .asUiState("Failed to load user profile")

    val categoriesState: StateFlow<UiState<List<DashboardCategory>>> = refreshTrigger
        .flatMapLatest { repository.observeDashboardCategories() }
        .map { list -> list.ifEmpty { MurshidabadConstants.DEFAULT_DASHBOARDS } }
        .asUiState("Failed to load dashboard categories")

    val bannersState: StateFlow<UiState<List<ContentItem>>> = contentState("banners")
    val adsState: StateFlow<UiState<List<ContentItem>>> = contentState("advertisements")
    val announcementsState: StateFlow<UiState<List<ContentItem>>> = contentState("announcements")
    val newsState: StateFlow<UiState<List<ContentItem>>> = contentState("news")
    val jobsState: StateFlow<UiState<List<ContentItem>>> = contentState("jobs")
    val educationState: StateFlow<UiState<List<ContentItem>>> = contentState("education")
    val scholarshipsState: StateFlow<UiState<List<ContentItem>>> = contentState("scholarships")
    val healthcareState: StateFlow<UiState<List<ContentItem>>> = contentState("healthcare")
    val businessesState: StateFlow<UiState<List<ContentItem>>> = contentState("businesses")
    val schemesState: StateFlow<UiState<List<ContentItem>>> = contentState("government_schemes")
    val agricultureState: StateFlow<UiState<List<ContentItem>>> = contentState("agriculture")
    val marketPricesState: StateFlow<UiState<List<ContentItem>>> = contentState("market_prices")
    val transportState: StateFlow<UiState<List<ContentItem>>> = contentState("transport")
    val tourismState: StateFlow<UiState<List<ContentItem>>> = contentState("tourism")
    val emergencyState: StateFlow<UiState<List<ContentItem>>> = contentState("emergency_contacts")
    val servicesState: StateFlow<UiState<List<ContentItem>>> = contentState("services")

    val notificationsState: StateFlow<UiState<List<NotificationItem>>> = refreshTrigger
        .flatMapLatest { repository.observeNotifications() }
        .asUiState("Failed to load notifications")

    val communityPostsState: StateFlow<UiState<List<CommunityPost>>> = refreshTrigger
        .flatMapLatest { repository.observeCommunityPosts() }
        .asUiState("Failed to load community posts")

    val lostFoundState: StateFlow<UiState<List<LostFoundItem>>> = refreshTrigger
        .flatMapLatest { repository.observeLostFound() }
        .asUiState("Failed to load lost & found items")

    val bloodDonorsState: StateFlow<UiState<List<BloodDonorItem>>> = refreshTrigger
        .flatMapLatest { repository.observeBloodDonors() }
        .asUiState("Failed to load blood donors")

    val appUpdateConfig: StateFlow<AppUpdateConfig?> = repository.observeAppUpdate()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val publicSettings: StateFlow<PublicSettings?> = repository.observePublicSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    init {
        viewModelScope.launch {
            if (firebaseUser != null) {
                repository.ensureUserProfile(firebaseUser)
                    .onSuccess { shouldOnboard -> _needsOnboarding.value = shouldOnboard }
                    .onFailure {
                        _needsOnboarding.value = false
                        _actionMessage.value = it.localizedMessage ?: "Profile sync failed"
                    }
            } else {
                _needsOnboarding.value = false
            }
        }
    }

    private fun contentState(collection: String): StateFlow<UiState<List<ContentItem>>> =
        refreshTrigger
            .flatMapLatest { repository.observeContentCollection(collection) }
            .asUiState("Failed to load $collection")

    private fun <T> Flow<T>.asUiState(fallbackError: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, fallbackError, rootError)
                emit(UiState.Error(rootError.localizedMessage ?: fallbackError))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    fun refreshAll() {
        viewModelScope.launch {
            _isRefreshing.value = true
            refreshTrigger.value += 1
            kotlinx.coroutines.delay(500)
            _isRefreshing.value = false
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            preferences.setLanguage(lang)
            val profile = (userProfile.value as? UiState.Success)?.data
            if (profile != null) {
                repository.updateUserProfile(
                    uid = currentUserId,
                    name = profile.name,
                    village = profile.village,
                    block = profile.block,
                    district = profile.district,
                    mobile = profile.mobile,
                    preferredLanguage = lang,
                    selectedDashboards = profile.selectedDashboards,
                    primaryDashboard = profile.primaryDashboard
                )
            }
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { preferences.setDarkMode(enabled) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setNotificationsEnabled(enabled) }
    }

    fun completeOnboardingOrUpdateProfile(
        name: String,
        village: String,
        block: String,
        district: String,
        mobile: String,
        preferredLanguage: String,
        selectedDashboards: List<String>,
        primaryDashboard: String,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            preferences.setLanguage(preferredLanguage)
            repository.updateUserProfile(
                uid = currentUserId,
                name = name,
                village = village,
                block = block,
                district = district,
                mobile = mobile,
                preferredLanguage = preferredLanguage,
                selectedDashboards = selectedDashboards,
                primaryDashboard = primaryDashboard
            ).onSuccess {
                _needsOnboarding.value = false
                _actionMessage.value = "Profile saved"
                onDone()
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Failed to save profile"
            }
        }
    }

    fun switchPrimaryDashboard(slug: String) {
        val profile = (userProfile.value as? UiState.Success)?.data ?: return
        val updatedSelected = if (profile.selectedDashboards.contains(slug)) profile.selectedDashboards
        else (profile.selectedDashboards + slug).take(10)
        completeOnboardingOrUpdateProfile(
            name = profile.name,
            village = profile.village,
            block = profile.block,
            district = profile.district,
            mobile = profile.mobile,
            preferredLanguage = profile.preferredLanguage,
            selectedDashboards = updatedSelected,
            primaryDashboard = slug
        )
    }

    fun createCommunityPost(title: String, description: String, imageUrl: String, category: String, onSuccess: () -> Unit) {
        val profile = (userProfile.value as? UiState.Success)?.data
        viewModelScope.launch {
            repository.createCommunityPost(
                title = title,
                description = description,
                imageUrl = imageUrl,
                category = category,
                block = profile?.block ?: "",
                authorName = profile?.name ?: firebaseUser?.displayName ?: "Resident",
                authorPhotoUrl = profile?.photoUrl ?: firebaseUser?.photoUrl?.toString() ?: ""
            ).onSuccess {
                _actionMessage.value = "Post published"
                onSuccess()
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Failed to publish post"
            }
        }
    }

    fun toggleLike(post: CommunityPost) {
        viewModelScope.launch {
            repository.toggleCommunityLike(post).onFailure {
                _actionMessage.value = it.localizedMessage ?: "Failed to update like"
            }
        }
    }

    fun observeComments(postId: String): Flow<List<CommunityComment>> = repository.observeComments(postId)

    fun addComment(postId: String, text: String, onSuccess: () -> Unit) {
        val profile = (userProfile.value as? UiState.Success)?.data
        viewModelScope.launch {
            repository.addComment(
                postId = postId,
                text = text,
                authorName = profile?.name ?: firebaseUser?.displayName ?: "Resident",
                authorPhotoUrl = profile?.photoUrl ?: firebaseUser?.photoUrl?.toString() ?: ""
            ).onSuccess { onSuccess() }
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Failed to post comment" }
        }
    }

    fun createLostFound(
        title: String,
        description: String,
        imageUrl: String,
        category: String,
        location: String,
        contactInfo: String,
        dateText: String,
        onSuccess: () -> Unit
    ) {
        val profile = (userProfile.value as? UiState.Success)?.data
        viewModelScope.launch {
            repository.createLostFoundItem(
                title = title,
                description = description,
                imageUrl = imageUrl,
                category = category,
                location = location,
                contactInfo = contactInfo,
                dateText = dateText,
                reporterName = profile?.name ?: firebaseUser?.displayName ?: "Resident"
            ).onSuccess {
                _actionMessage.value = "Item submitted"
                onSuccess()
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Submission failed"
            }
        }
    }

    fun createBloodEntry(
        title: String,
        description: String,
        bloodGroup: String,
        category: String,
        location: String,
        contactPhone: String,
        availability: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.createBloodEntry(
                title = title,
                description = description,
                bloodGroup = bloodGroup,
                category = category,
                location = location,
                contactPhone = contactPhone,
                availability = availability
            ).onSuccess {
                _actionMessage.value = "Blood registry updated"
                onSuccess()
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Failed to save"
            }
        }
    }

    fun observeBusinessReviews(businessId: String): Flow<List<BusinessReview>> =
        repository.observeBusinessReviews(businessId)

    fun submitBusinessReview(business: ContentItem, rating: Int, comment: String, onSuccess: () -> Unit) {
        val profile = (userProfile.value as? UiState.Success)?.data
        viewModelScope.launch {
            repository.submitBusinessReview(
                business = business,
                authorName = profile?.name ?: firebaseUser?.displayName ?: "Resident",
                rating = rating,
                comment = comment
            ).onSuccess {
                _actionMessage.value = "Review added"
                onSuccess()
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Failed to add review"
            }
        }
    }

    fun submitReport(targetCollection: String, targetId: String, reason: String, details: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.submitReport(targetCollection, targetId, reason, details)
                .onSuccess {
                    _actionMessage.value = "Report submitted for review"
                    onSuccess()
                }
                .onFailure {
                    _actionMessage.value = it.localizedMessage ?: "Failed to submit report"
                }
        }
    }

    companion object {
        private const val TAG = "AppViewModel"
    }
}
