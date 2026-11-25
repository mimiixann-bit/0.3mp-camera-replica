package com.yourco.grainshot.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PhotoSource { CAMERA, IMPORT, VIDEO_STILL }

@Entity(tableName = "photos")
data class Photo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaStoreUri: String,
    val source: PhotoSource,
    val capturedAt: Long,
    val hasFrame: Boolean = false,
    val frameId: Long? = null,
    val zoomLevel: Float = 1f,
    val flashOn: Boolean = false,
)

@Entity(tableName = "videos")
data class Video(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaStoreUri: String,
    val recordedAt: Long,
    val durationMs: Long? = null,
)

@Entity(tableName = "frames")
data class Frame(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val assetPath: String,
)

@Entity(tableName = "subscription_state")
data class SubscriptionState(
    @PrimaryKey val id: Int = 1,
    val isSubscribed: Boolean = false,
    val shotCount: Int = 0,
    val lastResetDate: String = "",   // ISO date YYYY-MM-DD
    val expiryMs: Long? = null,
    val purchaseToken: String? = null,
)

const val FREE_SHOT_LIMIT = 10
