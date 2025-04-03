plugins {
  kotlin("plugin.allopen")
  kotlin("plugin.serialization")
}

dependencies {
  kotlinCompilerPluginClasspath(project(":lombokt-plugin"))
  testCompileOnly(project(":lombokt-api"))
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.8.1")
}

allOpen {
  annotation("lombokt.Buildable")
}
