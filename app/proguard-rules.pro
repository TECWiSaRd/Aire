# Keep kotlinx.serialization generated serializers for the domain models.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class com.aire.domain.** {
    *** Companion;
}
-keepclasseswithmembers class com.aire.domain.** {
    kotlinx.serialization.KSerializer serializer(...);
}
