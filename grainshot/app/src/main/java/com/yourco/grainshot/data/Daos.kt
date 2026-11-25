package com.yourco.grainshot.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos ORDER BY capturedAt DESC")
    fun observeAll(): Flow<List<Photo>>

    @Query("SELECT * FROM photos WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Photo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: Photo): Long

    @Delete
    suspend fun delete(photo: Photo)

    @Update
    suspend fun update(photo: Photo)
}

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos ORDER BY recordedAt DESC")
    fun observeAll(): Flow<List<Video>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(video: Video): Long

    @Delete
    suspend fun delete(video: Video)
}

@Dao
interface FrameDao {
    @Query("SELECT * FROM frames ORDER BY name ASC")
    suspend fun getAll(): List<Frame>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(frames: List<Frame>)
}

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscription_state WHERE id = 1 LIMIT 1")
    fun observe(): Flow<SubscriptionState?>

    @Query("SELECT * FROM subscription_state WHERE id = 1 LIMIT 1")
    suspend fun get(): SubscriptionState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: SubscriptionState)

    @Query("UPDATE subscription_state SET shotCount = shotCount + 1 WHERE id = 1")
    suspend fun incrementShotCount()

    @Query("UPDATE subscription_state SET shotCount = 0, lastResetDate = :date WHERE id = 1")
    suspend fun resetDailyCount(date: String)

    @Query("UPDATE subscription_state SET isSubscribed = 1, purchaseToken = :token, expiryMs = :expiryMs WHERE id = 1")
    suspend fun markSubscribed(token: String, expiryMs: Long?)

    @Query("UPDATE subscription_state SET isSubscribed = 0 WHERE id = 1")
    suspend fun markUnsubscribed()
}
