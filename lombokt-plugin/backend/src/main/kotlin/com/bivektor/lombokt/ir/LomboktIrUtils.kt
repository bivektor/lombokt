package com.bivektor.lombokt.ir
import org.jetbrains.kotlin.DeprecatedForRemovalCompilerApi
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.types.isNullableAny
import org.jetbrains.kotlin.ir.util.getValueArgument
import org.jetbrains.kotlin.name.Name

@Suppress("UNCHECKED_CAST")
fun <T> IrConstructorCall.getConstValueByName(name: String, defaultValue: T): T {
  return (getValueArgument(Name.identifier(name)) as? IrConst)?.value as? T ?: defaultValue
}

val IrFunction.regularParameters: Iterable<IrValueParameter> get() = parameters.filter { it.kind == IrParameterKind.Regular }

@OptIn(DeprecatedForRemovalCompilerApi::class)
val IrFunction.isObjectEquals: Boolean get() {
  return name.asString() == "equals" &&
    valueParameters.count() == 1 &&
    valueParameters[0].type.isNullableAny() &&
    extensionReceiverParameter == null &&
    dispatchReceiverParameter != null
}

@OptIn(DeprecatedForRemovalCompilerApi::class)
val IrFunction.isObjectHashCode: Boolean get() {
  return name.asString() == "hashCode" &&
    valueParameters.isEmpty() &&
    extensionReceiverParameter == null &&
    dispatchReceiverParameter != null
}
