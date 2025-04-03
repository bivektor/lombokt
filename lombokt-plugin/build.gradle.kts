plugins {
  id("publishing-conventions")
}

configurations.create("embedded")

subprojects {
  // Make sure each subproject exposes its classes to the root project
  configurations.create("embeddable") {
    isCanBeConsumed = true
    isCanBeResolved = false
  }

  afterEvaluate {
    // Add the compiled classes to the embeddable configuration
    artifacts {
      add("embeddable", tasks.named("jar").get())
    }
  }
}

dependencies {
  subprojects.forEach { subproject ->
    "embedded"(project(subproject.path))
  }
}

tasks {
  apiBuild {
    inputJar.value(jar.flatMap { it.archiveFile })
  }

  jar {
    dependsOn(subprojects.map { it.tasks.named("jar") })
    subprojects.forEach { subproject ->
      from(subproject.sourceSets.main.get().output.classesDirs)
      from(subproject.sourceSets.main.get().output.resourcesDir)
    }

    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")

    // Optional: Deduplicate if multiple modules have the same resources
    duplicatesStrategy = DuplicatesStrategy.WARN
  }

  sourcesJar {
    archiveClassifier.set("sources")
    dependsOn(subprojects.map { it.tasks.named("sourcesJar") })
    subprojects.forEach { subproject ->
      from(subproject.sourceSets.main.get().allSource)
    }
  }
}
