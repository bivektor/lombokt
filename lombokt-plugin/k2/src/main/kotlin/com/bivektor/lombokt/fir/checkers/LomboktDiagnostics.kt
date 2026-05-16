package com.bivektor.lombokt.fir.checkers

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.error1
import org.jetbrains.kotlin.diagnostics.warning1

object LomboktDiagnostics {
  val UNSUPPORTED_CLASS_TYPE by error1<PsiElement, String>()
  val FUNCTION_DECLARED_OR_NOT_OVERRIDABLE by warning1<PsiElement, String>()
  val INVALID_EQUALITY_MEMBER by error1<PsiElement, String>()
  val CALL_SUPER_NO_SUPER_CLASS by warning1<PsiElement, String>()
}
