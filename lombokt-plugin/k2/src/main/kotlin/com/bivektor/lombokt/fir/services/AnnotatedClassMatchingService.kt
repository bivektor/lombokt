package com.bivektor.lombokt.fir.services

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

abstract class AnnotatedClassMatchingService(
  session: FirSession, val annotationName: FqName
) : FirExtensionSessionComponent(session) {

  @Suppress("UNUSED_ANONYMOUS_PARAMETER")
  private val cache: FirCache<FirClassLikeSymbol<*>, Boolean, Nothing?> =
    session.firCachesFactory.createCache { symbol, _ -> symbol.matches() }

  fun isAnnotated(symbol: FirClassLikeSymbol<*>): Boolean {
    return cache.getValue(symbol)
  }

  private fun FirClassLikeSymbol<*>.matches(): Boolean {
    return hasAnnotation(ClassId.topLevel(annotationName), session)
  }
}
