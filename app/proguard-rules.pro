# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.example.nutrition.**$$serializer { *; }
-keepclassmembers class com.example.nutrition.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.nutrition.** {
    kotlinx.serialization.KSerializer serializer(...);
}
