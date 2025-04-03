plugins {
  id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

subprojects {
  apply(plugin = "kotlin-jvm-conventions")
}

apiValidation {
  ignoredProjects += project(":lombokt-plugin").subprojects.map { it.name }
  ignoredProjects += subprojects.filter { it.name.contains("test") }.map { it.name }
}
