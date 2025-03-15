package com.bivektor.lombokt.fir.checkers

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.error1
import org.jetbrains.kotlin.diagnostics.warning1

object LomboktDiagnostics {
  val UNSUPPORTED_CLASS_TYPE by error1<PsiElement, String>()
  val FUNCTION_DECLARED_OR_NOT_OVERRIDABLE by warning1<PsiElement, String>()
  val ANNOTATED_DATA_CLASS_BODY_PROPERTY by error1<PsiElement, String>()
  val BUILDER_INVALID_LOCATION by error1<PsiElement, String>()
  val BUILDABLE_INVALID_PRIMARY_CONSTRUCTOR by error1<PsiElement, String>()
  val BUILDER_MISSING_METHOD by error1<PsiElement, String>()
  val BUILDER_INVALID_METHOD_SIGNATURE by error1<PsiElement, String>()
}
