-keep class com.android.billingclient.** { *; }
-keep interface com.android.billingclient.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keep class androidx.camera.** { *; }
-keep class coil.** { *; }
-keepclassmembers class com.yourco.grainshot.camera.FilterRenderer {
    private static final java.lang.String CMOS_SHADER_SRC;
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
