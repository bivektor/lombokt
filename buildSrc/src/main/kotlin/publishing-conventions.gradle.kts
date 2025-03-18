import io.deepmedia.tools.deployer.DeployerExtension

plugins {
  id("io.deepmedia.tools.deployer")
}

the<JavaPluginExtension>().apply {
  if (hasJavaDocs)
    withJavadocJar()
}

the<DeployerExtension>().apply {
  projectInfo {
    val githubRepo: String by project
    val githubUrl = "https://github.com/$githubRepo"

    url = githubUrl
    scm {
      connection = "scm:git:git://github.com/${githubRepo}.git"
      developerConnection = connection
      url = githubUrl
    }

    license(apache2)
    description = project.findProperty("description") as? String ?: project.name
    developer {
      name = "Bivektor Team"
      email = "dev@bivektor.com"
      organization = "Bivektor"
    }
  }

  content {
    component {
      fromJava()
      if (!hasJavaDocs)
        emptyDocs()
    }
  }

  localSpec()

  centralPortalSpec {

    // Do not publish to maven central. We do it manually
    allowMavenCentralSync = false

    auth {
      user.set(secret("centralPortal.user"))
      password.set(secret("centralPortal.password"))
    }

    signing {
      key.set(secret("signing.key"))
      password.set(secret("signing.password"))
    }
  }
}

private val Project.hasJavaDocs get() = pluginManager.hasPlugin("org.jetbrains.dokka")
