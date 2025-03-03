dependencies {
  api(project(":plugin:k2"))
  api(project(":plugin:common"))
  compileOnly(kotlin("compiler-embeddable"))
}