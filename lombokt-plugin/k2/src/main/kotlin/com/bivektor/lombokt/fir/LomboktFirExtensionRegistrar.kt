package com.bivektor.lombokt.fir

import com.bivektor.lombokt.fir.checkers.LomboktCheckersComponent
import com.bivektor.lombokt.fir.generators.EqualsAndHashcodeGenerator
import com.bivektor.lombokt.fir.generators.ToStringGenerator
import com.bivektor.lombokt.fir.services.EqualsAndHashCodeService
import com.bivektor.lombokt.fir.services.ToStringService
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class LomboktFirExtensionRegistrar(private val messageCollector: MessageCollector) : FirExtensionRegistrar() {
  override fun ExtensionRegistrarContext.configurePlugin() {
    +::ToStringGenerator
    +EqualsAndHashcodeGenerator.factory(messageCollector)
    +::ToStringService
    +::EqualsAndHashCodeService
    +::LomboktCheckersComponent
  }
}
