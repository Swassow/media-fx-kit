plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
}

android {
    namespace = "com.neuralsound.ffmpegkit"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    api(files("libs/ffmpeg-kit.aar"))
    api("com.arthenica:smart-exception-java:0.2.1")
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.neuralsound"
            artifactId = "ffmpeg-kit-wrapper"
            version = "1.0.0"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}
