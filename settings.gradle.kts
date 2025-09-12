pluginManagement {
	repositories {
		gradlePluginPortal()
		mavenCentral()
		google()
		maven("https://repo.dairy.foundation/releases/")
	}
}

rootProject.name = "Marrow"

include(
	":core",
	":solverslib",
	":nextftc",
	":ftclib",
)

project(":solverslib").projectDir = file("customLibraries/solverslib")
project(":nextftc").projectDir = file("customLibraries/nextftc")
project(":ftclib").projectDir = file("customLibraries/ftclib")