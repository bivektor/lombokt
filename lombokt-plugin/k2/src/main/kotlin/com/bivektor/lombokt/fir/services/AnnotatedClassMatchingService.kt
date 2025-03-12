package com.bivektor.lombokt.fir.services

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

abstract class AnnotatedClassMatchingService(
  session: FirSession, val annotationName: FqName
) : FirExtensionSessionComponent(session) {

  private val cache: FirCache<FirClassSymbol<*>, Boolean, Nothing?> =
    session.firCachesFactory.createCache { symbol, _ -> symbol.matches() }

  fun isAnnotated(symbol: FirClassSymbol<*>): Boolean {
    return cache.getValue(symbol)
  }

  private fun FirClassSymbol<*>.matches(): Boolean {
    return hasAnnotation(ClassId.topLevel(annotationName), session)
  }
}
