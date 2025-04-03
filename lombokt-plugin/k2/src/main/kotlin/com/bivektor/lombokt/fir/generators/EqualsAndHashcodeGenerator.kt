package com.bivektor.lombokt.fir.generators

import com.bivektor.lombokt.PluginKeys.EqualsHashCodeKey
import com.bivektor.lombokt.fir.NamedFunctionDescriptor
import com.bivektor.lombokt.fir.services.EqualsAndHashCodeService
import com.bivektor.lombokt.fir.services.equalsAndHashCodeService
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

open class EqualsAndHashcodeGenerator(
  session: FirSession
) : FirDeclarationGenerationExtension(session) {

  private val equalsAndHashCodeService: EqualsAndHashCodeService by lazy { session.equalsAndHashCodeService }

  private val equalsFunction: NamedFunctionDescriptor get() = equalsAndHashCodeService.equalsFunction

  private val hashCodeFunction: NamedFunctionDescriptor get() = equalsAndHashCodeService.hashCodeFunction

  override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
    if (!isSuitableClass(classSymbol)) return emptySet()
    return setOf(equalsFunction.name, hashCodeFunction.name)
  }

  private fun isSuitableClass(classSymbol: FirClassSymbol<*>): Boolean {
    if (classSymbol !is FirRegularClassSymbol) return false
    return equalsAndHashCodeService.isSuitableClassType(classSymbol) && equalsAndHashCodeService.isAnnotated(classSymbol)
  }

  override fun generateFunctions(
    callableId: CallableId,
    context: MemberGenerationContext?
  ): List<FirNamedFunctionSymbol> {
    val classSymbol = context?.owner ?: return emptyList()
    if (equalsAndHashCodeService.isAnyFunctionDeclaredOrNotOverridable(classSymbol)) return emptyList()

    val callableName = callableId.callableName
    return when (callableName) {
      equalsFunction.name -> listOf(generateEqualsMethod(classSymbol).symbol)
      hashCodeFunction.name -> listOf(generateHashCodeMethod(classSymbol).symbol)
      else -> error("Unexpected callable name: $callableName")
    }
  }

  private fun generateEqualsMethod(
    classSymbol: FirClassSymbol<*>
  ): FirSimpleFunction {
    return createMemberFunction(
      classSymbol,
      EqualsHashCodeKey,
      equalsFunction.name,
      session.builtinTypes.booleanType.coneType
    ) {
      valueParameter(Name.identifier("other"), equalsFunction.valueParameterTypes.single())
      modality = Modality.OPEN
    }
  }

  private fun generateHashCodeMethod(
    classSymbol: FirClassSymbol<*>
  ): FirSimpleFunction = createMemberFunction(
    classSymbol,
    EqualsHashCodeKey,
    hashCodeFunction.name,
    session.builtinTypes.intType.coneType
  ) {
    modality = Modality.OPEN
  }
}
