package com.bivektor.lombokt.fir.services

import com.bivektor.lombokt.LomboktNames.BUILDABLE_ANNOTATION_NAME
import com.bivektor.lombokt.LomboktNames.BUILDER_ANNOTATION_NAME
import com.bivektor.lombokt.fir.isValueClass
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate.BuilderContext.and
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate.BuilderContext.annotated
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate.BuilderContext.parentAnnotated
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol

class BuildableService(session: FirSession) : FirExtensionSessionComponent(session) {
  private val builderPredicate = annotated(BUILDER_ANNOTATION_NAME).and(
    parentAnnotated(BUILDABLE_ANNOTATION_NAME)
  )

  @Suppress("UNUSED_ANONYMOUS_PARAMETER")
  private val builderCache: FirCache<FirClassSymbol<*>, Boolean, Nothing?> =
    session.firCachesFactory.createCache { symbol, _ ->
      session.predicateBasedProvider.matches(builderPredicate, symbol)
    }

  override fun FirDeclarationPredicateRegistrar.registerPredicates() {
    register(builderPredicate)
  }

  fun isBuilderClass(classSymbol: FirClassSymbol<*>): Boolean = builderCache.getValue(classSymbol)

  fun isSuitableBuilderClassType(symbol: FirClassSymbol<*>): Boolean = isRegularClass(symbol)

  fun isSuitableBuildableClassType(symbol: FirClassSymbol<*>): Boolean = isRegularClass(symbol)

  private fun isRegularClass(symbol: FirClassSymbol<*>): Boolean {
    if (symbol !is FirRegularClassSymbol) return false
    return when {
      // Allow only regular classes not objects, enum classes & entries, interfaces, annotation classes
      symbol.classKind != ClassKind.CLASS -> false

      // Disallow special types
      symbol.isInline || symbol.isValueClass || symbol.isLocal || symbol.isInner -> false

      else -> true
    }
  }
}

val FirSession.buildableService: BuildableService by FirSession.sessionComponentAccessor()
