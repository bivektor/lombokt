package com.bivektor.lombokt.maven

import org.apache.maven.plugin.MojoExecution
import org.apache.maven.project.MavenProject
import org.codehaus.plexus.component.annotations.*
import org.codehaus.plexus.logging.*
import org.jetbrains.kotlin.maven.KotlinMavenPluginExtension
import org.jetbrains.kotlin.maven.PluginOption

const val LOMBOKT_COMPILER_PLUGIN_ID = "com.bivektor.lombokt"

@Component(role = KotlinMavenPluginExtension::class, hint = "bivektor-lombokt")
class KotlinLomboktMavenPluginExtension : KotlinMavenPluginExtension {

  @Requirement
  lateinit var logger: Logger

  override fun getCompilerPluginId() = LOMBOKT_COMPILER_PLUGIN_ID

  override fun isApplicable(project: MavenProject, execution: MojoExecution) = true

  override fun getPluginOptions(project: MavenProject, execution: MojoExecution): List<PluginOption> {
    logger.debug("Loaded Maven plugin " + javaClass.name)
    return emptyList()
  }
}
