# Retrofit/OkHttp/kotlinx-serialization/Hilt/Coil/Firebase ship their own consumer rules.
# What is left is generic type information and the reflection the demo fixture router does.

# Retrofit reads generic return types (Response<T>) and annotations at runtime.
-keepattributes Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations, AnnotationDefault

# The generated API interfaces: keep operation names (the demo FixtureInterceptor maps them to fixture files).
-keep interface id.oemah.nusantara.api.*Api { *; }

# Generated request/response models are (de)serialized by name.
-keep,includedescriptorclasses class id.oemah.nusantara.api.model.** { *; }
