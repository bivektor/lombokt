package com.bivektor.lombokt

import com.bivektor.lombokt.LomboktNames.TO_STRING_ANNOTATION_NAME
import com.bivektor.lombokt.LomboktNames.TO_STRING_METHOD_NAME
import getConstValueByName
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.Name

class ToStringIrVisitor(
  private val pluginContext: IrPluginContext,
  private val messageCollector: MessageCollector
) : IrVisitorVoid() {

  override fun visitElement(element: IrElement) {
    when (element) {
      is IrDeclaration,
      is IrFile,
      is IrBlockBody,
      is IrModuleFragment -> element.acceptChildrenVoid(this)
    }
  }

  override fun visitSimpleFunction(declaration: IrSimpleFunction) {
    if (!declaration.isGeneratedByPluginKey(PluginKeys.ToStringKey)) {
      return super.visitSimpleFunction(declaration)
    }

    val classDeclaration = declaration.parent
    require(classDeclaration is IrClass) { "Function ${declaration.name} is not a member of a class" }
    val annotation = getAnnotationAttributes(classDeclaration)
    declaration.body = generateMethodBody(declaration, annotation)
  }

  @OptIn(UnsafeDuringIrConstructionAPI::class)
  private fun generateMethodBody(
    fn: IrSimpleFunction,
    annotation: AnnotationConfig
  ): IrBlockBody {
    val parentClass = fn.parentAsClass
    return DeclarationIrBuilder(pluginContext, fn.symbol).irBlockBody(fn) {
      val thisRef = irGet(fn.dispatchReceiverParameter!!)
      val result = irConcat()
      result.arguments.add(irString(parentClass.name.asString() + "("))

      if (annotation.callSuper) {
        val superFn = resolveSuperFunction(fn)
        result.arguments.add(irString("super="))
        result.arguments.add(
          irCall(superFn.symbol).apply {
            dispatchReceiver = thisRef
            superQualifierSymbol = superFn.parentAsClass.symbol
          })

        result.arguments.add(irString(", "))
      }

      var hasIncludedField = false
      parentClass.acceptChildrenVoid(object : IrVisitorVoid() {

        override fun visitProperty(declaration: IrProperty) {
          if (declaration.origin != IrDeclarationOrigin.DEFINED) return
          val getter = declaration.getter ?: return
          val hasBackingField = declaration.backingField != null

          val config = getPropertyConfig(declaration)
          if (!config.isIncluded(annotation, !hasBackingField)) return

          if (annotation.doNotUseGetters && hasBackingField) {
            handleField(declaration.backingField!!, config)
            return
          }

          hasIncludedField = true
          val outputName = config?.customName ?: declaration.name.asString()
          result.arguments.add(irString("$outputName="))
          result.arguments.add(irCall(getter.symbol).apply {
            dispatchReceiver = thisRef
          })

          result.arguments.add(irString(", "))
        }

        override fun visitField(declaration: IrField) {
          // Property fields are handled by visitProperty
          if (declaration.isPropertyField) return
          val config = getElementConfig(declaration.annotations)
          if (!config.isIncluded(annotation)) return
          handleField(declaration, config)
        }

        private fun handleField(declaration: IrField, config: ElementConfig?) {
          hasIncludedField = true
          val outputName = config?.customName ?: declaration.name.asString()
          result.arguments.add(irString("$outputName="))
          result.arguments.add(irGetField(thisRef, declaration))
          result.arguments.add(irString(", "))
        }
      })

      if (hasIncludedField) {
        result.arguments.removeAt(result.arguments.size - 1)
      }

      result.arguments.add(irString(")"))
      +irReturn(result)
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
        it.name == TO_STRING_METHOD_NAME && it.valueParameters.isEmpty()
      }!!
  }

  private fun getAnnotationAttributes(klass: IrClass): AnnotationConfig {
    val annotation = requireNotNull(klass.annotations.findAnnotation(TO_STRING_ANNOTATION_NAME)) {
      "Class ${klass.kotlinFqName} is not annotated with @" + TO_STRING_ANNOTATION_NAME
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

private val INCLUDE_ANNOTATION_NAME = TO_STRING_ANNOTATION_NAME.child(Name.identifier("Include"))
private val EXCLUDE_ANNOTATION_NAME = TO_STRING_ANNOTATION_NAME.child(Name.identifier("Exclude"))
