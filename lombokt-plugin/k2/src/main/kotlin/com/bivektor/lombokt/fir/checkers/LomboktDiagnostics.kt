package com.bivektor.lombokt.fir.checkers

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.fir.declarations.FirFunction

object LomboktDiagnostics {
  val UNSUPPORTED_CLASS_TYPE by error1<PsiElement, String>()
  val FUNCTION_DECLARED_OR_NOT_OVERRIDABLE by warning1<PsiElement, String>()
  val ANNOTATED_DATA_CLASS_BODY_PROPERTY by error1<PsiElement, String>()
  val BUILDER_INVALID_LOCATION by error1<PsiElement, String>()
  val BUILDABLE_INVALID_PRIMARY_CONSTRUCTOR by error1<PsiElement, String>()
  val BUILDABLE_MISSING_BUILDER by error1<PsiElement, String>()
  val BUILDER_UNRECOGNIZED_METHOD by warning1<PsiElement, String>()
}

fun DiagnosticReporter.unrecognizedBuilderMethod(function: FirFunction, context: DiagnosticContext) {
  reportOn(
    function.source,
    LomboktDiagnostics.BUILDER_UNRECOGNIZED_METHOD,
    "Unrecognized builder method. Primary constructor does not contain a parameter with the same name.",
    context
  )
}
