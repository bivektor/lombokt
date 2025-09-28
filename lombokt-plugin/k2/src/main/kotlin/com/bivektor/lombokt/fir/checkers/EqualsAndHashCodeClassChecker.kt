package com.bivektor.lombokt.fir.checkers

import com.bivektor.lombokt.fir.services.equalsAndHashCodeService
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.symbols.SymbolInternals

object EqualsAndHashCodeClassChecker : FirClassChecker(MppCheckerKind.Common) {
  context(context: CheckerContext, reporter: DiagnosticReporter)
  override fun check(declaration: FirClass) {
    val equalsAndHashCodeService = context.session.equalsAndHashCodeService
    equalsAndHashCodeService.checkClass(declaration, context, reporter)
  }
}
