import gradle.kotlin.dsl.accessors._cb6cb9110f5ef9edc63ba98f09b0c2ae.dokkaJavadoc
import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URL

plugins {
  id("org.jetbrains.dokka")
}

val kotlinToolChainVersion: String by project
val githubRepo: String by project

tasks.withType<DokkaTask>().configureEach {
  dokkaSourceSets.configureEach {
    languageVersion.set(kotlinToolChainVersion)
    apiVersion.set(kotlinToolChainVersion)

    sourceLink {
      remoteUrl.set(URL("https://github.com/$githubRepo/tree/main/$project.name/src/main/kotlin"))
      localDirectory.set(file("src/main/kotlin"))
      remoteLineSuffix.set("#L")
    }
  }
}

the<JavaPluginExtension>().withJavadocJar()

tasks.named<Jar>("javadocJar") {
  dependsOn(tasks.dokkaJavadoc)
  from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
  archiveClassifier.set("javadoc")
}
