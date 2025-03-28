package com.bivektor.lombokt.ir

import com.bivektor.lombokt.LomboktNames
import com.bivektor.lombokt.LomboktNames.EQUALS_METHOD_NAME
import com.bivektor.lombokt.LomboktNames.HASHCODE_METHOD_NAME
import com.bivektor.lombokt.PluginKeys
import com.bivektor.lombokt.isGeneratedByPluginKey
import getConstValueByName
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImplWithShape
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isArray
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.types.isNullableAny
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.Name

private val ANNOTATION_NAME = LomboktNames.EQUALS_HASHCODE_ANNOTATION_NAME

class EqualsAndHashCodeIrBodyGenerator(
  private val irClass: IrClass,
  private val pluginContext: IrPluginContext,
  private val messageCollector: MessageCollector,
) {
  private val annotationConfig: EqualsAndHashCodeAnnotationConfig? =
    irClass.getAnnotation(ANNOTATION_NAME)?.toAnnotationConfig()

  @OptIn(UnsafeDuringIrConstructionAPI::class)
  private val primaryConstructorParams =
    if (annotationConfig != null && irClass.isData) irClass.primaryConstructor!!.valueParameters.map { it.name } else emptyList()

  @OptIn(UnsafeDuringIrConstructionAPI::class)
  @Suppress("UNUSED_ANONYMOUS_PARAMETER")
  private val propertiesToUse = annotationConfig?.let { _ ->
    irClass.properties.filter { it.isIncluded }.toList()
  } ?: emptyList()

  private val functionsVisitor = object : IrVisitorVoid() {
    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
      if (!declaration.isGeneratedByPluginKey(PluginKeys.EqualsHashCodeKey)) return
      val methodName = declaration.name
      val functionBuilder =
        EqualsAndHashCodeFunctionBuilder(pluginContext, irClass, declaration, propertiesToUse, annotationConfig!!)
      declaration.body = functionBuilder.blockBody {
        when (methodName) {
          EQUALS_METHOD_NAME -> functionBuilder.generateEqualsMethodBody()
          HASHCODE_METHOD_NAME -> functionBuilder.generateHashCodeMethodBody(17)
          else -> error("Unknown method name: $methodName")
        }
      }
    }
  }

  fun processClass() {
    if (annotationConfig == null) return
    irClass.acceptChildrenVoid(functionsVisitor)
  }

  private val IrProperty.isIncluded: Boolean
    get() {
      if (origin != IrDeclarationOrigin.DEFINED) return false
      val isEligible = backingField != null && !(irClass.isData && !primaryConstructorParams.contains(name))

      if (hasAnnotation(EXCLUDE_ANNOTATION_NAME)) return false

      val explicitlyIncluded = hasAnnotation(INCLUDE_ANNOTATION_NAME)
      if (explicitlyIncluded && !isEligible)
        messageCollector.report(
          CompilerMessageSeverity.EXCEPTION,
          "Property '$name' on class '${parent.kotlinFqName}' cannot be used for equals/hashCode generation"
        )

      if (!isEligible) return false
      if (explicitlyIncluded) return true
      return !annotationConfig!!.onlyExplicitlyIncluded
    }

  private fun IrConstructorCall.toAnnotationConfig(): EqualsAndHashCodeAnnotationConfig {
    val onlyExplicitlyIncluded = getConstValueByName("onlyExplicitlyIncluded", false)
    val callSuper = getConstValueByName("callSuper", false)
    val doNotUseGetters = getConstValueByName("doNotUseGetters", false)

    return EqualsAndHashCodeAnnotationConfig(
      onlyExplicitlyIncluded = onlyExplicitlyIncluded,
      callSuper = callSuper,
      doNotUseGetters = doNotUseGetters,
    )
  }
}

