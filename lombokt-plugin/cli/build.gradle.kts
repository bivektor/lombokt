dependencies {
  implementation(project(":lombokt-plugin:k2"))
  implementation(project(":lombokt-plugin:common"))
  implementation(project(":lombokt-plugin:backend"))
  compileOnly(kotlin("compiler-embeddable"))
}
