# kotlinx.serialization: keep generated serializers of @Serializable classes (type-safe
# navigation routes and JSON DTOs) so Navigation and the JSON codec can resolve them after R8.
-keepattributes *Annotation*, InnerClasses, Signature
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers @kotlinx.serialization.Serializable class com.gabrieltagama.menuplanner.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class com.gabrieltagama.menuplanner.**
-keepclassmembers class com.gabrieltagama.menuplanner.<1>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}

-keepclasseswithmembers class com.gabrieltagama.menuplanner.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Type-safe navigation routes are matched by class, keep them unobfuscated.
-keep @kotlinx.serialization.Serializable class com.gabrieltagama.menuplanner.**.navigation.** { *; }
