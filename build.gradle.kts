import io.deepmedia.tools.deployer.DeployerExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension

plugins {
  kotlin("jvm") apply false
  id("io.deepmedia.tools.deployer") apply false
}

allprojects {
  apply(plugin = "idea")
  apply(plugin = "org.jetbrains.kotlin.jvm")

  dependencies {
    val testImplementation by configurations
    val testRuntimeOnly by configurations

    testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
  }

  tasks.named<Test>("test") {
    useJUnitPlatform()
  }

  the<JavaPluginExtension>().apply {
    withSourcesJar()
  }
}

subprojects {
  if (project.parent != rootProject || project.name.contains("test"))
    return@subprojects

  apply(plugin = "io.deepmedia.tools.deployer")

  the<KotlinJvmExtension>().apply {
    compilerOptions {
      extraWarnings = true
    }
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
}
