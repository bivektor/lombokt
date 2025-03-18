plugins {
  kotlin("jvm")
}

kotlin {
  val kotlinToolChainVersion: String by project

  compilerOptions {
    jvmToolchain(kotlinToolChainVersion.toInt())
    extraWarnings = true
  }
}

java {
  if (!isTestProject) {
    withSourcesJar()
  }
}

dependencies {
  testImplementation(kotlin("test"))
}

tasks.withType<Test> {
  useJUnitPlatform()
  testLogging {
    events("passed", "skipped", "failed")
  }
}
