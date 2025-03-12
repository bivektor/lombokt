package com.bivektor.lombokt.fir.generators

import com.bivektor.lombokt.LomboktNames.TO_STRING_METHOD_NAME
import com.bivektor.lombokt.PluginKeys
import com.bivektor.lombokt.fir.NamedFunctionDescriptor
import com.bivektor.lombokt.fir.getDeclaredFunction
import com.bivektor.lombokt.fir.isValueClass
import com.bivektor.lombokt.fir.services.ToStringService
import com.bivektor.lombokt.fir.services.toStringService
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.LOGGING
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

private val toStringFunction = NamedFunctionDescriptor(TO_STRING_METHOD_NAME, emptyList())

open class ToStringGenerator(
  session: FirSession,
  private val messageCollector: MessageCollector
) : FirDeclarationGenerationExtension(session) {

  private val toStringService: ToStringService by lazy {
    session.toStringService
  }

  private val methodName = toStringFunction.name

  override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
    if (isSuitableClass(classSymbol)) return setOf(methodName)
    return emptySet()
  }

  private fun isSuitableClass(classSymbol: FirClassSymbol<*>): Boolean {
    if (!(classSymbol.classKind == ClassKind.CLASS || classSymbol.classKind == ClassKind.OBJECT)) return false
    if (classSymbol.isValueClass) return false
    return toStringService.isAnnotated(classSymbol)
  }

  override fun generateFunctions(
    callableId: CallableId,
    context: MemberGenerationContext?
  ): List<FirNamedFunctionSymbol> {
    val callableName = callableId.callableName
    require(callableName == methodName) { "Unexpected callable name: $callableName" }
    val classSymbol = context?.owner ?: return emptyList()
    if (classSymbol.getDeclaredFunction(toStringFunction) != null) {
      messageCollector.report(
        LOGGING,
        "Skipping '$methodName' generation on '${classSymbol.classId}'. The class is annotated with '${toStringService.annotationName}' but it already declares a method with the same name"
      )
      return emptyList()
    }

    return listOf(generateToStringMethod(classSymbol).symbol)
  }

  private fun generateToStringMethod(
    classSymbol: FirClassSymbol<*>
  ): FirSimpleFunction = createMemberFunction(
    classSymbol,
    PluginKeys.ToStringKey,
    methodName,
    session.builtinTypes.stringType.coneType,
  ) {
    modality = Modality.OPEN
  }

  internal companion object {
    internal fun factory(messageCollector: MessageCollector): Factory =
      Factory { session -> ToStringGenerator(session, messageCollector) }
  }

}
