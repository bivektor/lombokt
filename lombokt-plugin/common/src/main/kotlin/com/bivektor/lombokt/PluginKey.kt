package com.bivektor.lombokt

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin

private class LomboktPluginKey(val feature: String) : GeneratedDeclarationKey() {
  override fun toString(): String = "Lombokt($feature)"
}

object PluginKeys {
  val EqualsHashCodeKey: GeneratedDeclarationKey = LomboktPluginKey("EqualsHashCode")
  val ToStringKey: GeneratedDeclarationKey = LomboktPluginKey("ToString")
}

fun IrDeclaration.isGeneratedByPluginKey(key: GeneratedDeclarationKey): Boolean = (origin as? IrDeclarationOrigin.GeneratedByPlugin)?.pluginKey == key