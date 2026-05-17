package com.bivektor.lombokt.fir.checkers

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.error1
import org.jetbrains.kotlin.diagnostics.error2
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.Renderer
import org.jetbrains.kotlin.diagnostics.warning1

object LomboktDiagnostics : KtDiagnosticsContainer() {
  val UNSUPPORTED_CLASS_TYPE by error2<PsiElement, String, String>()
  val FUNCTION_DECLARED_OR_NOT_OVERRIDABLE by warning1<PsiElement, String>()
  val INVALID_EQUALITY_MEMBER by error1<PsiElement, String>()
  val CALL_SUPER_NO_SUPER_CLASS by warning1<PsiElement, String>()

  override fun getRendererFactory(): BaseDiagnosticRendererFactory {
    return LomboktErrorMessages
  }
}

object LomboktErrorMessages : BaseDiagnosticRendererFactory() {
  override val MAP by KtDiagnosticFactoryToRendererMap("LomboktErrors") { map ->
    map.put(
      LomboktDiagnostics.UNSUPPORTED_CLASS_TYPE,
      "''{0}'' is only supported {1}.",
      TEXT_RENDERER,
      TEXT_RENDERER
    )

    map.put(
      LomboktDiagnostics.FUNCTION_DECLARED_OR_NOT_OVERRIDABLE,
      "''{0}'' is useless on this class, because the method is already declared or final in a super class.",
      TEXT_RENDERER
    )

    map.put(
      LomboktDiagnostics.INVALID_EQUALITY_MEMBER,
      "{0}",
      TEXT_RENDERER
    )

    map.put(
      LomboktDiagnostics.CALL_SUPER_NO_SUPER_CLASS,
      "{0}",
      TEXT_RENDERER
    )
  }
}

private val TEXT_RENDERER = Renderer { text: String -> text }
