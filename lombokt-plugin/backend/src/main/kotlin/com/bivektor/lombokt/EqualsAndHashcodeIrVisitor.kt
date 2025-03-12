package com.bivektor.lombokt

import com.bivektor.lombokt.LomboktNames.EQUALS_METHOD_NAME
import com.bivektor.lombokt.LomboktNames.HASHCODE_METHOD_NAME
import com.bivektor.lombokt.PluginKeys.EqualsHashCodeKey
import getConstValueByName
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.isNullableAny
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class EqualsAndHashCodeIrVisitor(
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
    val methodName = declaration.name
    if (!declaration.isGeneratedByPluginKey(EqualsHashCodeKey)) {
      return super.visitSimpleFunction(declaration)
    }

    val parentClass = declaration.parent
    require(parentClass is IrClass) { "Function ${declaration.name} is not a member of a class" }
    val annotation = getAnnotationAttributes(parentClass)
    val propertiesToUse = getFieldsToUse(annotation, parentClass)

    declaration.body = when (methodName) {
      EQUALS_METHOD_NAME -> generateEqualsMethodBody(propertiesToUse, declaration, annotation)
      HASHCODE_METHOD_NAME -> generateHashCodeMethodBody(propertiesToUse, declaration, annotation)
      else -> error("Unknown method name: $methodName")
    }
  }

  private fun generateEqualsMethodBody(
    fields: List<IrField>,
    fn: IrSimpleFunction,
    annotation: AnnotationAttrs,
  ): IrBlockBody {
    val thisParam = fn.dispatchReceiverParameter!!

    // Get the "other" parameter
    val otherParam = fn.valueParameters[0]

    return DeclarationIrBuilder(pluginContext, fn.symbol).irBlockBody(fn) {

      val thisRef = irGet(thisParam)
      val otherParamRef = irGet(otherParam)

      if (annotation.callSuper) {
        val superFn = resolveSuperFunction(fn) {
          it.name == EQUALS_METHOD_NAME && it.valueParameters.size == 1 && it.valueParameters[0].type.isNullableAny()
        }

        +irIfThen(
          condition = irNot(irCall(superFn.symbol).apply {
            dispatchReceiver = thisRef
            superQualifierSymbol = superFn.parentAsClass.symbol
            putValueArgument(0, otherParamRef)
          }),
          thenPart = irReturn(irFalse())
        )
      }

      // Perform identity check first
      +irIfThen(
        condition = irEquals(thisRef, otherParamRef, IrStatementOrigin.Companion.EQEQEQ),
        thenPart = irReturn(irTrue())
      )

      // Check if other is null or not the same type
      val instanceCheck = irIs(otherParamRef, fn.parentAsClass.defaultType)
      +irIfThen(
        condition = irNot(instanceCheck),
        thenPart = irReturn(irFalse())
      )

      val castedOther = irTemporary(irAs(otherParamRef, fn.parentAsClass.defaultType))
      val castedOtherRef = irGet(castedOther)

      for (field in fields) {
        val thisValue = irGetField(thisRef, field)
        val otherValue = irGetField(castedOtherRef, field)

        +irIfThen(
          condition = irNot(irEquals(thisValue, otherValue)),
          thenPart = irReturn(irFalse())
        )
      }

      +irReturn(irTrue())
    }
  }

  @OptIn(UnsafeDuringIrConstructionAPI::class)
  private fun generateHashCodeMethodBody(
    fields: List<IrField>,
    fn: IrSimpleFunction,
    annotation: AnnotationAttrs,
  ): IrBlockBody {

    // Using java.util.Objects.hashCode() method
    val hashCodeSymbol = pluginContext.referenceClass(
      ClassId.Companion.topLevel(FqName("java.util.Objects"))
    )!!.owner.functions.single { it.name.asString() == "hashCode" && it.valueParameters.size == 1 }.symbol

    return DeclarationIrBuilder(pluginContext, fn.symbol).irBlockBody(fn) {
      val thisRef = irGet(fn.dispatchReceiverParameter!!)

      // Start with a prime number for better distribution
      val initialValue: IrExpression = if (annotation.callSuper) {
        val superFn = resolveSuperFunction(fn) {
          it.name == HASHCODE_METHOD_NAME && it.valueParameters.isEmpty()
        }

        irCall(superFn.symbol).apply {
          dispatchReceiver = thisRef
          superQualifierSymbol = superFn.parentAsClass.symbol
        }
      } else irInt(17)

      val resultVar = createTmpVariable(
        initialValue,
        nameHint = "result",
        isMutable = true,
        irType = pluginContext.irBuiltIns.intType
      )

      for (field in fields) {
        val value = irGetField(thisRef, field)
        val hashValue = irCall(hashCodeSymbol, pluginContext.irBuiltIns.intType).apply {
          putValueArgument(0, value)
        }

        // result = 31 * result + hashValue
        +irSet(
          resultVar,
          irCallOp(
            pluginContext.irBuiltIns.intPlusSymbol,
            pluginContext.irBuiltIns.intType,
            irCallOp(
              pluginContext.irBuiltIns.intTimesSymbol,
              pluginContext.irBuiltIns.intType,
              irInt(31),
              irGet(resultVar)
            ),
            hashValue
          )
        )
      }

      // Return the computed result
      +irReturn(irGet(resultVar))
    }
  }

  @OptIn(UnsafeDuringIrConstructionAPI::class)
  private fun resolveSuperFunction(fn: IrSimpleFunction, predicate: (IrSimpleFunction) -> Boolean): IrSimpleFunction {
    return (fn.parentAsClass.superClass ?: pluginContext.irBuiltIns.anyClass.owner)
      .functions
      .single(predicate)
  }

  private fun getAnnotationAttributes(klass: IrClass): AnnotationAttrs {
    val annotation = klass.annotations.findAnnotation(LomboktNames.EQUALS_HASHCODE_ANNOTATION_NAME)
    require(annotation != null) { "Class ${klass.name} is not annotated with @" + LomboktNames.EQUALS_HASHCODE_ANNOTATION_NAME }
    return parseAnnotationAttributes(annotation)
  }

  private fun getFieldsToUse(
    annotation: AnnotationAttrs,
    klass: IrClass
  ): List<IrField> {
    val matchingFields = mutableListOf<IrField>()

    klass.acceptChildrenVoid(object : IrVisitorVoid() {
      override fun visitProperty(declaration: IrProperty) {
        if (declaration.origin != IrDeclarationOrigin.DEFINED) return

        var mode = getIncludeMode(declaration.annotations)
        if (declaration.backingField == null) {
          if (mode != null)
            messageCollector.report(
              CompilerMessageSeverity.ERROR,
              "Property ${klass.packageFqName}.${klass.name}.${declaration.name} is annotated with @EqualsAndHashCode.[Exclude|Include] but has no backing field"
            )
        } else {
          mode = mode ?: getIncludeMode(declaration.backingField!!.annotations)
          if (isIncludedMode(annotation, mode))
            matchingFields.add(declaration.backingField!!)
        }
      }

      override fun visitField(declaration: IrField) {
        // Property fields will be handled when visiting properties
        if (declaration.isPropertyField) return

        val mode = getIncludeMode(declaration.annotations)
        if (isIncludedMode(annotation, mode))
          matchingFields.add(declaration)
      }
    })

    return matchingFields.sortedBy { it.startOffset }
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
    val onlyExplicitlyIncluded = annotation.getConstValueByName("onlyExplicitlyIncluded", false)
    val callSuper = annotation.getConstValueByName("callSuper", false)

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

private val INCLUDE_ANNOTATION_NAME = LomboktNames.EQUALS_HASHCODE_ANNOTATION_NAME.child(Name.identifier("Include"))
private val EXCLUDE_ANNOTATION_NAME = LomboktNames.EQUALS_HASHCODE_ANNOTATION_NAME.child(Name.identifier("Exclude"))

