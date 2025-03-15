package com.bivektor.lombokt.ir

import com.bivektor.lombokt.LomboktNames
import com.bivektor.lombokt.PluginKeys
import com.bivektor.lombokt.isGeneratedByPluginKey
import getConstValueByName
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.Name

class ToStringIrBodyGenerator(
  private val pluginContext: IrPluginContext,
  private val messageCollector: MessageCollector
) {
  fun processSimpleFunction(declaration: IrSimpleFunction) {
    if (!declaration.isGeneratedByPluginKey(PluginKeys.ToStringKey)) return
    val classDeclaration = declaration.parent
    require(classDeclaration is IrClass) { "Function ${declaration.name} is not a member of a class" }
    val annotation = getAnnotationAttributes(classDeclaration)
    declaration.body = ToStringFunctionBodyBuilder(declaration, annotation).let { builder ->
      builder.blockBody {
        builder.generateMethodBody()
      }
    }
  }

  private inner class ToStringFunctionBodyBuilder(
    private val irFunction: IrSimpleFunction,
    private val annotation: AnnotationConfig,
  ) : AbstractClassFunctionBlockBodyBuilder (
    irFunction,
    pluginContext,
    Scope(irFunction.symbol),
    SYNTHETIC_OFFSET,
    SYNTHETIC_OFFSET
  ) {

    private val parentClass = irFunction.parentAsClass

    fun generateMethodBody() {
      val result = irConcat()
      result.arguments.add(irString(parentClass.name.asString() + "("))

      var isFirstArgument = true
      if (annotation.callSuper) {
        val superFn = resolveSuperFunction(irFunction)
        result.arguments.add(irString("super="))
        result.arguments.add(
          irCall(superFn.symbol).apply {
            dispatchReceiver = irThis()
            superQualifierSymbol = superFn.parentAsClass.symbol
          })

        isFirstArgument = false
      }

      parentClass.acceptChildrenVoid(object : IrVisitorVoid() {
        override fun visitProperty(declaration: IrProperty) {
          if (declaration.origin != IrDeclarationOrigin.Companion.DEFINED) return
          if (declaration.getter == null) return
          val hasBackingField = declaration.backingField != null

          val config = getPropertyConfig(declaration)
          if (!config.isIncluded(annotation, !hasBackingField)) return

          if (!isFirstArgument) result.arguments.add(irString(", "))
          isFirstArgument = false

          val outputName = config?.customName ?: declaration.name.asString()
          result.arguments.add(irString("$outputName="))
          result.arguments.add(irGetProperty(declaration, annotation.doNotUseGetters))
        }
      })

      result.arguments.add(irString(")"))

      +irReturn(result)
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun irGetProperty(property: IrProperty, doNotUseGetter: Boolean): IrExpression {
      with(property) {
        if (isLateinit || (doNotUseGetter && backingField != null))
          return irGetThisField(backingField!!)

        return irGetThisProperty(this)
      }
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun resolveSuperFunction(fn: IrSimpleFunction): IrSimpleFunction {
      val parentClass = fn.parentAsClass
      if (parentClass.superClass == null)
        messageCollector.report(
          CompilerMessageSeverity.WARNING,
          "ToString on ${parentClass.kotlinFqName} requires super call but the class has no super class"
        )

      return (parentClass.superClass ?: pluginContext.irBuiltIns.anyClass.owner)
        .functions
        .singleOrNull {
          it.name == LomboktNames.TO_STRING_METHOD_NAME && it.valueParameters.isEmpty()
        }!!
    }
  }

  private fun getAnnotationAttributes(klass: IrClass): AnnotationConfig {
    val annotation = requireNotNull(klass.annotations.findAnnotation(LomboktNames.TO_STRING_ANNOTATION_NAME)) {
      "Class ${klass.kotlinFqName} is not annotated with @" + LomboktNames.TO_STRING_ANNOTATION_NAME
    }

    return parseToStringAnnotation(annotation)
  }

  private fun parseToStringAnnotation(annotation: IrConstructorCall): AnnotationConfig {
    return AnnotationConfig(
      onlyExplicitlyIncluded = annotation.getConstValueByName("onlyExplicitlyIncluded", false),
      callSuper = annotation.getConstValueByName("callSuper", false),
      doNotUseGetters = annotation.getConstValueByName("doNotUseGetters", false),
    )
  }

  private fun getPropertyConfig(
    declaration: IrProperty
  ): ElementConfig? = getElementConfig(declaration.annotations) ?: getElementConfig(declaration.getter!!.annotations)

  private fun getElementConfig(annotations: List<IrConstructorCall>): ElementConfig? {
    if (annotations.findAnnotation(EXCLUDE_ANNOTATION_NAME) != null) return ElementConfig.ExcludeDefault
    val includeAnnotation = annotations.findAnnotation(INCLUDE_ANNOTATION_NAME) ?: return null
    val customName = includeAnnotation.getConstValueByName("name", "")
    if (customName.isBlank()) return ElementConfig.IncludeDefault
    return ElementConfig(includeOption = true, customName = customName)
  }

  private data class AnnotationConfig(
    val onlyExplicitlyIncluded: Boolean,
    val callSuper: Boolean,
    val doNotUseGetters: Boolean,
  )

  private data class ElementConfig(
    val includeOption: Boolean? = null,
    val customName: String? = null,
  ) {
    companion object {
      val IncludeDefault = ElementConfig(includeOption = true)
      val ExcludeDefault = ElementConfig(includeOption = false)
    }
  }

  private fun ElementConfig?.isIncluded(
    annotation: AnnotationConfig,
    requireExplicitInclude: Boolean = false
  ): Boolean {
    return if (annotation.onlyExplicitlyIncluded || requireExplicitInclude)
      this?.includeOption == true
    else this?.includeOption != false
  }
}

private val INCLUDE_ANNOTATION_NAME = LomboktNames.TO_STRING_ANNOTATION_NAME.child(Name.identifier("Include"))
private val EXCLUDE_ANNOTATION_NAME = LomboktNames.TO_STRING_ANNOTATION_NAME.child(Name.identifier("Exclude"))
