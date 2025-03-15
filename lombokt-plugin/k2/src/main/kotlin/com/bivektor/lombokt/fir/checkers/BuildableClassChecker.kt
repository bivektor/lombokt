package com.bivektor.lombokt.fir.checkers

import com.bivektor.lombokt.LomboktNames.BUILDABLE_ANNOTATION_ID
import com.bivektor.lombokt.LomboktNames.BUILDER_ANNOTATION_ID
import com.bivektor.lombokt.LomboktNames.BUILDER_BUILD_METHOD_NAME
import com.bivektor.lombokt.fir.checkers.LomboktDiagnostics.BUILDER_INVALID_METHOD_SIGNATURE
import com.bivektor.lombokt.fir.services.BuildableService
import com.bivektor.lombokt.fir.services.buildableService
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.getContainingDeclaration
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.Name

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
        LomboktDiagnostics.UNSUPPORTED_CLASS_TYPE,
        "Builder class must be a regular class, not an object, interface, inner, inline, value or enum class.",
        context
      )
    }

    if (!service.isSuitableBuildableClassType(buildableClass.symbol)) {
      return reporter.reportOn(
        buildableClass.source,
        LomboktDiagnostics.UNSUPPORTED_CLASS_TYPE,
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
    val builderMethods = builderClass.declarations.filterIsInstance<FirSimpleFunction>()
      .filter { constructorParams.contains(it.name) || it.name == BUILDER_BUILD_METHOD_NAME }
      .associateBy { it.name }

    constructorParams.values.forEach { param ->
      val builderMethod = builderMethods[param.name]
      if (builderMethod == null)
        reporter.reportOn(
          builderClass.source,
          LomboktDiagnostics.BUILDER_MISSING_METHOD,
          "Builder class must have a method for each constructor parameter but '${param.name}' is missing",
          context
        )
      else
        checkBuilderMethod(builderClass, param, builderMethod, context, reporter)
    }

    checkBuilderBuildMethod(builderClass, buildableClass, builderMethods, reporter, context)

    for (child in builderClass.declarations) {
      if (child !is FirProperty && child !is FirSimpleFunction) continue
      if (child is FirSimpleFunction && (child.name == BUILDER_BUILD_METHOD_NAME || builderMethods.contains(child.name))) continue
      return reporter.reportOn(
        child.source,
        LomboktDiagnostics.UNSUPPORTED_CLASS_TYPE,
        "Builder class must not have any other members than the property setter methods and the '$BUILDER_BUILD_METHOD_NAME' method",
        context
      )
    }
  }

  private fun checkBuilderBuildMethod(
    builderClass: FirClass,
    buildableClass: FirClass,
    builderMethods: Map<Name, FirSimpleFunction>,
    reporter: DiagnosticReporter,
    context: CheckerContext
  ) {
    val buildMethodName = BUILDER_BUILD_METHOD_NAME
    val buildMethod = builderMethods[buildMethodName]
    if (buildMethod == null)
      return reporter.reportOn(
        builderClass.source,
        LomboktDiagnostics.BUILDER_MISSING_METHOD,
        "Builder class must have a '$buildMethodName' method",
        context
      )

    if (buildMethod.valueParameters.isNotEmpty() || buildMethod.returnTypeRef.coneType != buildableClass.symbol.defaultType())
      return reporter.reportOn(
        buildMethod.source,
        BUILDER_INVALID_METHOD_SIGNATURE,
        "Builder '$buildMethodName' method must not have any parameters and must return the buildable class '${buildableClass.symbol.name}'",
        context
      )
  }

  private fun checkBuilderMethod(
    builderClass: FirClass,
    constructorParam: FirValueParameterSymbol,
    builderMethod: FirSimpleFunction,
    context: CheckerContext,
    reporter: DiagnosticReporter
  ) {

    fun reportInvalidSignature(message: String, source: KtSourceElement? = builderMethod.source) {
      reporter.reportOn(
        source,
        BUILDER_INVALID_METHOD_SIGNATURE,
        message,
        context
      )
    }

    if (builderMethod.returnTypeRef.coneType != builderClass.symbol.defaultType())
      return reportInvalidSignature("Builder method must return the same type as the builder class")

    val params = builderMethod.valueParameters
    if (params.size != 1)
      return reportInvalidSignature("Builder method must have exactly one parameter of the same type as the associated constructor parameter")

    val firstParam = params.first()
    if (firstParam.returnTypeRef.coneType != constructorParam.resolvedReturnType)
      return reportInvalidSignature("Invalid builder method parameter type. Expected type: '${constructorParam.resolvedReturnType}'")
  }
}

