package com.bivektor.lombokt.fir.checkers

import com.bivektor.lombokt.LomboktNames.BUILDABLE_ANNOTATION_ID
import com.bivektor.lombokt.LomboktNames.BUILDER_ANNOTATION_ID
import com.bivektor.lombokt.LomboktNames.BUILDER_BUILD_METHOD_NAME
import com.bivektor.lombokt.fir.checkers.LomboktDiagnostics.UNSUPPORTED_CLASS_TYPE
import com.bivektor.lombokt.fir.services.BuildableService
import com.bivektor.lombokt.fir.services.buildableService
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.resolve.getContainingDeclaration
import org.jetbrains.kotlin.fir.symbols.SymbolInternals

object BuildableClassChecker : FirClassChecker(MppCheckerKind.Common) {

  private val buildableAnnotationText = "@${BUILDABLE_ANNOTATION_ID.shortClassName}"
  private val builderAnnotationText = "@${BUILDER_ANNOTATION_ID.shortClassName}"

  @OptIn(SymbolInternals::class)
  override fun check(
    declaration: FirClass,
    context: CheckerContext,
    reporter: DiagnosticReporter
  ) {
    val session = context.session
    val service = session.buildableService
    if (service.isBuilderClass(declaration.symbol)) {
      val containingClass = declaration.getContainingDeclaration(session) as? FirClass
      val buildableAnnotation = containingClass?.getAnnotationByClassId(BUILDABLE_ANNOTATION_ID, session)

      if (buildableAnnotation == null) {
        return reporter.reportOn(
          declaration.source,
          LomboktDiagnostics.BUILDER_INVALID_LOCATION,
          "Builder class must have a parent class annotated with $buildableAnnotationText",
          context
        )
      }

      return checkBuilderClass(service, declaration, containingClass, context, reporter)
    }

    if (service.isBuildableClass(declaration.symbol)) {
      val builderClass = declaration.declarations.filterIsInstance<FirClass>().find { service.isBuilderClass(it.symbol) }
      if (builderClass == null)
        return reporter.reportOn(
          declaration.source,
          LomboktDiagnostics.BUILDABLE_MISSING_BUILDER,
          "$buildableAnnotationText class must have a nested builder class annotated with $builderAnnotationText",
          context
        )

      return checkBuilderClass(service, builderClass, declaration, context, reporter)
    }
  }

  private fun checkBuilderClass(
    service: BuildableService,
    builderClass: FirClass,
    buildableClass: FirClass,
    context: CheckerContext,
    reporter: DiagnosticReporter
  ) {
    val session = context.session
    if (!service.isSuitableBuilderClassType(builderClass.symbol)) {
      return reporter.reportOn(
        builderClass.source,
        UNSUPPORTED_CLASS_TYPE,
        "Builder class must be a regular class, not an object, interface, inner, inline, value or enum class.",
        context
      )
    }

    if (!service.isSuitableBuildableClassType(buildableClass.symbol)) {
      return reporter.reportOn(
        buildableClass.source,
        UNSUPPORTED_CLASS_TYPE,
        "Buildable class must be a regular class, not an object, interface, inner, inline, value or enum class.",
        context
      )
    }

    val constructor = buildableClass.primaryConstructorIfAny(session)

    if (constructor == null) {
      reporter.reportOn(
        buildableClass.source,
        LomboktDiagnostics.BUILDABLE_INVALID_PRIMARY_CONSTRUCTOR,
        "Buildable class must have a primary constructor with at least one property",
        context
      )
      return
    }

    val constructorParams = constructor.valueParameterSymbols.associateBy { it.name }
    builderClass.declarations.filterIsInstance<FirSimpleFunction>().forEach { builderFunction ->
      if (builderFunction.name == BUILDER_BUILD_METHOD_NAME) return@forEach
      if (!constructorParams.containsKey(builderFunction.name))
        reporter.unrecognizedBuilderMethod(builderFunction, context)

    }
  }
}

