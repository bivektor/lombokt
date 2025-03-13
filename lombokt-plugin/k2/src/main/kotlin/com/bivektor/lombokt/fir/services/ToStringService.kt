package com.bivektor.lombokt.fir.services

import com.bivektor.lombokt.LomboktNames.TO_STRING_ANNOTATION_NAME
import com.bivektor.lombokt.LomboktNames.TO_STRING_METHOD_NAME
import com.bivektor.lombokt.fir.NamedFunctionDescriptor
import com.bivektor.lombokt.fir.checkers.LomboktDiagnostics
import com.bivektor.lombokt.fir.checkers.LomboktDiagnostics.UNSUPPORTED_CLASS_TYPE
import com.bivektor.lombokt.fir.findAnnotation
import com.bivektor.lombokt.fir.isFunctionDeclaredOrNotOverridable
import com.bivektor.lombokt.fir.isValueClass
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousObjectSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.name.ClassId

private val toStringAnnotationClassId = ClassId.topLevel(TO_STRING_ANNOTATION_NAME)

private val annotationSimpleName = TO_STRING_ANNOTATION_NAME.shortName()

class ToStringService(session: FirSession) : AnnotatedClassMatchingService(session, TO_STRING_ANNOTATION_NAME) {

  val toStringFunction = TO_STRING_FUNCTION_DESCRIPTOR

  fun checkClass(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
    val classSymbol = declaration.symbol
    val annotation = classSymbol.findAnnotation(session, toStringAnnotationClassId) ?: return
    if (!isSuitableClassType(classSymbol)) {
      reporter.reportOn(
        annotation.source,
        UNSUPPORTED_CLASS_TYPE,
        "@${annotationSimpleName} is only supported on regular classes and objects, not on anonymous objects, interfaces, inline, value and enum classes.",
        context
      )

      return
    }

    if (isFunctionDeclaredOrNotOverridable(classSymbol)) {
      reporter.reportOn(
        annotation.source,
        LomboktDiagnostics.FUNCTION_DECLARED_OR_NOT_OVERRIDABLE,
        "@${annotationSimpleName} is useless on this class, because the method is already declared or final in a super class.",
        context
      )
    }
  }

  fun isSuitableClassType(symbol: FirClassSymbol<*>): Boolean {
    return when {
      // Allow only regular classes and objects, not enum classes & entries, interfaces, annotation classes
      symbol.classKind != ClassKind.CLASS && symbol.classKind != ClassKind.OBJECT -> false

      // Disallow anonymous objects
      symbol is FirAnonymousObjectSymbol -> false

      // Disallow special types
      symbol.isInline || symbol.isValueClass || symbol.isLocal -> false

      else -> true
    }
  }

  fun isFunctionDeclaredOrNotOverridable(classSymbol: FirClassSymbol<*>): Boolean {
    return classSymbol.isFunctionDeclaredOrNotOverridable(session, toStringFunction.predicate)
  }
}

private val TO_STRING_FUNCTION_DESCRIPTOR = NamedFunctionDescriptor(TO_STRING_METHOD_NAME, emptyList())

val FirSession.toStringService: ToStringService by FirSession.sessionComponentAccessor()
