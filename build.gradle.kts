subprojects {
    apply(plugin = "com.android.library")
    apply(plugin = "io.deepmedia.tools.deployer")

    group = "io.github.skeleton-army.marrow"
    version = "0.0.1"

    repositories {
        mavenCentral()
        google()
    }

    dependencies {
        add("compileOnly", "org.firstinspires.ftc:Inspection:11.0.0")
        add("compileOnly", "org.firstinspires.ftc:Blocks:11.0.0")
        add("compileOnly", "org.firstinspires.ftc:RobotCore:11.0.0")
        add("compileOnly", "org.firstinspires.ftc:RobotServer:11.0.0")
        add("compileOnly", "org.firstinspires.ftc:OnBotJava:11.0.0")
        add("compileOnly", "org.firstinspires.ftc:Hardware:11.0.0")
        add("compileOnly", "org.firstinspires.ftc:FtcCommon:11.0.0")
        add("compileOnly", "org.firstinspires.ftc:Vision:11.0.0")
        add("compileOnly", "androidx.appcompat:appcompat:1.2.0")
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
