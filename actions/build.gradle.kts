plugins {
    id("dev.frozenmilk.android-library") version "10.1.1-0.1.3"
    id("dev.frozenmilk.publish") version "0.0.4"
    id("dev.frozenmilk.doc") version "0.0.4"
}

android.namespace = "com.skeletonarmy.marrow.actions"

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
}

dependencies {
    implementation ("com.acmerobotics.roadrunner:actions:1.0.1") { exclude(group = "com.acmerobotics.dashboard")  }
    compileOnly("com.acmerobotics.dashboard:dashboard:0.4.16") // compileOnly so it would work with Sloth
}

dairyPublishing {
    gitDir = file("..")
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.skeletonarmy.marrow"
            artifactId = "actions"

            artifact(dairyDoc.dokkaHtmlJar)
            artifact(dairyDoc.dokkaJavadocJar)

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}