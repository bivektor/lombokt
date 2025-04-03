package com.bivektor.lombokt.fir.services

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate.BuilderContext.annotated
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.FqName

abstract class AnnotatedClassMatchingService(
  session: FirSession, annotationName: FqName
) : FirExtensionSessionComponent(session) {

  private val predicate = annotated(annotationName)

  override fun FirDeclarationPredicateRegistrar.registerPredicates() {
    register(predicate)
  }

  @Suppress("UNUSED_ANONYMOUS_PARAMETER")
  private val cache: FirCache<FirRegularClassSymbol, Boolean, Nothing?> =
    session.firCachesFactory.createCache { symbol, _ -> symbol.matches() }

  fun isAnnotated(symbol: FirRegularClassSymbol): Boolean {
    return cache.getValue(symbol)
  }

  private fun FirRegularClassSymbol.matches(): Boolean {
    return session.predicateBasedProvider.matches(predicate, this)
  }
}
