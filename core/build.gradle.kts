plugins {
    id("dev.frozenmilk.android-library") version "10.2.0-0.1.3"
    id("dev.frozenmilk.publish") version "0.0.4"
    id("dev.frozenmilk.doc") version "0.0.4"
}

apply(from = "./dependencies.gradle")

android.namespace = "com.skeletonarmy.marrow.core"

ftc {
    sdk {
        RobotCore {
            configurationNames += "testImplementation"
        }
        FtcCommon
        Hardware
    }
}

repositories {
    mavenCentral()
    maven("https://maven.brott.dev")
    maven("https://repo.dairy.foundation/releases")
}

dependencies {
    api("dev.frozenmilk.dairy:CachingHardware:1.0.0")
    implementation("org.ftclib.ftclib:core:2.1.1")
}

dairyPublishing {
    gitDir = file("..")
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.skeletonarmy.marrow"
            artifactId = "core"

            artifact(dairyDoc.dokkaHtmlJar)
            artifact(dairyDoc.dokkaJavadocJar)

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}