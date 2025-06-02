plugins {
	id("dev.frozenmilk.android-library") version "10.1.1-0.1.3"
	id("dev.frozenmilk.publish") version "0.0.4"
	id("dev.frozenmilk.doc") version "0.0.4"
}

android.namespace = "com.skeletonarmy.marrow"

// Most FTC libraries will want the following
ftc {
	sdk {
		RobotCore
		FtcCommon {
			configurationNames += "testImplementation"
		}
	}
}

publishing {
	publications {
		register<MavenPublication>("release") {
			groupId = "com.skeletonarmy"
			artifactId = "Marrow"

			artifact(dairyDoc.dokkaHtmlJar)
			artifact(dairyDoc.dokkaJavadocJar)

			afterEvaluate {
				from(components["release"])
			}
		}
	}
}
