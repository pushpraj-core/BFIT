# ─── BFIT ProGuard Rules ───
# These rules prevent R8/ProGuard from stripping classes needed at runtime.

# ─── Keep line numbers for crash reports ───
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ─── Firebase ───
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# ─── Firestore data classes ───
-keepclassmembers class com.example.bfit.database.** { *; }
-keepclassmembers class com.example.bfit.network.** { *; }
-keepclassmembers class com.example.bfit.PlanResult { *; }
-keepclassmembers class com.example.bfit.GroceryItem { *; }
-keepclassmembers class com.example.bfit.Supplement { *; }
-keepclassmembers class com.example.bfit.ChatMessage { *; }

# ─── Retrofit / OkHttp ───
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class retrofit2.** { *; }
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**

# ─── Gson ───
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ─── Google AI / Gemini ───
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**

# ─── ML Kit ───
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# ─── MPAndroidChart ───
-keep class com.github.mikephil.charting.** { *; }

# ─── Room ───
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ─── CameraX ───
-keep class androidx.camera.** { *; }

# ─── Google Play Services / Billing ───
-keep class com.android.vending.billing.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# ─── Kotlin Serialization / Coroutines ───
-dontwarn kotlinx.coroutines.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ─── Prevent stripping of Parcelable/Serializable ───
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}