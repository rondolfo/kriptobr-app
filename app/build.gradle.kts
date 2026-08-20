plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

/* O push só entra quando você colocar o google-services.json em app/.
   Sem o arquivo o app compila e funciona igual — só não recebe notificação. */
val temFirebase = file("google-services.json").exists()
if (temFirebase) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.kriptobr.mercado"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kriptobr.mercado"
        minSdk = 24                 // Android 7.0 — cobre praticamente todo aparelho em uso
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        debug {
            // APK de teste: pode instalar lado a lado com a versão da loja
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
    buildFeatures { buildConfig = true }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // As bibliotecas entram sempre, para o projeto compilar do mesmo jeito.
    // Sem google-services.json o Firebase apenas não inicia — registra um aviso no log e segue.
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-analytics")
}
