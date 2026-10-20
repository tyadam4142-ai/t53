plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }

// Optional release signing for CI. The keystore/passwords are supplied via environment
// variables and must never be committed to source control.
val t53StoreFile = System.getenv("T53_RELEASE_STORE_FILE")
val t53StorePassword = System.getenv("T53_RELEASE_STORE_PASSWORD")
val t53KeyAlias = System.getenv("T53_RELEASE_KEY_ALIAS")
val t53KeyPassword = System.getenv("T53_RELEASE_KEY_PASSWORD")
val t53ReleaseSigningConfigured = listOf(
    t53StoreFile, t53StorePassword, t53KeyAlias, t53KeyPassword
).all { !it.isNullOrBlank() }

android {
    if (t53ReleaseSigningConfigured) {
        signingConfigs {
            create("t53Release") {
                storeFile = file(t53StoreFile!!)
                storePassword = t53StorePassword!!
                keyAlias = t53KeyAlias!!
                keyPassword = t53KeyPassword!!
            }
        }
    }
    namespace = "com.t53.telecom53.parent"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.t53.telecom53.parent"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            if (t53ReleaseSigningConfigured) signingConfig = signingConfigs.getByName("t53Release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
