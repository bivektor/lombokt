dependencies {
  implementation(project(":lombokt-plugin"))
  compileOnly(kotlin("maven-plugin"))
  compileOnly("org.apache.maven:maven-core:3.9.9")
}
