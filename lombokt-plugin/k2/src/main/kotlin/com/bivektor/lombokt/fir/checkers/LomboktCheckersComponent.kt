package com.bivektor.lombokt.fir.checkers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension

class LomboktCheckersComponent(session: FirSession) : FirAdditionalCheckersExtension(session) {
  override val declarationCheckers: DeclarationCheckers
    get() = object: DeclarationCheckers() {
      override val classCheckers: Set<FirClassChecker>
        get() = setOf(ToStringClassChecker, EqualsAndHashCodeClassChecker, BuildableClassChecker)
    }
}
