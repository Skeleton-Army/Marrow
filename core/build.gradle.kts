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

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
    testImplementation("junit:junit:4.13.2")

    compileOnly("org.firstinspires.ftc:Inspection:11.0.0")
    compileOnly("org.firstinspires.ftc:Blocks:11.0.0")
    compileOnly("org.firstinspires.ftc:RobotCore:11.0.0")
    compileOnly("org.firstinspires.ftc:RobotServer:11.0.0")
    compileOnly("org.firstinspires.ftc:OnBotJava:11.0.0")
    compileOnly("org.firstinspires.ftc:Hardware:11.0.0")
    compileOnly("org.firstinspires.ftc:FtcCommon:11.0.0")
    compileOnly("org.firstinspires.ftc:Vision:11.0.0")
    compileOnly("androidx.appcompat:appcompat:1.2.0")

    // WARNING: DO NOT UPDATE FASTERXML JACKSON
    // 2.10.0 is the latest known good version that doesn't break our code and works with the Android API of the Control Hub
    implementation("com.fasterxml.jackson.core:jackson-core:2.10.0")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.10.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.10.0")
}