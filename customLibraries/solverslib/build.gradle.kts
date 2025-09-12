android {
    namespace = "io.github.skeletonarmy.marrow.solverslib"
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

repositories {
    maven("https://repo.dairy.foundation/releases")
}

dependencies {
    implementation("org.solverslib:core:0.3.1")
}
