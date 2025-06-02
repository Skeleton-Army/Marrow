pluginManagement {
	repositories {
		gradlePluginPortal()
		mavenCentral()
		google()
		maven("https://repo.dairy.foundation/releases/")
	}
}

includeBuild("../Marrow/core") {
	dependencySubstitution {
		substitute(module("com.skeletonarmy.marrow:core")).using(project(":"))
	}
}

includeBuild("actions")