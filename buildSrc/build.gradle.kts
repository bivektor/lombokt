plugins {
  `kotlin-dsl`
}

repositories {
  mavenCentral()
}

dependencies {
  val kotlinVersion: String by project
  implementation(kotlin("gradle-plugin", version = kotlinVersion))
  implementation("org.jetbrains.kotlin.plugin.allopen:org.jetbrains.kotlin.plugin.allopen.gradle.plugin:$kotlinVersion")
  implementation("org.jetbrains.kotlin.plugin.serialization:org.jetbrains.kotlin.plugin.serialization.gradle.plugin:$kotlinVersion")

  val deployerPluginVersion: String by project
  implementation("io.deepmedia.tools.deployer:io.deepmedia.tools.deployer.gradle.plugin:$deployerPluginVersion")

  val dokkaVersion: String by project
  implementation("org.jetbrains.dokka:dokka-gradle-plugin:$dokkaVersion")

  val binaryCompatibilityValidatorVersion: String by project
  implementation("org.jetbrains.kotlinx.binary-compatibility-validator:org.jetbrains.kotlinx.binary-compatibility-validator.gradle.plugin:$binaryCompatibilityValidatorVersion")

}
