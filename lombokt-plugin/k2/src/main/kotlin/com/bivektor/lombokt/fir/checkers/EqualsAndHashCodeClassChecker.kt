package com.bivektor.lombokt.fir.checkers

import com.bivektor.lombokt.fir.services.equalsAndHashCodeService
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.declarations.FirClass

object EqualsAndHashCodeClassChecker : FirClassChecker(MppCheckerKind.Common) {
  override fun check(
    declaration: FirClass,
    context: CheckerContext,
    reporter: DiagnosticReporter
  ) {
    val equalsAndHashCodeService = context.session.equalsAndHashCodeService
    equalsAndHashCodeService.checkClass(declaration, context, reporter)
  }
}
