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
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.findAnnotation
import org.jetbrains.kotlin.ir.util.getValueArgument
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
    annotation: AnnotationAttrs
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
          val mode = getIncludeMode(declaration.annotations) ?: getIncludeMode(getter.annotations)
          if (!isIncludedMode(annotation, mode)) return

          hasIncludedField = true
          result.arguments.add(irString(declaration.name.asString() + "="))
          result.arguments.add(irCall(getter.symbol).apply {
            dispatchReceiver = thisRef
          })

          result.arguments.add(irString(", "))
        }

        override fun visitField(declaration: IrField) {
          if (declaration.isPropertyField) return
          val mode = getIncludeMode(declaration.annotations)
          if (!isIncludedMode(annotation, mode)) return

          hasIncludedField = true
          result.arguments.add(irString(declaration.name.asString() + "="))
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

  private fun getAnnotationAttributes(klass: IrClass): AnnotationAttrs {
    val annotation = klass.annotations.findAnnotation(LomboktNames.TO_STRING_ANNOTATION_NAME)
    require(annotation != null) { "Class ${klass.name} is not annotated with @" + LomboktNames.TO_STRING_ANNOTATION_NAME }
    return parseAnnotationAttributes(annotation)
  }

  private fun getIncludeMode(annotations: List<IrConstructorCall>): IncludeMode? {
    if (annotations.findAnnotation(EXCLUDE_ANNOTATION_NAME) != null) return IncludeMode.EXCLUDE
    if (annotations.findAnnotation(INCLUDE_ANNOTATION_NAME) != null) return IncludeMode.INCLUDE
    return null
  }

  private fun isIncludedMode(annotation: AnnotationAttrs, mode: IncludeMode?): Boolean {
    return if (annotation.onlyExplicitlyIncluded)
      mode == IncludeMode.INCLUDE
    else mode != IncludeMode.EXCLUDE
  }

  private fun parseAnnotationAttributes(annotation: IrConstructorCall): AnnotationAttrs {
    val onlyExplicitlyIncluded =
      (annotation.getValueArgument(Name.identifier("onlyExplicitlyIncluded")) as? IrConst)?.value as? Boolean == true

    val callSuper = (annotation.getValueArgument(Name.identifier("callSuper")) as? IrConst)?.value as? Boolean == true

    return AnnotationAttrs(
      onlyExplicitlyIncluded = onlyExplicitlyIncluded,
      callSuper = callSuper
    )
  }

  private data class AnnotationAttrs(
    val onlyExplicitlyIncluded: Boolean,
    val callSuper: Boolean
  )

  private enum class IncludeMode {
    INCLUDE, EXCLUDE
  }
}

private val INCLUDE_ANNOTATION_NAME = LomboktNames.TO_STRING_ANNOTATION_NAME.child(Name.identifier("Include"))
private val EXCLUDE_ANNOTATION_NAME = LomboktNames.TO_STRING_ANNOTATION_NAME.child(Name.identifier("Exclude"))