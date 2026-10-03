import org.gradle.api.attributes.Attribute

plugins {
    java
    application
}

repositories {
    mavenCentral()
    google()
}

val androidBuildType = Attribute.of("com.android.build.api.attributes.BuildTypeAttr", String::class.java)
val artifactType = Attribute.of("artifactType", String::class.java)

// A plain JVM source set can't consume an Android library project directly: Gradle
// can't pick a build type, and AGP publishes several artifacts per variant. Resolve
// :core to its compiled classes jar instead, on a configuration scoped just to it so
// normal Maven dependencies aren't affected.
val coreClasses = configurations.create("coreClasses") {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
    attributes {
        attribute(androidBuildType, "debug")
        attribute(artifactType, "android-classes-jar")
    }
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        compileClasspath += coreClasses
        runtimeClasspath += coreClasses
    }
}

dependencies {
    coreClasses(project(":core"))
    implementation("androidx.annotation:annotation:1.2.0")
}

application {
    mainClass = "marrow.simulator.Simulator"
}
