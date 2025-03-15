package com.bivektor.lombokt.fir.services

import com.bivektor.lombokt.LomboktNames.BUILDABLE_ANNOTATION_ID
import com.bivektor.lombokt.LomboktNames.BUILDABLE_BUILDER_ANNOTATION_ID
import com.bivektor.lombokt.fir.isValueClass
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousObjectSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol

class BuildableService(session: FirSession) : FirExtensionSessionComponent(session) {
  private val builderCache: FirCache<FirClassSymbol<*>, Boolean, Nothing?> =
    session.firCachesFactory.createCache { symbol, _ ->
      symbol.hasAnnotation(
        BUILDABLE_BUILDER_ANNOTATION_ID,
        session
      )
    }

  fun buildableAnnotationId() = BUILDABLE_ANNOTATION_ID

  fun builderAnnotationId() = BUILDABLE_BUILDER_ANNOTATION_ID

  fun isBuildableBuilderClass(classSymbol: FirClassSymbol<*>): Boolean = builderCache.getValue(classSymbol)

  fun isSuitableBuilderClassType(symbol: FirClassSymbol<*>): Boolean = isRegularClass(symbol)

  fun isSuitableBuildableClassType(symbol: FirClassSymbol<*>): Boolean = isRegularClass(symbol)

  private fun isRegularClass(symbol: FirClassSymbol<*>): Boolean {
    return when {
      // Allow only regular classes not objects, enum classes & entries, interfaces, annotation classes
      symbol.classKind != ClassKind.CLASS -> false

      // Disallow anonymous objects
      symbol is FirAnonymousObjectSymbol -> false

      // Disallow special types
      symbol.isInline || symbol.isValueClass || symbol.isLocal || symbol.isInner -> false

      else -> true
    }
  }


}

val FirSession.buildableService: BuildableService by FirSession.sessionComponentAccessor()
