plugins {
  id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

subprojects {
  apply(plugin = "kotlin-jvm-conventions")
}

apiValidation {
  val validatedProjects = setOf("lombokt-api", "lombokt-maven")
  ignoredProjects += subprojects.filter { it.name !in validatedProjects }.map { it.name }
}
