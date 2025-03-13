package com.bivektor.lombokt.fir

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.hasModifier
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.isExtension
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

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

private fun FirClassSymbol<*>.getDeclaredFunction(predicate: (FirNamedFunctionSymbol) -> Boolean): FirNamedFunctionSymbol? =
  declarationSymbols.filterIsInstance<FirNamedFunctionSymbol>().firstOrNull(predicate)

fun FirClassSymbol<*>.findAnnotation(
  session: FirSession, annotationClassId: ClassId, withArguments: Boolean = false
): FirAnnotation? {
  val annotations = if (withArguments) resolvedAnnotationsWithArguments else resolvedAnnotationsWithClassIds
  return annotations.find { it.toAnnotationClassId(session) == annotationClassId }
}

val FirClassSymbol<*>.isValueClass get() = hasModifier(KtTokens.VALUE_KEYWORD)
