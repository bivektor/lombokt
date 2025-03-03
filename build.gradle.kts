plugins {
  kotlin("jvm") apply false
}

allprojects {
  apply(plugin = "idea")
  apply(plugin = "org.jetbrains.kotlin.jvm")

  dependencies {
    val testImplementation by configurations
    val testRuntimeOnly by configurations

    testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
  }

  tasks.named<Test>("test") {
    useJUnitPlatform()
  }

  the<JavaPluginExtension>().apply {
    withSourcesJar()
  }
}