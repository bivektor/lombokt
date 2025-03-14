package com.bivektor.lombokt.ir

import com.bivektor.lombokt.LomboktNames
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.hasDefaultValue
import org.jetbrains.kotlin.ir.util.isNullConst
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.Name

object BuildableIrBodyGenerator {

  fun processClass(irClass: IrClass, pluginContext: IrPluginContext) {
    if (!isBuilderClass(irClass)) return
    irClass.acceptChildrenVoid(Visitor(pluginContext, irClass))
  }

  private fun isBuilderClass(irClass: IrClass): Boolean {
    return irClass.hasAnnotation(LomboktNames.BUILDABLE_BUILDER_ANNOTATION_NAME)
  }

  private class Visitor(
    private val pluginContext: IrPluginContext,
    builderClass: IrClass
  ) : IrVisitorVoid() {
    private val parentClass = builderClass.parentAsClass

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private val parentClassConstructor = parentClass.primaryConstructor!!

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private val constructorArguments = parentClassConstructor.valueParameters

    private val constructorArgumentsByName = constructorArguments.associateBy { it.name }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private val propertiesByName = builderClass.properties.filter { it.backingField != null }.associateBy { it.name }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
      val functionName = declaration.name
      val functionBuilder = BuildableFunctionBuilder(declaration)
      if (functionName == LomboktNames.BUILDABLE_BUILD_METHOD_NAME) {
        declaration.body = functionBuilder.blockBody { functionBuilder.generateBuildMethodBody() }
        return
      }

      if (constructorArguments.any { it.name == functionName }) {
        declaration.body = functionBuilder.blockBody { functionBuilder.generateSetterBody() }
        return
      }
    }

    private inner class BuildableFunctionBuilder(
      private val irFunction: IrSimpleFunction,
      startOffset: Int = SYNTHETIC_OFFSET,
      endOffset: Int = SYNTHETIC_OFFSET,
    ) : IrBlockBodyBuilder(pluginContext, Scope(irFunction.symbol), startOffset, endOffset) {

      fun generateSetterBody() {
        +irSetProperty(irThis(), irFunction.name, irGet(irFunction.valueParameters[0]))
        val parameter = constructorArgumentsByName.getValue(irFunction.name)
        if (parameter.hasDefaultValue() && !parameter.defaultValue!!.expression.isNullConst())
          +irSetProperty(irThis(), Name.identifier("${irFunction.name}Set"), irTrue())

        +irReturn(irThis())
      }

      @OptIn(UnsafeDuringIrConstructionAPI::class)
      fun generateBuildMethodBody() {
        for (parameter in constructorArguments) {
          if (!parameter.type.isNullable() && !parameter.hasDefaultValue()) {
            +irIfThen(
              irIsPropertyNotSet(irThis(), parameter),
              irThrow(irCall(context.irBuiltIns.illegalArgumentExceptionSymbol).apply {
                putValueArgument(0, irString("${parameter.name} is required but was not set"))
              })
            )
          }
        }

        +irReturn(
          irCallConstructor(parentClassConstructor.symbol, emptyList()).apply {
            for ((index, parameter) in constructorArguments.withIndex()) {
              putValueArgument(index, parameter.toCallArgument())
            }
          }
        )
      }

      private fun IrValueParameter.toCallArgument(): IrExpression {
        val parameterValue = irGetProperty(irThis(), name)
        return if (hasDefaultValue()) {
          if (defaultValue!!.expression.isNullConst()) return parameterValue
          irIfThenElse(parameterValue.type, irIsPropertyNotSet(irThis(), this), defaultValue!!.expression, parameterValue)
        } else {
          parameterValue
        }
      }

      private fun irIsPropertyNotSet(receiver: IrExpression, parameter: IrValueParameter): IrExpression {
        if (parameter.type.isNullable() && parameter.hasDefaultValue() && !parameter.defaultValue!!.expression.isNullConst())
          return irNot(irGetIsSetProperty(receiver, parameter.name))

        return irEqualsNull(irGetProperty(receiver, parameter.name))
      }

      private fun irGetIsSetProperty(receiver: IrExpression, name: Name): IrExpression {
        val flagName = "${name}Set"
        val property = propertiesByName.getValue(Name.identifier(flagName))
        return irCall(property.getter!!).apply {
          dispatchReceiver = receiver
        }
      }

      private fun irSetProperty(receiver: IrExpression, propertyName: Name, value: IrExpression): IrExpression {
        val property = propertiesByName.getValue(propertyName)
        return irCall(property.setter!!).apply {
          putValueArgument(0, value)
          dispatchReceiver = receiver
        }
      }

      @OptIn(UnsafeDuringIrConstructionAPI::class)
      fun irGetProperty(receiver: IrExpression, propertyName: Name): IrExpression {
        return irCall(propertiesByName.getValue(propertyName).getter!!).apply {
          dispatchReceiver = receiver
        }
      }

      fun irThis(): IrExpression {
        val irDispatchReceiverParameter = irFunction.dispatchReceiverParameter!!
        return IrGetValueImpl(
          startOffset, endOffset,
          irDispatchReceiverParameter.type,
          irDispatchReceiverParameter.symbol
        )
      }
    }

  }

}
