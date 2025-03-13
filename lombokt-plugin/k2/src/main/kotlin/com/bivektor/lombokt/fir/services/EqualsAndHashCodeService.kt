package com.bivektor.lombokt.fir.services

import com.bivektor.lombokt.LomboktNames.EQUALS_METHOD_NAME
import com.bivektor.lombokt.LomboktNames.HASHCODE_METHOD_NAME
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
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousObjectSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.name.ClassId
import com.bivektor.lombokt.LomboktNames.EQUALS_HASHCODE_ANNOTATION_NAME as ANNOTATION_NAME

private val annotationClassId = ClassId.topLevel(ANNOTATION_NAME)

private val annotationSimpleName = ANNOTATION_NAME.shortName()

class EqualsAndHashCodeService(session: FirSession) : AnnotatedClassMatchingService(session, ANNOTATION_NAME) {

  val equalsFunction: NamedFunctionDescriptor by lazy {
    NamedFunctionDescriptor(EQUALS_METHOD_NAME, listOf(session.builtinTypes.nullableAnyType.coneType))
  }

  val hashCodeFunction = HASHCODE_FUNCTION_DESCRIPTOR

  private val anyFunctionPredicate: (FirNamedFunctionSymbol) -> Boolean = {
    equalsFunction.predicate(it) || hashCodeFunction.predicate(it)
  }

  fun checkClass(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
    val classSymbol = declaration.symbol
    val annotation = classSymbol.findAnnotation(session, annotationClassId) ?: return
    if (!isSuitableClassType(classSymbol)) {
      reporter.reportOn(
        annotation.source,
        UNSUPPORTED_CLASS_TYPE,
        "@${annotationSimpleName} is only supported on regular classes and objects, not on anonymous objects, interfaces, inline, value and enum classes.",
        context
      )

      return
    }

    if (isAnyFunctionDeclaredOrNotOverridable(classSymbol)) {
      reporter.reportOn(
        annotation.source,
        LomboktDiagnostics.FUNCTION_DECLARED_OR_NOT_OVERRIDABLE,
        "@${annotationSimpleName} is useless on this class, because '$EQUALS_METHOD_NAME' or '$HASHCODE_METHOD_NAME' method is already declared or final in a super class.",
        context
      )
    }
  }

  fun isAnyFunctionDeclaredOrNotOverridable(classSymbol: FirClassSymbol<*>): Boolean {
    return classSymbol.isFunctionDeclaredOrNotOverridable(session, anyFunctionPredicate)
  }

  fun isSuitableClassType(symbol: FirClassSymbol<*>): Boolean {
    return when {
      // Allow only regular classes not objects, enum classes & entries, interfaces, annotation classes
      symbol.classKind != ClassKind.CLASS -> false

      // Disallow anonymous objects
      symbol is FirAnonymousObjectSymbol -> false

      // Disallow special types
      symbol.isInline || symbol.isValueClass -> false

      else -> true
    }
  }
}

private val HASHCODE_FUNCTION_DESCRIPTOR = NamedFunctionDescriptor(HASHCODE_METHOD_NAME, emptyList())

val FirSession.equalsAndHashCodeService: EqualsAndHashCodeService by FirSession.sessionComponentAccessor()
