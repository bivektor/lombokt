import org.gradle.api.Project

val Project.isTestProject get() = name.contains("test")
