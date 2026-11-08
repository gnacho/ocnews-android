# Keep rules para Retrofit / kotlinx.serialization / Coil (se refinan con el primer build release).
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# kotlinx.serialization: mantener serializers.
-keep,includedescriptorclasses class app.ocnews.**$$serializer { *; }
-keepclassmembers class app.ocnews.** {
    *** Companion;
}
-keepclasseswithmembers class app.ocnews.** {
    kotlinx.serialization.KSerializer serializer(...);
}
