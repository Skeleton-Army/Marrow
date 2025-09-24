subprojects {
    apply(plugin = "com.android.library")
    apply(plugin = "io.deepmedia.tools.deployer")

    group = "io.github.skeleton-army.marrow"
    version = "0.1.2"

    repositories {
        mavenCentral()
        google()
    }

    extensions.configure<io.deepmedia.tools.deployer.DeployerExtension> {
        projectInfo {
            name.set("Marrow")
            description.set("A lightweight library for building advanced robot behaviors.")
            url.set("https://skeleton-army.gitbook.io/marrow")
            scm.fromGithub("Skeleton-Army", "Marrow")
            developer("Skeleton Army", "skeleton.army23644@gmail.com")
            license(MIT)
        }

        signing {
            key.set(secret("SIGNING_KEY"))
            password.set(secret("SIGNING_PASSWORD"))
        }

        nexusSpec("snapshot") {
            repositoryUrl.set("https://central.sonatype.com/repository/maven-snapshots/")

            auth {
                user.set(secret("CENTRAL_PORTAL_USER"))
                password.set(secret("CENTRAL_PORTAL_PASSWORD"))
            }
        }

        centralPortalSpec {
            auth {
                user.set(secret("CENTRAL_PORTAL_USER"))
                password.set(secret("CENTRAL_PORTAL_PASSWORD"))
            }
        }

        localSpec() // For publishing to Maven Local

        content {
            androidComponents("release")
        }
    }
}

tasks.register("deployCentralPortal") {
    group = "publishing"
    description = "Publishes all subprojects to Maven Central."
    dependsOn(subprojects.map { it.tasks.named("deployCentralPortal") })
}

tasks.register("deployNexusSnapshot") {
    group = "publishing"
    description = "Publishes all subprojects to Maven Central Snapshots."
    dependsOn(subprojects.map { it.tasks.named("deployNexusSnapshot") })
}

tasks.register("deployLocal") {
    group = "publishing"
    description = "Publishes all subprojects to Maven Local."
    dependsOn(subprojects.map { it.tasks.named("deployLocal") })
}
