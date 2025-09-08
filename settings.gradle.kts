pluginManagement {
	repositories {
		gradlePluginPortal()
		mavenCentral()
		google()
		maven("https://repo.dairy.foundation/releases/")
	}
}

includeBuild("core")
includeBuild("customLibraries/solverslib")
includeBuild("customLibraries/nextftc")
includeBuild("customLibraries/ftclib")
