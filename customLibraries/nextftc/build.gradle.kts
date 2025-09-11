plugins {
    id("dev.frozenmilk.android-library") version "10.3.0-0.1.4"
    id("dev.frozenmilk.publish") version "0.0.5"
    id("dev.frozenmilk.doc") version "0.0.5"
}

apply(from = "./dependencies.gradle")

android.namespace = "com.skeletonarmy.marrow.nextftc"

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
}

dependencies {
    implementation("dev.nextftc:ftc:1.0.0")
}

dairyPublishing {
    gitDir = file("..")
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.skeletonarmy.marrow"
            artifactId = "nextftc"

            artifact(dairyDoc.dokkaHtmlJar)
            artifact(dairyDoc.dokkaJavadocJar)

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}