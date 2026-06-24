# Proguard rules for Samurai Jack
# This file specifies which classes should NOT be obfuscated

####################  Keep Android classes  ####################
-keep public class android.**
-keepclassmembers class android.** { *; }

####################  Keep R classes  ####################
-keepclassmembers class **.R$* {
    public static <fields>;
}

####################  Keep View classes for Compose  ####################
-keepclasseswithmembers class * {
    *** *ViewModel();
}

####################  Keep Annotations  ####################
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes LocalVariableTable,LocalVariableTypeTable
-keepattributes Signature
-keepattributes Exceptions,InnerClasses,EnclosingMethod

####################  Keep Kotlin metadata  ####################
-keepattributes RuntimeVisibleAnnotations
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keep class kotlin.**DebugMetadata
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

####################  Keep serializable classes  ####################
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

####################  Keep Jetpack Compose  ####################
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }
-keepattributes *Annotation*

####################  Keep Material 3  ####################
-keep class androidx.compose.material3.** { *; }
-keepclassmembers class androidx.compose.material3.** { *; }

####################  Keep Lifecycle classes  ####################
-keep class androidx.lifecycle.** { *; }
-keepclassmembers class androidx.lifecycle.** { *; }

####################  Keep Navigation  ####################
-keep class androidx.navigation.** { *; }
-keepclassmembers class androidx.navigation.** { *; }

####################  Keep Hilt  ####################
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.android.qualifiers.* class * { *; }
-keep @dagger.Module class * { *; }
-keep @dagger.Provides class * { *; }
-keepclassmembers class ** {
    @dagger.Provides <methods>;
    @dagger.hilt.** <methods>;
}

####################  Keep Coroutines  ####################
-keep class kotlin.coroutines.** { *; }
-keepclassmembers class kotlin.coroutines.** { *; }
-keep class kotlinx.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.** { *; }

####################  Keep Timber  ####################
-keep class com.jakewharton.timber.** { *; }
-keepclassmembers class com.jakewharton.timber.** { *; }

####################  Keep Game Classes  ####################
-keep class com.thigazhini_labs.samuraijack.** { *; }
-keepclassmembers class com.thigazhini_labs.samuraijack.** { *; }

-keep class com.thigazhini_labs.samuraijack.engine.** { *; }
-keep class com.thigazhini_labs.samuraijack.audio.** { *; }
-keep class com.thigazhini_labs.samuraijack.models.** { *; }
-keep class com.thigazhini_labs.samuraijack.stages.** { *; }
-keep class com.thigazhini_labs.samuraijack.ui.** { *; }

####################  Keep Parcelable classes  ####################
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

####################  Keep GSON serializable classes  ####################
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

####################  Optimization settings  ####################
-optimizationpasses 5
-dontusemixedcaseclassnames

# Remove debug info
-renamesourcefileattribute SourceFile

####################  Logging  ####################
# Keep BuildConfig for logging
-keep class com.thigazhini_labs.samuraijack.BuildConfig {
    public static <fields>;
}

####################  Reflection-based classes  ####################
# Add any classes that use reflection here
-keep class * implements java.lang.reflect.Type
-keep class * implements java.lang.reflect.ParameterizedType

####################  Native methods  ####################
-keepclasseswithmembernames class * {
    native <methods>;
}

####################  Enum classes  ####################
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

####################  Thread handlers  ####################
-keepclassmembers class * extends java.lang.Thread {
    void run();
}

####################  Custom application classes  ####################
-keep class com.thigazhini_labs.samuraijack.MainActivity { *; }

####################  Disable certain optimizations  ####################
-keepnames class * extends android.app.Activity
-keepnames class * extends android.app.Fragment
-keepnames class * extends androidx.fragment.app.Fragment
-keepnames class * extends android.app.Service
-keepnames class * extends android.content.BroadcastReceiver
-keepnames class * extends android.content.ContentProvider

####################  View constructors for inflation  ####################
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-verbose

# Verbose logging
# -printusage unused.txt
