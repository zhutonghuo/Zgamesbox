plugins {
    id("com.android.application")
}

android {
    namespace = "com.zhutonghuo.zgamesbox"
    compileSdk = 34

    signingConfigs {
        create("release") {
            storeFile = file("../zgamesbox.jks")
            storePassword = "zgamesbox2026"
            keyAlias = "zgamesbox"
            keyPassword = "zgamesbox2026"
        }
    }

    defaultConfig {
        applicationId = "com.zhutonghuo.zgamesbox"
        minSdk = 24
        targetSdk = 34
        versionCode = 15
        versionName = "2.4"
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
