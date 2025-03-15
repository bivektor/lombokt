package com.bivektor.lombokt.fir.generators

import com.bivektor.lombokt.PluginKeys
import com.bivektor.lombokt.fir.services.BuildableService
import com.bivektor.lombokt.fir.services.buildableService
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.primaryConstructorSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.plugin.createMemberProperty
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.canBeNull
import org.jetbrains.kotlin.fir.types.isNullLiteral
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.fir.types.withNullability
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty


private val builderPropertiesCallableName = Name.identifier("builderProperties")

class BuildableGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {

  private val buildableService: BuildableService by lazy { session.buildableService }

  override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
    // All these checks are necessary because even FIR declaration checker fails, our generator methods below may still run causing unexpected errors
    if (!buildableService.isSuitableBuilderClassType(classSymbol)) return emptySet()
    if (!buildableService.isBuilderClass(classSymbol)) return emptySet()
    if (!buildableService.isSuitableBuildableClassType(classSymbol.getContainingClassSymbol() as FirClassSymbol<*>)) return emptySet()
    return setOf(builderPropertiesCallableName)
  }

  override fun generateProperties(callableId: CallableId, context: MemberGenerationContext?): List<FirPropertySymbol> {
    if (context == null) return emptyList()
    val callableName = callableId.callableName
    if (callableName != builderPropertiesCallableName) return emptyList()
    return generateBuilderProperties(context)
  }

  private fun generateBuilderProperties(context: MemberGenerationContext): List<FirPropertySymbol> {
    val owner = context.owner
    val containingClass = owner.getContainingClassSymbol() as FirClassSymbol<*>
    return containingClass.primaryConstructorSymbol(session)!!.valueParameterSymbols.mapNotNull { param ->
      generateBuilderPropertiesForParameter(context, param)
    }.ifNotEmpty { flatten() } ?: emptyList()
  }

  private fun generateBuilderPropertiesForParameter(
    context: MemberGenerationContext,
    param: FirValueParameterSymbol
  ): Iterable<FirPropertySymbol>? {
    val paramType = param.resolvedReturnType
    val owner = context.owner
    if (owner.declarationSymbols.any { it is FirPropertySymbol && it.name == param.name }) return null

    val actualProperty = createMemberProperty(
      owner,
      PluginKeys.BuildableKey,
      param.name,
      paramType.withNullability(true, session.typeContext),
      isVal = false,
    ) {
      visibility = Visibilities.Private
    }.symbol

    if (!paramType.canBeNull(session) || !(param.hasDefaultValue && !param.resolvedDefaultValue!!.isNullLiteral)) return listOf(actualProperty)

    val flagProperty = createMemberProperty(
      owner,
      PluginKeys.BuildableKey,
      Name.identifier("${param.name}Set"),
      session.builtinTypes.booleanType.coneType,
      isVal = false,
    ) {
      visibility = Visibilities.Private
    }.symbol

    return listOf(actualProperty, flagProperty)
  }
}
