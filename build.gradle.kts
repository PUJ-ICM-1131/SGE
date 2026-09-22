plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    // TODO: uncomment after adding app/google-services.json from Firebase Console
    // alias(libs.plugins.google.services) apply false
}
