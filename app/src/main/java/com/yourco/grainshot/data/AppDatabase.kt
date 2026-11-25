package com.yourco.grainshot.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@TypeConverters(Converters::class)
@Database(
    entities = [Photo::class, Video::class, Frame::class, SubscriptionState::class],
    version  = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun photoDao(): PhotoDao
    abstract fun videoDao(): VideoDao
    abstract fun frameDao(): FrameDao
    abstract fun subscriptionDao(): SubscriptionDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "grainshot.db"
                )
                    .addCallback(DatabaseCallback())
                    .build()
                    .also { INSTANCE = it }
            }
    }
}

class Converters {
    @TypeConverter fun fromPhotoSource(v: PhotoSource): String = v.name
    @TypeConverter fun toPhotoSource(v: String): PhotoSource   = PhotoSource.valueOf(v)
}

private class DatabaseCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        // Seed subscription singleton row
        db.execSQL("INSERT OR IGNORE INTO subscription_state (id) VALUES (1)")
        // Seed bundled frames
        val frames = listOf("grid", "stamp", "corner_ticks", "vignette", "scanlines")
        frames.forEachIndexed { i, name ->
            db.execSQL(
                "INSERT OR IGNORE INTO frames (id, name, assetPath) " +
                "VALUES (${i + 1}, '$name', 'frames/${name}.png')"
            )
        }
    }
}
