package com.bivektor.lombokt.ir

import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.util.getValueArgument
import org.jetbrains.kotlin.name.Name

@Suppress("UNCHECKED_CAST")
fun <T> IrConstructorCall.getConstValueByName(name: String, defaultValue: T): T {
  return (getValueArgument(Name.identifier(name)) as? IrConst)?.value as? T ?: defaultValue
}