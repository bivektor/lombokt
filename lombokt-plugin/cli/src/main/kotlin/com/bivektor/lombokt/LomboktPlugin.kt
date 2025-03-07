package com.bivektor.lombokt

import com.bivektor.lombokt.fir.LombokktFirExtensionRegistrar
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

@OptIn(ExperimentalCompilerApi::class)
class LomboktComponentRegistrar : CompilerPluginRegistrar() {
  override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
    val messageCollector = configuration.get(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
      ?: MessageCollector.NONE

    IrGenerationExtension.registerExtension(LomboktIrGenerationExtension(messageCollector))
    FirExtensionRegistrarAdapter.registerExtension(LombokktFirExtensionRegistrar(messageCollector))
  }

  override val supportsK2: Boolean
    get() = true
}
