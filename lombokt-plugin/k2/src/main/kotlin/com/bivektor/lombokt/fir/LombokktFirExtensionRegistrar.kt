package com.bivektor.lombokt.fir

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class LombokktFirExtensionRegistrar(private val messageCollector: MessageCollector) : FirExtensionRegistrar() {
  override fun ExtensionRegistrarContext.configurePlugin() {
    +{ session: FirSession -> EqualsAndHashcodeGenerator(session, messageCollector) }
    +{ session: FirSession -> ToStringGenerator(session, messageCollector) }
  }
}