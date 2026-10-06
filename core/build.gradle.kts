android {
    namespace = "com.skeletonarmyftc.marrow.core"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

configurations.all {
    resolutionStrategy {
        // Force Gradle to use version 2.10.0 even if there is a newer version inside your TeamCode
        force("com.fasterxml.jackson.core:jackson-core:2.10.0")
        force("com.fasterxml.jackson.core:jackson-annotations:2.10.0")
        force("com.fasterxml.jackson.core:jackson-databind:2.10.0")
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
    testImplementation("junit:junit:4.13.2")

    compileOnly("org.firstinspires.ftc:Inspection:12.0.0")
    compileOnly("org.firstinspires.ftc:Blocks:12.0.0")
    compileOnly("org.firstinspires.ftc:RobotCore:12.0.0")
    compileOnly("org.firstinspires.ftc:RobotServer:12.0.0")
    compileOnly("org.firstinspires.ftc:OnBotJava:12.0.0")
    compileOnly("org.firstinspires.ftc:Hardware:12.0.0")
    compileOnly("org.firstinspires.ftc:FtcCommon:12.0.0")
    compileOnly("org.firstinspires.ftc:Vision:12.0.0")
    compileOnly("androidx.appcompat:appcompat:1.2.0")

    compileOnly("org.ftclib.ftclib:core:2.1.1")
    compileOnly("dev.nextftc:ftc:1.1.0")
    compileOnly("com.acmerobotics.roadrunner:actions:1.0.1")
    compileOnly("com.acmerobotics.dashboard:dashboard:0.6.0")
    compileOnly("com.pedropathing.ivy:pedro:1.1.1")
    compileOnly("com.pedropathing:core:3.0.1")

    // WARNING: DO NOT UPDATE FASTERXML JACKSON
    // 2.10.0 is the latest known good version that doesn't break our code and works with the Android API of the Control Hub
    implementation("com.fasterxml.jackson.core:jackson-core:2.10.0")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.10.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.10.0")
}