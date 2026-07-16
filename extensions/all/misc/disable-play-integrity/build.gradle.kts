extension {
    name = "extensions/all/misc/disable-play-integrity.mpe"
}

android {
    defaultConfig {
        minSdk = 21
    }

    buildFeatures {
        aidl = true
    }
}

dependencies {
    compileOnly(libs.annotation)
}
