android {
    namespace = "io.github.skeletonarmy.marrow.nextftc"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    publishing {
        singleVariant("release")
        multipleVariants("merged") { includeBuildTypeValues("debug", "release") }
    }
}

dependencies {
    implementation("dev.nextftc:ftc:1.0.0")
}
