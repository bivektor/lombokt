import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

dependencies {
  kotlinCompilerPluginClasspath(project(":lombokt-plugin"))
  testCompileOnly(project(":lombokt-api"))
  testImplementation(kotlin("test"))
}

kotlin {
  compilerOptions {
    freeCompilerArgs = listOf(
      "-P",
      "plugin:com.bivektor.lombokt.LomboktComponentRegistrar:trace=true"
    )
  }
}

tasks.withType<KotlinCompile>() {
  this.logging.captureStandardOutput(LogLevel.INFO)
}