plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

/* O google-services.json do projeto "KriptoBR Mercado" (kriptobr-mercado) já está
   aqui em app/ — o push da marca e o relatório de falhas ligam sozinhos.
   Se algum dia esse arquivo sumir, o app continua compilando e funcionando: só o
   push da KriptoBR para de existir. Os alertas de preço que o próprio usuário cria
   nunca dependeram do Firebase. */
val temFirebase = file("google-services.json").exists()
if (temFirebase) {
    apply(plugin = "com.google.gms.google-services")
    // Relatório de falhas: exigência prática para manter nota boa na Play Store.
    apply(plugin = "com.google.firebase.crashlytics")
}

android {
    namespace = "com.kriptobr.mercado"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.kriptobr.mercado"
        minSdk = 24
        targetSdk = 36
        versionCode = 3
        versionName = "2.1.0"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-teste"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    /* Fica em 33.7.0 de propósito. A partir da BoM 34 o Firebase passou a
       publicar o play-services-measurement compilado com Kotlin 2.2, e o
       compilador Kotlin 2.0.21 deste projeto recusa metadado 2.2:
         "Module was compiled with an incompatible version of Kotlin.
          The binary version of its metadata is 2.2.0, expected version is 2.0.0."
       Subir a BoM exige subir o Kotlin e o Compose junto. Não vale o risco
       agora: 33.7.0 tem push, analytics e Crashlytics completos. */
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
