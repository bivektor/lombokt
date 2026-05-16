package com.bivektor.lombokt.fir

import com.bivektor.lombokt.fir.checkers.LomboktDiagnostics
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.isExtension
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

data class NamedFunctionDescriptor(val name: Name, val valueParameterTypes: List<ConeKotlinType>) {
  val predicate: (FirNamedFunctionSymbol) -> Boolean = {
    !it.isExtension &&
      it.name == name &&
      it.valueParameterSymbols.map { it.resolvedReturnType } == valueParameterTypes
  }
}

fun FirClassSymbol<*>.isFunctionDeclaredOrNotOverridable(
  session: FirSession, predicate: (FirNamedFunctionSymbol) -> Boolean
): Boolean = findNamedFunction(session, predicate, true)?.let { it.second || it.first.isFinal } == true

private fun FirClassSymbol<*>.findNamedFunction(
  session: FirSession,
  predicate: (FirNamedFunctionSymbol) -> Boolean,
  recurseSuperTypes: Boolean = false
): Pair<FirNamedFunctionSymbol, Boolean>? {
  val declared = getDeclaredFunction(predicate)
  if (declared != null) return Pair(declared, true)
  if (!recurseSuperTypes) return null
  for (superType in resolvedSuperTypes) {
    val superTypeSymbol = superType.toClassSymbol(session) ?: continue
    if (superTypeSymbol.classKind != ClassKind.CLASS) continue
    val found = superTypeSymbol.findNamedFunction(session, predicate, true)
    if (found != null) return Pair(found.first, false)
  }

  return null
}

@OptIn(DirectDeclarationsAccess::class)
private fun FirClassSymbol<*>.getDeclaredFunction(
  predicate: (FirNamedFunctionSymbol) -> Boolean
): FirNamedFunctionSymbol? {
  return declarationSymbols.filterIsInstance<FirNamedFunctionSymbol>().firstOrNull { predicate(it) }
}

fun FirClassSymbol<*>.findAnnotation(
  session: FirSession, annotationClassId: ClassId, withArguments: Boolean = false
): FirAnnotation? {
  val annotations = if (withArguments) resolvedAnnotationsWithArguments else resolvedAnnotationsWithClassIds
  return annotations.find { it.toAnnotationClassId(session) == annotationClassId }
}

fun FirClassSymbol<*>.hasNonAnySuperClass(session: FirSession): Boolean {
  return resolvedSuperTypes.any { superType ->
    val classSymbol = superType.toClassSymbol(session) ?: return@any false
    classSymbol.classKind == ClassKind.CLASS && classSymbol.classId != StandardClassIds.Any
  }
}

fun FirAnnotation.reportCallSuperWithoutSuperClass(
  session: FirSession,
  declaredClass: FirClass,
  context: CheckerContext,
  reporter: DiagnosticReporter
) {
  val callSuperExpr = argumentMapping.mapping[Name.identifier("callSuper")]
  val callSuper = (callSuperExpr as? FirLiteralExpression)?.value as? Boolean ?: false
  if (callSuper && !declaredClass.symbol.hasNonAnySuperClass(session)) {
    reporter.reportOn(
      source,
      LomboktDiagnostics.CALL_SUPER_NO_SUPER_CLASS,
      "'callSuper' is set to true but the class has no super class.",
      context
    )
  }
}
