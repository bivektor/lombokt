package com.bivektor.lombokt.ir

import com.bivektor.lombokt.LomboktNames
import com.bivektor.lombokt.PluginKeys
import com.bivektor.lombokt.isGeneratedByPluginKey
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irConcat
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.findAnnotation
import org.jetbrains.kotlin.ir.util.isPropertyField
import org.jetbrains.kotlin.ir.util.parentAsClass
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
      var result = irConcat()
      result.arguments.add(irString(parentClass.name.asString() + "("))

      if (annotation.callSuper) {
        val superFn = fn.overriddenSymbols.single().owner

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
          val config = getPropertyConfig(declaration)
          if (!config.isIncluded(annotation)) return

          if (annotation.doNotUseGetters && declaration.backingField != null) {
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

  private fun getAnnotationAttributes(klass: IrClass): AnnotationConfig {
    val annotation = klass.annotations.findAnnotation(LomboktNames.TO_STRING_ANNOTATION_NAME)
    require(annotation != null) { "Class ${klass.name} is not annotated with @" + LomboktNames.TO_STRING_ANNOTATION_NAME }
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

  private fun ElementConfig?.isIncluded(annotation: AnnotationConfig): Boolean {
    return if (annotation.onlyExplicitlyIncluded)
      this?.includeOption == true
    else this?.includeOption != false
  }
}

private val INCLUDE_ANNOTATION_NAME = LomboktNames.TO_STRING_ANNOTATION_NAME.child(Name.identifier("Include"))
private val EXCLUDE_ANNOTATION_NAME = LomboktNames.TO_STRING_ANNOTATION_NAME.child(Name.identifier("Exclude"))