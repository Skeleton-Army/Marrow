pluginManagement {
	repositories {
		gradlePluginPortal()
		mavenCentral()
		google()
	}
}

plugins {
    id("com.android.library") version "8.6.1" apply false
    id("io.deepmedia.tools.deployer") version "0.16.0" apply false
}

rootProject.name = "Marrow"

include(":core")
