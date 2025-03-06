package com.bivektor.lombokt

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class LomboktIrGenerationExtension(private val messageCollector: MessageCollector) : IrGenerationExtension {

  override fun generate(
    moduleFragment: IrModuleFragment,
    pluginContext: IrPluginContext
  ) {
    val visitors = listOf(
      EqualsAndHashCodeIrVisitor(pluginContext, messageCollector),
      ToStringIrVisitor(pluginContext, messageCollector)
    )

    moduleFragment.acceptChildrenVoid(object : IrVisitorVoid() {
      override fun visitElement(element: IrElement) {
        visitors.forEach { element.acceptVoid(it) }
      }
    })
  }
}
