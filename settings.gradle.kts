dependencyResolutionManagement {
  @Suppress("UnstableApiUsage")
  repositories {
    mavenCentral()
  }
}

rootProject.name = "lombokt"
include("lombokt-api")
include("lombokt-plugin")
include("lombokt-plugin:k2")
include("lombokt-plugin:cli")
include("lombokt-plugin:common")
include("lombokt-plugin:backend")
include("lombokt-maven")

include("plugin-test")
