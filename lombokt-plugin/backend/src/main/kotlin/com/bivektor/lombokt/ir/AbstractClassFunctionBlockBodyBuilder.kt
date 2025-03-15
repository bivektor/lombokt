package com.bivektor.lombokt.ir

import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl

abstract class AbstractClassFunctionBlockBodyBuilder(
  private val irFunction: IrFunction,
  context: IrGeneratorContext,
  scope: Scope,
  startOffset: Int,
  endOffset: Int
) : IrBlockBodyBuilder(context, scope, startOffset, endOffset) {

  protected fun irThis(): IrExpression {
    val irDispatchReceiverParameter = irFunction.dispatchReceiverParameter!!
    return IrGetValueImpl(
      startOffset, endOffset,
      irDispatchReceiverParameter.type,
      irDispatchReceiverParameter.symbol
    )
  }

  protected fun irGetThisProperty(property: IrProperty) = with(property) {
    irCall(getter!!).apply {
      dispatchReceiver = irThis()
    }
  }

  protected fun irGetThisField(field: IrField) = irGetField(irThis(), field)
}
