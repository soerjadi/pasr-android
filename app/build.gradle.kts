import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.openapi.generator)
}

fun prop(name: String): String = providers.gradleProperty(name).getOrElse("")

android {
    namespace = "id.oemah.nusantara"
    compileSdk = 37

    defaultConfig {
        applicationId = "id.oemah.nusantara"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    flavorDimensions += "env"
    productFlavors {
        create("demo") {
            dimension = "env"
            applicationIdSuffix = ".demo"
            buildConfigField("String", "API_BASE_URL", "\"http://localhost/\"")
            buildConfigField("String", "SUPPORT_WHATSAPP", "\"${prop("nusantara.supportWhatsapp")}\"")
        }
        create("live") {
            dimension = "env"
            val baseUrl = providers.gradleProperty("nusantara.apiBaseUrl")
                .getOrElse("https://api.invalid/")
            buildConfigField("String", "API_BASE_URL", "\"$baseUrl\"")
            buildConfigField("String", "SUPPORT_WHATSAPP", "\"${prop("nusantara.supportWhatsapp")}\"")
            // Push (FCM) is configured here instead of google-services.json; blank means push is off.
            buildConfigField("String", "FIREBASE_APP_ID", "\"${prop("nusantara.firebase.appId")}\"")
            buildConfigField("String", "FIREBASE_API_KEY", "\"${prop("nusantara.firebase.apiKey")}\"")
            buildConfigField("String", "FIREBASE_PROJECT_ID", "\"${prop("nusantara.firebase.projectId")}\"")
            buildConfigField("String", "FIREBASE_SENDER_ID", "\"${prop("nusantara.firebase.senderId")}\"")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // java.time is API 26+; minSdk is 24.
        isCoreLibraryDesugaringEnabled = true
    }

    sourceSets {
        // Demo flavor ships the shared API fixtures (docs/api/fixtures) as classpath resources.
        getByName("demo") { resources.directories.add(rootProject.file("../docs/api").path) }
    }

    packaging {
        resources { excludes += "/openapi.yaml" }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.forkEvery = 15
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

// The Retrofit client is generated from the shared contract: docs/api/openapi.yaml
val generatedApiDir = layout.buildDirectory.dir("generated/openapi")

openApiGenerate {
    generatorName.set("kotlin")
    library.set("jvm-retrofit2")
    inputSpec.set(rootProject.file("../docs/api/openapi.yaml").path)
    outputDir.set(generatedApiDir.get().asFile.path)
    apiPackage.set("id.oemah.nusantara.api")
    modelPackage.set("id.oemah.nusantara.api.model")
    packageName.set("id.oemah.nusantara.api")
    generateApiTests.set(false)
    generateModelTests.set(false)
    generateApiDocumentation.set(false)
    generateModelDocumentation.set(false)
    // Only the APIs, the models and the one supporting file the APIs import.
    globalProperties.set(mapOf("apis" to "", "models" to "", "supportingFiles" to "CollectionFormats.kt"))
    configOptions.set(
        mapOf(
            "useCoroutines" to "true",
            "serializationLibrary" to "kotlinx_serialization",
            "dateLibrary" to "string",
            "enumPropertyNaming" to "UPPERCASE",
        ),
    )
}

tasks.named<org.openapitools.generator.gradle.plugin.tasks.ValidateTask>("openApiValidate") {
    inputSpec.set(rootProject.file("../docs/api/openapi.yaml").path)
}

androidComponents {
    onVariants { variant ->
        variant.sources.kotlin?.addStaticSourceDirectory("build/generated/openapi/src/main/kotlin")
    }
}

tasks.configureEach {
    val generatedCodeConsumer = name.startsWith("ksp") || (name.startsWith("compile") && name.endsWith("Kotlin"))
    if (generatedCodeConsumer) dependsOn("openApiGenerate")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.androidx.datastore.preferences)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.room.compiler)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.mockk)
    testImplementation(libs.hilt.android.testing)
    testImplementation(libs.coil.test)
    kspTest(libs.hilt.compiler)
    testImplementation(libs.androidx.paging.testing)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
}
