dependencyResolutionManagement {
  @Suppress("UnstableApiUsage")
  repositories {
    mavenCentral()
  }
}

pluginManagement {
  plugins {
    val kotlinVersion: String by settings
    kotlin("jvm") version kotlinVersion

    val deployerPluginVersion: String by settings
    id("io.deepmedia.tools.deployer") version deployerPluginVersion
  }
}

rootProject.name = "lombokt"
include("lombokt-api")
include("lombokt-plugin")
include("lombokt-plugin:k2")
include("lombokt-plugin:cli")
include("lombokt-plugin:common")
include("lombokt-plugin:backend")

include("plugin-test")
