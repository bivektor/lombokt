package com.bivektor.lombokt.fir

import com.bivektor.lombokt.LomboktNames.EQUALS_HASHCODE_ANNOTATION_ID
import com.bivektor.lombokt.LomboktNames.EQUALS_METHOD_NAME
import com.bivektor.lombokt.LomboktNames.HASHCODE_METHOD_NAME
import com.bivektor.lombokt.PluginKeys.EqualsHashCodeKey
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.utils.isClass
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.scopes.impl.FirClassDeclaredMemberScope
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

open class EqualsAndHashcodeGenerator(
  session: FirSession,
  private val messageCollector: MessageCollector
) : FirDeclarationGenerationExtension(session) {

  private val functionsCache: FirCache<FirClassSymbol<*>, Map<Name, FirSimpleFunction>?, Nothing?> =
    session.firCachesFactory.createCache(::createFunctions)

  @OptIn(SymbolInternals::class)
  private fun createFunctions(
    classSymbol: FirClassSymbol<*>,
    @Suppress("unused") scope: FirClassDeclaredMemberScope?
  ): Map<Name, FirSimpleFunction>? {
    if (shouldIgnoreClass(classSymbol)) return null
    val equalsFn = generateEqualsMethod(classSymbol)
    val hashCodeFn = generateHashCodeMethod(classSymbol)

    return mapOf(
      EQUALS_METHOD_NAME to equalsFn,
      HASHCODE_METHOD_NAME to hashCodeFn
    )
  }

  private fun shouldIgnoreClass(classSymbol: FirClassSymbol<*>): Boolean {
    classSymbol.annotations.findAnnotation(EQUALS_HASHCODE_ANNOTATION_ID) ?: return true
    if (!classSymbol.isClass) {
      messageCollector.report(
        CompilerMessageSeverity.WARNING,
        "Invalid usage of EqualsAndHashCode on ${classSymbol.classId} which is not a class"
      )
      return true
    }

    return classSymbol.declarationSymbols.any {
      it is FirNamedFunctionSymbol && (it.callableId.callableName == EQUALS_METHOD_NAME || it.callableId.callableName == HASHCODE_METHOD_NAME)
    }
  }

  private fun generateEqualsMethod(
    classSymbol: FirClassSymbol<*>
  ): FirSimpleFunction = createMemberFunction(
    classSymbol,
    EqualsHashCodeKey,
    EQUALS_METHOD_NAME,
    session.builtinTypes.booleanType.coneType
  ) {
    valueParameter(Name.identifier("other"), session.builtinTypes.nullableAnyType.coneType)
    modality = Modality.OPEN
  }

  private fun generateHashCodeMethod(
    classSymbol: FirClassSymbol<*>
  ): FirSimpleFunction = createMemberFunction(
    classSymbol,
    EqualsHashCodeKey,
    HASHCODE_METHOD_NAME,
    session.builtinTypes.intType.coneType
  ) {
    modality = Modality.OPEN
  }

  override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
    return functionsCache.getValue(classSymbol)?.keys.orEmpty()
  }

  override fun generateFunctions(
    callableId: CallableId,
    context: MemberGenerationContext?
  ): List<FirNamedFunctionSymbol> {
    val classSymbol = context?.owner ?: return emptyList()
    return functionsCache.getValue(classSymbol)?.get(callableId.callableName)?.let { listOf(it.symbol) } ?: emptyList()
  }

}
