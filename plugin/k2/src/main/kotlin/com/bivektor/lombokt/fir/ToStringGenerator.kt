package com.bivektor.lombokt.fir

import com.bivektor.lombokt.LomboktNames.TO_STRING_ANNOTATION_ID
import com.bivektor.lombokt.LomboktNames.TO_STRING_METHOD_NAME
import com.bivektor.lombokt.PluginKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.utils.isClass
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.scopes.impl.FirClassDeclaredMemberScope
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

open class ToStringGenerator(
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
    val toStringFn = generateToStringMethod(classSymbol)

    return mapOf(
      TO_STRING_METHOD_NAME to toStringFn,
    )
  }

  private fun shouldIgnoreClass(classSymbol: FirClassSymbol<*>): Boolean {
    classSymbol.annotations.findAnnotation(TO_STRING_ANNOTATION_ID) ?: return true

    // TODO: Support objects as well
    require(classSymbol.isClass && !classSymbol.isData) {
      "ToString annotation can be applied only to regular Kotlin classes but was applied to ${classSymbol.classId}"
    }

    return classSymbol.declarationSymbols.any {
      it is FirNamedFunctionSymbol && (it.callableId.callableName == TO_STRING_METHOD_NAME)
    }
  }

  private fun generateToStringMethod(
    classSymbol: FirClassSymbol<*>
  ): FirSimpleFunction = createMemberFunction(
    classSymbol,
    PluginKeys.ToStringKey,
    TO_STRING_METHOD_NAME,
    session.builtinTypes.stringType.coneType,
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