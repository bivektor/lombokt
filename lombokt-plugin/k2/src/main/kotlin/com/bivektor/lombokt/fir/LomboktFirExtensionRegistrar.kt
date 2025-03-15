package com.bivektor.lombokt.fir

import com.bivektor.lombokt.fir.checkers.LomboktCheckersComponent
import com.bivektor.lombokt.fir.generators.BuildableGenerator
import com.bivektor.lombokt.fir.generators.EqualsAndHashcodeGenerator
import com.bivektor.lombokt.fir.generators.ToStringGenerator
import com.bivektor.lombokt.fir.services.BuildableService
import com.bivektor.lombokt.fir.services.EqualsAndHashCodeService
import com.bivektor.lombokt.fir.services.ToStringService
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class LomboktFirExtensionRegistrar : FirExtensionRegistrar() {
  override fun ExtensionRegistrarContext.configurePlugin() {
    +::LomboktCheckersComponent
    +::ToStringGenerator
    +::EqualsAndHashcodeGenerator
    +::ToStringService
    +::EqualsAndHashCodeService
    +::BuildableService
    +::BuildableGenerator
  }
}
