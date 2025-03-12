package com.bivektor.lombokt

import com.bivektor.lombokt.fir.LomboktFirExtensionRegistrar
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

@OptIn(ExperimentalCompilerApi::class)
class LomboktComponentRegistrar : CompilerPluginRegistrar() {
  override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
    IrGenerationExtension.registerExtension(LomboktIrGenerationExtension(configuration.messageCollector))
    FirExtensionRegistrarAdapter.registerExtension(LomboktFirExtensionRegistrar(configuration.messageCollector))
  }

  override val supportsK2: Boolean
    get() = true
}
