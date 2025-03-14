package com.bivektor.lombokt.fir.generators

import com.bivektor.lombokt.LomboktNames
import com.bivektor.lombokt.PluginKeys
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.primaryConstructorSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
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


private val builderPropertiesCallableName = Name.identifier("builderProperties")

class BuildableGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {

  private val buildableService: BuildableService by lazy { session.buildableService }

  override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
    if (!buildableService.isBuildableBuilderClass(classSymbol)) return emptySet()
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
    return containingClass.primaryConstructorSymbol(session)!!.valueParameterSymbols.flatMap { param ->
      generateBuilderPropertiesForParameter(context, param)
    }
  }

  private fun generateBuilderPropertiesForParameter(
    context: MemberGenerationContext,
    param: FirValueParameterSymbol
  ): Iterable<FirPropertySymbol> {
    val paramType = param.resolvedReturnType
    val actualProperty = createMemberProperty(
      context.owner,
      PluginKeys.BuildableKey,
      param.name,
      paramType.withNullability(true, session.typeContext),
      isVal = false,
    ) {
      visibility = Visibilities.Private
    }.symbol

    if (!paramType.canBeNull(session) || !(param.hasDefaultValue && !param.resolvedDefaultValue!!.isNullLiteral)) return listOf(actualProperty)

    val flagProperty = createMemberProperty(
      context.owner,
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

internal class BuildableService(session: FirSession) : FirExtensionSessionComponent(session) {
  private val builderCache: FirCache<FirClassSymbol<*>, Boolean, Nothing?> =
    session.firCachesFactory.createCache { symbol, _ ->
      symbol.hasAnnotation(
        LomboktNames.BUILDABLE_BUILDER_ANNOTATION_ID,
        session
      )
    }

  fun isBuildableBuilderClass(classSymbol: FirClassSymbol<*>): Boolean = builderCache.getValue(classSymbol)
}

private val FirSession.buildableService: BuildableService by FirSession.sessionComponentAccessor()
