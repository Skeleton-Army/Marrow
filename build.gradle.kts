subprojects {
    apply(plugin = "com.android.library")
    apply(plugin = "io.deepmedia.tools.deployer")

    group = "io.github.skeleton-army.marrow"
    version = "0.0.1"

    repositories {
        mavenCentral()
        google()
    }

    extensions.configure<io.deepmedia.tools.deployer.DeployerExtension> {
        projectInfo {
            name.set("Marrow")
            description.set("A lightweight library for building advanced robot behaviors.")
            url.set("https://github.com/Skeleton-Army/Marrow")
            scm {
                fromGithub("Skeleton-Army", "Marrow")
            }
            developer("Skeleton Army", "skeleton.army23644@gmail.com")
            license(MIT)
        }

        centralPortalSpec {
            auth.user.set(secret("CENTRAL_PORTAL_USER"))
            auth.password.set(secret("CENTRAL_PORTAL_PASSWORD"))

            signing.key.set(secret("SIGNING_KEY"))
            signing.password.set(secret("SIGNING_PASSWORD"))
        }

        content {
            androidComponents("release", "merged")
        }
    }
}

tasks.register("publishToMavenCentral") {
    dependsOn(subprojects.map { it.tasks.named("deployCentralPortal") })
}

tasks.register("publishToMavenLocal") {
    dependsOn(subprojects.map { it.tasks.named("publishToMavenLocal") })
}
