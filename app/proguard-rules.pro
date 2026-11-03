# kotlinx.serialization
# Room 实体同时用作 JSON 反序列化目标,R8 full mode 下需保留生成的序列化器
-keepattributes *Annotation*, InnerClasses, Signature
-dontnote kotlinx.serialization.**

-keep,includedescriptorclasses class com.example.driveprep.**$$serializer { *; }
-keepclassmembers class com.example.driveprep.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.driveprep.** {
    kotlinx.serialization.KSerializer serializer(...);
}
