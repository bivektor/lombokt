plugins {
  `kotlin-dsl`
}

repositories {
  mavenCentral()
}

dependencies {
  val kotlinVersion: String by project
  val deployerPluginVersion: String by project

  implementation(kotlin("gradle-plugin", version = kotlinVersion))
  implementation("io.deepmedia.tools.deployer:io.deepmedia.tools.deployer.gradle.plugin:$deployerPluginVersion")
}