private class EqualsAndHashCodeFunctionBuilder(
  context: IrGeneratorContext,
  val irClass: IrClass,
  val irFunction: IrSimpleFunction,
  val propertiesToUse: List<IrProperty>,
  val config: EqualsAndHashCodeAnnotationConfig,
  startOffset: Int = SYNTHETIC_OFFSET,
  endOffset: Int = SYNTHETIC_OFFSET,
) : AbstractClassFunctionBlockBodyBuilder(irFunction, context, Scope(irFunction.symbol), startOffset, endOffset) {

  private fun irOther(): IrExpression {
    val irFirstParameter = irFunction.valueParameters[0]
    return IrGetValueImpl(
      startOffset, endOffset,
      irFirstParameter.type,
      irFirstParameter.symbol
    )
  }

  fun generateEqualsMethodBody() {
    val irType = irClass.defaultType
    if (config.callSuper) {
      val superFn = resolveSuperFunction(irFunction) {
        it.name == EQUALS_METHOD_NAME && it.valueParameters.size == 1 && it.valueParameters[0].type.isNullableAny()
      }

      +irIfThenReturnFalse(
        irNot(irCall(superFn.symbol).apply {
          dispatchReceiver = irThis()
          superQualifierSymbol = superFn.parentAsClass.symbol
          putValueArgument(0, irOther())
        })
      )
    }

    +irIfThenReturnFalse(irNotIs(irOther(), irType))
    val otherWithCast = irTemporary(irAs(irOther(), irType), "other_with_cast")
    for (property in propertiesToUse) {
      val arg1 = irGetProperty(irThis(), property, config.doNotUseGetters)
      val arg2 = irGetProperty(irGet(irType, otherWithCast.symbol), property, config.doNotUseGetters)
      +irIfThenReturnFalse(
        IrCallImplWithShape(
          startOffset = startOffset,
          endOffset = endOffset,
          type = context.irBuiltIns.booleanType,
          symbol = context.irBuiltIns.booleanNotSymbol,
          typeArgumentsCount = 0,
          valueArgumentsCount = 0,
          contextParameterCount = 0,
          hasDispatchReceiver = true,
          hasExtensionReceiver = false,
          origin = IrStatementOrigin.EXCLEQ,
        ).apply<IrCallImpl> {
          dispatchReceiver =
            this@EqualsAndHashCodeFunctionBuilder.irEquals(arg1, arg2, origin = IrStatementOrigin.EXCLEQ)
        }
      )
    }
    +irReturnTrue()
  }

  @OptIn(UnsafeDuringIrConstructionAPI::class)
  fun generateHashCodeMethodBody(constHashCode: Int) {
    val initialValue = if (config.callSuper) {
      val superFn = resolveSuperFunction(irFunction) { it.name == HASHCODE_METHOD_NAME && it.valueParameters.isEmpty() }
      irCall(superFn.symbol).apply {
        dispatchReceiver = irThis()
        superQualifierSymbol = superFn.parentAsClass.symbol
      }
    } else irInt(constHashCode)

    if (propertiesToUse.isEmpty()) {
      +irReturn(initialValue)
      return
    }

    val irIntType = context.irBuiltIns.intType

    val irResultVar = IrVariableImpl(
      startOffset, endOffset,
      IrDeclarationOrigin.DEFINED,
      IrVariableSymbolImpl(),
      Name.identifier("result"), irIntType,
      isVar = true, isConst = false, isLateinit = false
    ).also {
      it.parent = irFunction
      it.initializer = initialValue
    }
    +irResultVar

    for (property in propertiesToUse) {
      val shiftedResult = shiftResultOfHashCode(irResultVar)
      val irRhs = IrCallImplWithShape(
        startOffset,
        endOffset,
        type = irIntType,
        symbol = context.irBuiltIns.intPlusSymbol,
        typeArgumentsCount = 0,
        valueArgumentsCount = 1,
        contextParameterCount = 0,
        hasDispatchReceiver = true,
        hasExtensionReceiver = false,
      ).apply {
        dispatchReceiver = shiftedResult
        putValueArgument(0, getHashCodeOfProperty(property, config.doNotUseGetters))
      }
      +irSet(irResultVar.symbol, irRhs)
    }

    +irReturn(irGet(irResultVar))
  }

  private fun irGetProperty(
    receiver: IrExpression,
    property: IrProperty,
    doNotUseGetter: Boolean,
  ): IrExpression {
    if (doNotUseGetter) return irGetField(receiver, property.backingField!!)
    return irGetProperty(receiver, property)
  }

  private fun IrBuilderWithScope.shiftResultOfHashCode(irResultVar: IrVariable): IrExpression =
    IrCallImplWithShape(
      startOffset = startOffset,
      endOffset = endOffset,
      symbol = context.irBuiltIns.intTimesSymbol,
      type = context.irBuiltIns.intType,
      typeArgumentsCount = 0,
      valueArgumentsCount = 1,
      contextParameterCount = 0,
      hasDispatchReceiver = true,
      hasExtensionReceiver = false,
    ).apply {
      dispatchReceiver = irGet(irResultVar)
      putValueArgument(0, irInt(31))
    }

  @OptIn(UnsafeDuringIrConstructionAPI::class)
  private fun resolveSuperFunction(fn: IrSimpleFunction, predicate: (IrSimpleFunction) -> Boolean): IrSimpleFunction {
    return (fn.parentAsClass.superClass ?: context.irBuiltIns.anyClass.owner)
      .functions
      .single(predicate)
  }

  private fun getHashCodeOfProperty(property: IrProperty, doNotUseGetter: Boolean): IrExpression {
    return when {
      property.isLateinit || property.backingField!!.type.isNullable() ->
        irIfNull(
          context.irBuiltIns.intType,
          irGetProperty(irThis(), property, doNotUseGetter),
          irInt(0),
          getHashCodeOf(this, property, irGetProperty(irThis(), property, doNotUseGetter))
        )

      else -> getHashCodeOf(this, property, irGetProperty(irThis(), property, doNotUseGetter))
    }
  }

  private fun getHashCodeOf(builder: IrBuilderWithScope, property: IrProperty, irValue: IrExpression): IrExpression {
    return builder.getHashCodeOf(getHashCodeFunctionInfo(property), irValue)
  }

  @OptIn(UnsafeDuringIrConstructionAPI::class)
  private fun IrBuilderWithScope.getHashCodeOf(
    hashCodeFunctionSymbol: IrSimpleFunctionSymbol,
    irValue: IrExpression
  ): IrExpression {
    val hasDispatchReceiver = hashCodeFunctionSymbol.owner.dispatchReceiverParameter != null
    return IrCallImplWithShape(
      startOffset = startOffset,
      endOffset = endOffset,
      symbol = hashCodeFunctionSymbol,
      type = context.irBuiltIns.intType,
      valueArgumentsCount = if (hasDispatchReceiver) 0 else 1,
      contextParameterCount = 0,
      hasDispatchReceiver = hasDispatchReceiver,
      hasExtensionReceiver = false,
      typeArgumentsCount = 0,
    ).apply {
      if (hasDispatchReceiver) {
        dispatchReceiver = irValue
      } else {
        putValueArgument(0, irValue)
      }
    }
  }

  @OptIn(UnsafeDuringIrConstructionAPI::class)
  private fun IrBuilderWithScope.getHashCodeFunctionInfo(property: IrProperty): IrSimpleFunctionSymbol {
    val type = property.backingField!!.type
    return if (type.isArray() || type.isPrimitiveArray()) {
      context.irBuiltIns.dataClassArrayMemberHashCodeSymbol
    } else {
      type.classOrNull?.functions?.singleOrNull { it.owner.isHashCode() }
        ?: context.irBuiltIns.anyClass.functions.single { it.owner.name.asString() == "hashCode" }
    }
  }
}

private data class EqualsAndHashCodeAnnotationConfig(
  val onlyExplicitlyIncluded: Boolean,
  val callSuper: Boolean,
  val doNotUseGetters: Boolean
)

private val INCLUDE_ANNOTATION_NAME = LomboktNames.EQUALS_HASHCODE_ANNOTATION_NAME.child(Name.identifier("Include"))
private val EXCLUDE_ANNOTATION_NAME = LomboktNames.EQUALS_HASHCODE_ANNOTATION_NAME.child(Name.identifier("Exclude"))

