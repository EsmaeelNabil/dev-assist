plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.supersam.sie"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.supersam.sie"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("boolean", "SOME_FEATURE_ENABLED", "true")
        buildConfigField("String", "HOST_URL",  "\"https://example.com\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)


    debugImplementation(project(":devassist"))
}


// Add this task to transform BuildConfig
tasks.register("transformBuildConfig") {
    doLast {
        val buildTypes = android.buildTypes.map { it.name }
        buildTypes.forEach { buildType ->
            fileTree("${project.layout.buildDirectory.asFile.get()}/generated/source/buildConfig/${buildType}").matching {
                include("**/BuildConfig.java")
            }.forEach { file ->
                val content = file.readText()
                println(content)

                // Transform boolean fields
                var transformed = content.replace(
                    "public static final boolean (\\w+) = (true|false);".toRegex(),
                    "public static final boolean $1 = dev.supersam.devassist.DevAssistCache.<Boolean>get(\"$1\", $2);"
                )

                // Transform int fields
                transformed = transformed.replace(
                    "public static final int (\\w+) = (\\d+);".toRegex(),
                    "public static final int $1 = dev.supersam.devassist.DevAssistCache.<Integer>get(\"$1\", $2);"
                )

                // Transform long fields
                transformed = transformed.replace(
                    "public static final long (\\w+) = (\\d+L);".toRegex(),
                    "public static final long $1 = dev.supersam.devassist.DevAssistCache.<Long>get(\"$1\", $2);"
                )

                // Transform float fields
                transformed = transformed.replace(
                    "public static final float (\\w+) = (\\d+\\.\\d+f);".toRegex(),
                    "public static final float $1 = dev.supersam.devassist.DevAssistCache.<Float>get(\"$1\", $2);"
                )

                // Transform string fields
                transformed = transformed.replace(
                    "public static final String (\\w+) = \"([^\"]*)\";".toRegex(),
                    "public static final String $1 = dev.supersam.devassist.DevAssistCache.<String>get(\"$1\", \"$2\");"
                )

                file.writeText(transformed)
                println("Transformed BuildConfig in ${file.path}")
            }
        }
    }
}
// Hook into the build process
tasks.whenTaskAdded {
    if (name.startsWith("generate") && name.endsWith("BuildConfig")) {
        finalizedBy("transformBuildConfig")
    }
}