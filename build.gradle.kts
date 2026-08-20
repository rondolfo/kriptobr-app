plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    // O plugin do Firebase só é aplicado no módulo :app quando existe google-services.json.
    id("com.google.gms.google-services") version "4.4.2" apply false
}
