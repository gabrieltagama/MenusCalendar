# kotlinx.serialization: keep generated serializers of the share DTOs.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers @kotlinx.serialization.Serializable class com.gabrieltagama.menuplanner.core.data.share.dto.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class com.gabrieltagama.menuplanner.core.data.share.dto.**
-keepclassmembers class com.gabrieltagama.menuplanner.core.data.share.dto.<1>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.gabrieltagama.menuplanner.core.data.share.dto.**$$serializer { *; }
