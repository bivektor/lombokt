package com.bivektor.lombokt.fir

import org.jetbrains.kotlin.fir.analysis.checkers.hasModifier
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.isExtension
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name

data class NamedFunctionDescriptor(val name: Name, val valueParameterTypes: List<ConeKotlinType>)

fun FirClassSymbol<*>.getDeclaredFunction(
  descriptor: NamedFunctionDescriptor,
): FirNamedFunctionSymbol? = declarationSymbols.filterIsInstance<FirNamedFunctionSymbol>().singleOrNull {
  !it.isExtension &&
    it.name == descriptor.name &&
    it.valueParameterSymbols.map { it.resolvedReturnType } == descriptor.valueParameterTypes
}

val FirClassSymbol<*>.isValueClass get() = hasModifier(KtTokens.VALUE_KEYWORD)
