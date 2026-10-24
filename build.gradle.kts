plugins {
    id("com.android.library") version "8.1.0"
    kotlin("android") version "1.9.0"
    id("com.lagradost.cloudstream3.gradle") version "0.1.0"
}

cloudstream {
    // URL del repositorio de GitHub para publicar la extensión
    setRepo(System.getenv("GITHUB_REPOSITORY") ?: "usuario/cloudstream-extensions")
}

android {
    namespace = "com.hackstore"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
    }
}

dependencies {
    val cloudstream by configurations
    cloudstream("com.lagradost:cloudstream3:pre-release")
}
