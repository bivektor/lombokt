import io.deepmedia.tools.deployer.DeployerExtension

plugins {
  id("io.deepmedia.tools.deployer")
}

the<DeployerExtension>().apply {
  projectInfo {
    val scmUrl = "https://github.com/bivektor/lombokt"

    url = scmUrl
    scm {
      connection = "scm:git:git://github.com/bivektor/${scmUrl}.git"
      developerConnection = connection
      url = scmUrl
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
