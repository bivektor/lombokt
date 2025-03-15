package com.bivektor.lombokt.ir

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class LomboktIrGenerationExtension(private val messageCollector: MessageCollector) : IrGenerationExtension {

  override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
    moduleFragment.acceptVoid(LomboktIrTransformer(pluginContext, messageCollector))
  }

  private class LomboktIrTransformer(
    private val pluginContext: IrPluginContext,
    private val messageCollector: MessageCollector
  ) : IrVisitorVoid() {

    private val toStringGenerator = ToStringIrBodyGenerator(pluginContext, messageCollector)

    override fun visitElement(element: IrElement) {
      when (element) {
        is IrDeclaration,
        is IrFile,
        is IrModuleFragment -> element.acceptChildrenVoid(this)
      }
    }

    override fun visitClass(declaration: IrClass) {
      super.visitClass(declaration)
      EqualsAndHashCodeIrBodyGenerator(declaration, pluginContext, messageCollector).processClass()
      BuildableIrBodyGenerator.processClass(declaration, pluginContext)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
      super.visitSimpleFunction(declaration)
      toStringGenerator.processSimpleFunction(declaration)
    }
  }
}
