package com.bivektor.lombokt.fir.generators

import com.bivektor.lombokt.PluginKeys
import com.bivektor.lombokt.fir.services.ToStringService
import com.bivektor.lombokt.fir.services.toStringService
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name


open class ToStringGenerator(
  session: FirSession
) : FirDeclarationGenerationExtension(session) {

  private val toStringService: ToStringService by lazy {
    session.toStringService
  }

  private val functionName: Name get() = toStringService.toStringFunction.name

  override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
    if (isSuitableClass(classSymbol)) return setOf(functionName)
    return emptySet()
  }

  private fun isSuitableClass(classSymbol: FirClassSymbol<*>): Boolean {
    return toStringService.isSuitableClassType(classSymbol) && toStringService.isAnnotated(classSymbol)
  }

  override fun generateFunctions(
    callableId: CallableId,
    context: MemberGenerationContext?
  ): List<FirNamedFunctionSymbol> {
    val classSymbol = context?.owner ?: return emptyList()
    if (toStringService.isFunctionDeclaredOrNotOverridable(classSymbol)) return emptyList()
    return listOf(generateToStringMethod(classSymbol).symbol)
  }

  private fun generateToStringMethod(
    classSymbol: FirClassSymbol<*>
  ): FirSimpleFunction = createMemberFunction(
    classSymbol,
    PluginKeys.ToStringKey,
    functionName,
    session.builtinTypes.stringType.coneType,
  ) {
    modality = Modality.OPEN
  }
}
