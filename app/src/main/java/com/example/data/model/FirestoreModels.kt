package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val provider: String = "google.com",
    val role: String = "USER",
    val status: String = "ACTIVE",
    val district: String = "Murshidabad",
    val block: String = "",
    val village: String = "",
    val mobile: String = "",
    val preferredLanguage: String = "bn",
    val selectedDashboards: List<String> = emptyList(),
    val primaryDashboard: String = "general",
    val fcmToken: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DashboardCategory(
    @DocumentId val id: String = "",
    val slug: String = "",
    val title: String = "",
    val bengaliName: String = "",
    val englishName: String = "",
    val hindiName: String = "",
    val description: String = "",
    val iconUrl: String = "",
    val bannerUrl: String = "",
    val imageUrl: String = "",
    val displayOrder: Long = 0L,
    @get:PropertyName("isPublished") @PropertyName("isPublished")
    val published: Boolean = true,
    val status: String = "ACTIVE",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ContentItem(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val category: String = "",
    val subCategory: String = "",
    val block: String = "",
    val location: String = "",
    val contact: String = "",
    val actionUrl: String = "",
    val badgeText: String = "",
    val metaPrimary: String = "",
    val metaSecondary: String = "",
    val metaTertiary: String = "",
    val targetDashboard: String = "",
    val displayOrder: Long = 0L,
    val ratingAvg: Double = 0.0,
    val ratingCount: Long = 0L,
    @get:PropertyName("isPublished") @PropertyName("isPublished")
    val published: Boolean = true,
    val status: String = "ACTIVE",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CommunityPost(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val category: String = "General",
    val block: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val authorPhotoUrl: String = "",
    val likedBy: List<String> = emptyList(),
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    @get:PropertyName("isPublished") @PropertyName("isPublished")
    val published: Boolean = true,
    val status: String = "ACTIVE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CommunityComment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val authorPhotoUrl: String = "",
    val text: String = "",
    val status: String = "ACTIVE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LostFoundItem(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val category: String = "LOST", // LOST or FOUND
    val location: String = "",
    val contactInfo: String = "",
    val dateText: String = "",
    val reporterUid: String = "",
    val reporterName: String = "",
    @get:PropertyName("isPublished") @PropertyName("isPublished")
    val published: Boolean = true,
    val status: String = "OPEN",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BloodDonorItem(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val bloodGroup: String = "O+",
    val category: String = "DONOR", // DONOR or REQUEST
    val location: String = "",
    val contactPhone: String = "",
    val availability: String = "",
    val userUid: String = "",
    @get:PropertyName("isPublished") @PropertyName("isPublished")
    val published: Boolean = true,
    val status: String = "ACTIVE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class NotificationItem(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val category: String = "General",
    val targetAudience: String = "ALL",
    val targetUid: String = "",
    @get:PropertyName("isPublished") @PropertyName("isPublished")
    val published: Boolean = true,
    val status: String = "SENT",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppUpdateConfig(
    @DocumentId val id: String = "",
    val latestVersion: String = "1.0",
    val versionCode: Long = 1L,
    val minSupportedVersion: Long = 1L,
    val releaseNotes: String = "",
    val forceUpdate: Boolean = false,
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String = "",
    val updateUrl: String = "",
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PublicSettings(
    @DocumentId val id: String = "",
    val appName: String = "Murshidabad Connect",
    val developerName: String = "Slstrms Technologies",
    val supportEmail: String = "support@slstrms.in",
    val supportPhone: String = "",
    val privacyPolicy: String = "",
    val termsConditions: String = "",
    val appLogoUrl: String = "",
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BusinessReview(
    @DocumentId val id: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val rating: Long = 5L,
    val comment: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

object MurshidabadConstants {
    val BLOCKS = listOf(
        "Berhampore", "Beldanga I", "Beldanga II", "Bharatpur I", "Bharatpur II",
        "Bhagwangola I", "Bhagwangola II", "Burwan", "Domkal", "Farakka",
        "Hariharpara", "Jalangi", "Kandi", "Khargram", "Lalgola",
        "Murshidabad-Jiaganj", "Nabagram", "Nawda", "Raghunathganj I", "Raghunathganj II",
        "Raninagar I", "Raninagar II", "Sagardighi", "Samserganj", "Suti I", "Suti II"
    )

    val DEFAULT_DASHBOARDS = listOf(
        DashboardCategory(slug = "student", title = "Student", bengaliName = "ছাত্র", englishName = "Student", hindiName = "छात्र", displayOrder = 1L),
        DashboardCategory(slug = "farmer", title = "Farmer", bengaliName = "কৃষক", englishName = "Farmer", hindiName = "किसान", displayOrder = 2L),
        DashboardCategory(slug = "job_seeker", title = "Job Seeker", bengaliName = "চাকরিপ্রার্থী", englishName = "Job Seeker", hindiName = "नौकरी चाहने वाले", displayOrder = 3L),
        DashboardCategory(slug = "business", title = "Business Owner", bengaliName = "ব্যবসায়ী", englishName = "Business Owner", hindiName = "व्यापारी", displayOrder = 4L),
        DashboardCategory(slug = "women", title = "Women", bengaliName = "মহিলা", englishName = "Women", hindiName = "महिला", displayOrder = 5L),
        DashboardCategory(slug = "senior", title = "Senior Citizen", bengaliName = "বয়স্ক নাগরিক", englishName = "Senior Citizen", hindiName = "वरिष्ठ नागरिक", displayOrder = 6L),
        DashboardCategory(slug = "general", title = "General Citizen", bengaliName = "সাধারণ নাগরিক", englishName = "General Citizen", hindiName = "आम नागरिक", displayOrder = 7L)
    )

    fun isValidImageUrl(url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return true
        return (trimmed.startsWith("https://") || trimmed.startsWith("http://")) && trimmed.length <= 1000
    }
}
