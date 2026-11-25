package com.yourco.grainshot.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class GalleryRepository(context: Context) {
    private val dao = AppDatabase.get(context).photoDao()

    fun observePhotos(): Flow<List<Photo>> = dao.observeAll()

    suspend fun getById(id: Long): Photo? = dao.getById(id)

    suspend fun save(
        uri: Uri, source: PhotoSource,
        zoom: Float = 1f, flashOn: Boolean = false,
    ): Long = dao.insert(
        Photo(
            mediaStoreUri = uri.toString(),
            source        = source,
            capturedAt    = System.currentTimeMillis(),
            zoomLevel     = zoom,
            flashOn       = flashOn,
        )
    )

    suspend fun update(photo: Photo) = dao.update(photo)
    suspend fun delete(photo: Photo) = dao.delete(photo)
}

class BillingRepository(context: Context) {
    private val dao = AppDatabase.get(context).subscriptionDao()

    fun observeState(): Flow<SubscriptionState?> = dao.observe()

    suspend fun ensureRow() {
        if (dao.get() == null) dao.upsert(SubscriptionState())
    }

    suspend fun incrementShot() {
        checkDailyReset()
        dao.incrementShotCount()
    }

    suspend fun isPaywallDue(): Boolean {
        checkDailyReset()
        val s = dao.get() ?: return false
        return !s.isSubscribed && s.shotCount >= FREE_SHOT_LIMIT
    }

    private suspend fun checkDailyReset() {
        val s    = dao.get() ?: return
        val today = LocalDate.now().toString()
        if (s.lastResetDate != today) dao.resetDailyCount(today)
    }

    suspend fun checkLocalExpiry(): Boolean {
        val s = dao.get() ?: return false
        if (!s.isSubscribed) return false
        val expiry = s.expiryMs ?: return true
        return if (System.currentTimeMillis() > expiry + GRACE_BUFFER_MS) {
            dao.markUnsubscribed(); false
        } else true
    }

    suspend fun markSubscribed(token: String, expiryMs: Long?) =
        dao.markSubscribed(token, expiryMs)

    suspend fun markUnsubscribed() = dao.markUnsubscribed()

    companion object {
        private const val GRACE_BUFFER_MS = 2L * 24 * 60 * 60 * 1000
    }
}
