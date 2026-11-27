package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
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
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class MurshidabadRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    suspend fun ensureUserProfile(user: FirebaseUser): Result<Boolean> {
        val docPath = "users/${user.uid}"
        return try {
            val uid = requireUserId()
            check(uid == user.uid) { "Authenticated user mismatch" }
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val payload = mapOf(
                    "uid" to uid,
                    "name" to (user.displayName?.takeIf { it.isNotBlank() } ?: "Murshidabad User"),
                    "email" to (user.email ?: "user@gmail.com"),
                    "photoUrl" to (user.photoUrl?.toString() ?: ""),
                    "provider" to "google.com",
                    "role" to "USER",
                    "status" to "ACTIVE",
                    "district" to "Murshidabad",
                    "block" to "",
                    "village" to "",
                    "preferredLanguage" to "bn",
                    "selectedDashboards" to listOf("general"),
                    "primaryDashboard" to "general",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
                syncFcmTokenQuietly(uid)
                Result.success(true) // needs onboarding
            } else {
                // Keep Google photo and name updated if blank
                val existingPhoto = snap.getString("photoUrl") ?: ""
                val googlePhoto = user.photoUrl?.toString() ?: ""
                if (googlePhoto.isNotBlank() && googlePhoto != existingPhoto) {
                    docRef.update(
                        mapOf(
                            "photoUrl" to googlePhoto,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
                syncFcmTokenQuietly(uid)
                val block = snap.getString("block") ?: ""
                Result.success(block.isBlank())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    private suspend fun syncFcmTokenQuietly(uid: String) {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            if (token.isNotBlank()) {
                db.collection("users").document(uid)
                    .update(mapOf("fcmToken" to token, "updatedAt" to FieldValue.serverTimestamp()))
                    .await()
            }
        } catch (e: Exception) {
            Log.d("MurshidabadRepo", "FCM token sync skipped: ${e.message}")
        }
    }

    fun observeUserProfile(uid: String): Flow<UserProfile?> = flow {
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { it.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsErr ?: error, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun updateUserProfile(
        uid: String,
        name: String,
        village: String,
        block: String,
        district: String,
        mobile: String,
        preferredLanguage: String,
        selectedDashboards: List<String>,
        primaryDashboard: String
    ): Result<Unit> {
        val path = "users/$uid"
        return try {
            val currentUid = requireUserId()
            check(currentUid == uid) { "Cannot update another user's profile" }
            require(name.trim().length in 1..120) { "Name must be 1..120 characters" }
            require(district.trim().length in 1..80) { "District is required" }
            val safeDashboards = selectedDashboards.ifEmpty { listOf("general") }.take(10)
            val safePrimary = primaryDashboard.ifBlank { safeDashboards.first() }
            val updates = mapOf(
                "name" to name.trim(),
                "village" to village.trim().take(120),
                "block" to block.trim().take(80),
                "district" to district.trim().take(80),
                "mobile" to mobile.trim().take(20),
                "preferredLanguage" to (if (preferredLanguage in listOf("bn", "en", "hi")) preferredLanguage else "bn"),
                "selectedDashboards" to safeDashboards,
                "primaryDashboard" to safePrimary,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeDashboardCategories(): Flow<List<DashboardCategory>> = flow {
        val path = "dashboard_categories"
        emitAll(
            db.collection(path)
                .whereEqualTo("isPublished", true)
                .limit(50)
                .snapshots()
                .map { snap ->
                    snap.toObjects(DashboardCategory::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.status == "ACTIVE" }
                        .sortedBy { it.displayOrder }
                }
                .catch { error -> logFlowError(error, path); throw error }
        )
    }

    fun observeContentCollection(collectionName: String, limitCount: Long = 80L): Flow<List<ContentItem>> = flow {
        emitAll(
            db.collection(collectionName)
                .whereEqualTo("isPublished", true)
                .limit(limitCount)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ContentItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.status == "ACTIVE" }
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error -> logFlowError(error, collectionName); throw error }
        )
    }

    fun observeNotifications(): Flow<List<NotificationItem>> = flow {
        val path = "notifications"
        emitAll(
            db.collection(path)
                .whereEqualTo("isPublished", true)
                .limit(60)
                .snapshots()
                .map { snap ->
                    snap.toObjects(NotificationItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error -> logFlowError(error, path); throw error }
        )
    }

    fun observeCommunityPosts(): Flow<List<CommunityPost>> = flow {
        val path = "community_posts"
        emitAll(
            db.collection(path)
                .whereEqualTo("isPublished", true)
                .whereEqualTo("status", "ACTIVE")
                .limit(80)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CommunityPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error -> logFlowError(error, path); throw error }
        )
    }

    suspend fun createCommunityPost(
        title: String,
        description: String,
        imageUrl: String,
        category: String,
        block: String,
        authorName: String,
        authorPhotoUrl: String
    ): Result<Unit> {
        val path = "community_posts"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim()
            val cleanDesc = description.trim()
            val cleanUrl = imageUrl.trim()
            require(cleanTitle.isNotEmpty() && cleanTitle.length <= 200) { "Title must be 1..200 chars" }
            require(cleanDesc.isNotEmpty() && cleanDesc.length <= 4000) { "Description must be 1..4000 chars" }
            require(MurshidabadConstants.isValidImageUrl(cleanUrl)) { "Invalid image URL" }

            val docRef = db.collection(path).document()
            val payload = mapOf(
                "title" to cleanTitle,
                "description" to cleanDesc,
                "imageUrl" to cleanUrl,
                "category" to category.ifBlank { "General" }.take(60),
                "block" to block.take(80),
                "authorUid" to uid,
                "authorName" to authorName.ifBlank { "Resident" }.take(120),
                "authorPhotoUrl" to authorPhotoUrl.take(1000),
                "likedBy" to emptyList<String>(),
                "likesCount" to 0L,
                "commentsCount" to 0L,
                "isPublished" to true,
                "status" to "ACTIVE",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleCommunityLike(post: CommunityPost): Result<Unit> {
        val path = "community_posts/${post.id}"
        return try {
            val uid = requireUserId()
            require(post.id.isNotBlank()) { "Post ID missing" }
            val currentLikes = post.likedBy.toMutableList()
            if (currentLikes.contains(uid)) {
                currentLikes.remove(uid)
            } else {
                if (currentLikes.size < 500) currentLikes.add(uid)
            }
            db.collection("community_posts").document(post.id).update(
                mapOf(
                    "likedBy" to currentLikes,
                    "likesCount" to currentLikes.size.toLong(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeComments(postId: String): Flow<List<CommunityComment>> = flow {
        val path = "community_comments"
        emitAll(
            db.collection(path)
                .whereEqualTo("postId", postId)
                .whereEqualTo("status", "ACTIVE")
                .limit(100)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CommunityComment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error -> logFlowError(error, path); throw error }
        )
    }

    suspend fun addComment(
        postId: String,
        text: String,
        authorName: String,
        authorPhotoUrl: String
    ): Result<Unit> {
        val path = "community_comments"
        return try {
            val uid = requireUserId()
            val cleanText = text.trim()
            require(postId.isNotBlank()) { "Post ID missing" }
            require(cleanText.isNotEmpty() && cleanText.length <= 1000) { "Comment must be 1..1000 chars" }

            val batch = db.batch()
            val commentRef = db.collection(path).document()
            batch.set(
                commentRef,
                mapOf(
                    "postId" to postId,
                    "authorUid" to uid,
                    "authorName" to authorName.ifBlank { "Resident" }.take(120),
                    "authorPhotoUrl" to authorPhotoUrl.take(1000),
                    "text" to cleanText,
                    "status" to "ACTIVE",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val postRef = db.collection("community_posts").document(postId)
            batch.update(
                postRef,
                mapOf(
                    "commentsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    fun observeLostFound(): Flow<List<LostFoundItem>> = flow {
        val path = "lost_found"
        emitAll(
            db.collection(path)
                .whereEqualTo("isPublished", true)
                .limit(80)
                .snapshots()
                .map { snap ->
                    snap.toObjects(LostFoundItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.status != "HIDDEN" }
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error -> logFlowError(error, path); throw error }
        )
    }

    suspend fun createLostFoundItem(
        title: String,
        description: String,
        imageUrl: String,
        category: String,
        location: String,
        contactInfo: String,
        dateText: String,
        reporterName: String
    ): Result<Unit> {
        val path = "lost_found"
        return try {
            val uid = requireUserId()
            require(title.trim().isNotEmpty() && title.trim().length <= 200) { "Title is required" }
            require(description.trim().isNotEmpty() && description.trim().length <= 2000) { "Description is required" }
            require(location.trim().isNotEmpty()) { "Location is required" }
            require(contactInfo.trim().length >= 3) { "Contact info is required" }
            require(MurshidabadConstants.isValidImageUrl(imageUrl.trim())) { "Invalid image URL" }

            val docRef = db.collection(path).document()
            docRef.set(
                mapOf(
                    "title" to title.trim(),
                    "description" to description.trim(),
                    "imageUrl" to imageUrl.trim(),
                    "category" to (if (category == "FOUND") "FOUND" else "LOST"),
                    "location" to location.trim().take(200),
                    "contactInfo" to contactInfo.trim().take(120),
                    "dateText" to dateText.trim().take(60),
                    "reporterUid" to uid,
                    "reporterName" to reporterName.ifBlank { "Resident" }.take(120),
                    "isPublished" to true,
                    "status" to "OPEN",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    fun observeBloodDonors(): Flow<List<BloodDonorItem>> = flow {
        val path = "blood_donors"
        emitAll(
            db.collection(path)
                .whereEqualTo("isPublished", true)
                .limit(80)
                .snapshots()
                .map { snap ->
                    snap.toObjects(BloodDonorItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.status == "ACTIVE" }
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error -> logFlowError(error, path); throw error }
        )
    }

    suspend fun createBloodEntry(
        title: String,
        description: String,
        bloodGroup: String,
        category: String,
        location: String,
        contactPhone: String,
        availability: String
    ): Result<Unit> {
        val path = "blood_donors"
        return try {
            val uid = requireUserId()
            require(title.trim().isNotEmpty()) { "Name/Title is required" }
            require(location.trim().isNotEmpty()) { "Location is required" }
            require(contactPhone.trim().length >= 5) { "Valid phone is required" }
            val docRef = db.collection(path).document()
            docRef.set(
                mapOf(
                    "title" to title.trim().take(140),
                    "description" to description.trim().take(600),
                    "bloodGroup" to bloodGroup,
                    "category" to (if (category == "REQUEST") "REQUEST" else "DONOR"),
                    "location" to location.trim().take(160),
                    "contactPhone" to contactPhone.trim().take(30),
                    "availability" to availability.trim().take(80),
                    "userUid" to uid,
                    "isPublished" to true,
                    "status" to "ACTIVE",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    fun observeBusinessReviews(businessId: String): Flow<List<BusinessReview>> = flow {
        val path = "businesses/$businessId/reviews"
        emitAll(
            db.collection("businesses").document(businessId).collection("reviews")
                .limit(50)
                .snapshots()
                .map { snap ->
                    snap.toObjects(BusinessReview::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error -> logFlowError(error, path); throw error }
        )
    }

    suspend fun submitBusinessReview(
        business: ContentItem,
        authorName: String,
        rating: Int,
        comment: String
    ): Result<Unit> {
        val path = "businesses/${business.id}/reviews"
        return try {
            val uid = requireUserId()
            require(business.id.isNotBlank()) { "Business ID missing" }
            require(rating in 1..5) { "Rating must be 1..5" }
            require(comment.trim().isNotEmpty() && comment.trim().length <= 1000) { "Review comment required" }

            val newCount = business.ratingCount + 1L
            val newAvg = ((business.ratingAvg * business.ratingCount) + rating) / newCount.toDouble()

            val batch = db.batch()
            val reviewRef = db.collection("businesses").document(business.id).collection("reviews").document()
            batch.set(
                reviewRef,
                mapOf(
                    "authorUid" to uid,
                    "authorName" to authorName.ifBlank { "Resident" }.take(120),
                    "rating" to rating.toLong(),
                    "comment" to comment.trim(),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            val businessRef = db.collection("businesses").document(business.id)
            batch.update(
                businessRef,
                mapOf(
                    "ratingAvg" to newAvg,
                    "ratingCount" to newCount,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun submitReport(
        targetCollection: String,
        targetId: String,
        reason: String,
        details: String
    ): Result<Unit> {
        val path = "reports"
        return try {
            val uid = requireUserId()
            require(targetId.isNotBlank()) { "Target ID required" }
            require(reason.trim().isNotEmpty()) { "Reason required" }
            val docRef = db.collection(path).document()
            docRef.set(
                mapOf(
                    "targetCollection" to targetCollection,
                    "targetId" to targetId,
                    "reason" to reason.trim().take(200),
                    "details" to details.trim().take(1000),
                    "reporterUid" to uid,
                    "status" to "PENDING",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    fun observeAppUpdate(): Flow<AppUpdateConfig?> = flow {
        val path = "app_updates/current"
        emitAll(
            db.collection("app_updates").document("current")
                .snapshots()
                .map { snap ->
                    if (snap.exists()) snap.toObject(AppUpdateConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    else null
                }
                .catch { error -> logFlowError(error, path); emit(null) }
        )
    }

    fun observePublicSettings(): Flow<PublicSettings?> = flow {
        val path = "settings/public"
        emitAll(
            db.collection("settings").document("public")
                .snapshots()
                .map { snap ->
                    if (snap.exists()) snap.toObject(PublicSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    else null
                }
                .catch { error -> logFlowError(error, path); emit(null) }
        )
    }

    private fun logFlowError(error: Throwable, path: String) {
        val fsErr = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (fsErr == null && error is CancellationException) return
        if (auth.currentUser != null) {
            handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
        }
    }
}
