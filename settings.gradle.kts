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
  }
}

rootProject.name = "lombokt"
include("plugin:k2")
include("plugin:cli")
include("plugin:common")
include("lombokt-api")
include("plugin-test")
project(":plugin").name = "lombokt-plugin"