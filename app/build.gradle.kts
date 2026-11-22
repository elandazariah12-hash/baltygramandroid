plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "site.baltygram.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "site.baltygram.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.0"
        val url = project.findProperty("SUPABASE_URL")?.toString() ?: System.getenv("SUPABASE_URL") ?: "https://atovuyugeqrbfhkpyhax.supabase.co"
        val key = project.findProperty("SUPABASE_PUBLISHABLE_KEY")?.toString() ?: System.getenv("SUPABASE_PUBLISHABLE_KEY") ?: ""
        buildConfigField("String", "SUPABASE_URL", "\"$url\"")
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"$key\"")
    }
    buildFeatures { buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
