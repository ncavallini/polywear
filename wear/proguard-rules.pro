# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}
# Keep @Serializable data classes
-keep,includedescriptorclasses class ch.ncavallini.eduappwear.**$$serializer { *; }
-keepclassmembers class ch.ncavallini.eduappwear.** {
    *** Companion;
}
