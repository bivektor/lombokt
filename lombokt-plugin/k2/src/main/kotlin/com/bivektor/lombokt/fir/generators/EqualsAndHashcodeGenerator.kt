package com.bivektor.lombokt.fir.generators

import com.bivektor.lombokt.LomboktNames.EQUALS_METHOD_NAME
import com.bivektor.lombokt.LomboktNames.HASHCODE_METHOD_NAME
import com.bivektor.lombokt.PluginKeys.EqualsHashCodeKey
import com.bivektor.lombokt.fir.NamedFunctionDescriptor
import com.bivektor.lombokt.fir.isFunctionDeclaredOrNotOverridable
import com.bivektor.lombokt.fir.isValueClass
import com.bivektor.lombokt.fir.services.EqualsAndHashCodeService
import com.bivektor.lombokt.fir.services.equalsAndHashCodeService
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
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

open class EqualsAndHashcodeGenerator(
  session: FirSession,
  private val messageCollector: MessageCollector
) : FirDeclarationGenerationExtension(session) {

  private val equalsAndHashCodeService: EqualsAndHashCodeService by lazy { session.equalsAndHashCodeService }

  private val equalsFunction: NamedFunctionDescriptor by lazy {
    NamedFunctionDescriptor(EQUALS_METHOD_NAME, listOf(session.builtinTypes.nullableAnyType.coneType))
  }

  private val hashCodeFunction = NamedFunctionDescriptor(HASHCODE_METHOD_NAME, emptyList())

  override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
    if (!isSuitableClass(classSymbol)) return emptySet()
    return setOf(equalsFunction.name, hashCodeFunction.name)
  }

  private fun isSuitableClass(classSymbol: FirClassSymbol<*>): Boolean {
    if (classSymbol.classKind != ClassKind.CLASS || classSymbol.isValueClass) return false
    if (!equalsAndHashCodeService.isAnnotated(classSymbol)) return false
    if (classSymbol.isFunctionDeclaredOrNotOverridable(
        session,
        equalsFunction
      ) || classSymbol.isFunctionDeclaredOrNotOverridable(session, hashCodeFunction)
    ) {
      messageCollector.report(
        CompilerMessageSeverity.LOGGING,
        eitherFunctionAlreadyDeclaredMessage(classSymbol)
      )
      return false
    }

    return true
  }

  override fun generateFunctions(
    callableId: CallableId,
    context: MemberGenerationContext?
  ): List<FirNamedFunctionSymbol> {
    val classSymbol = context?.owner ?: return emptyList()


    val callableName = callableId.callableName
    return when (callableName) {
      equalsFunction.name -> listOf(generateEqualsMethod(classSymbol).symbol)
      hashCodeFunction.name -> listOf(generateHashCodeMethod(classSymbol).symbol)
      else -> error("Unexpected callable name: $callableName")
    }
  }

  private fun eitherFunctionAlreadyDeclaredMessage(classSymbol: FirClassSymbol<*>) =
    "Skipping '${equalsFunction.name}' and '${hashCodeFunction.name}' generation on '${classSymbol.classId}'. " +
      "The class is annotated with '${equalsAndHashCodeService.annotationName}' but it already declares one of these methods or those methods are final in a super class"

  private fun generateEqualsMethod(
    classSymbol: FirClassSymbol<*>
  ): FirSimpleFunction {
    return createMemberFunction(
      classSymbol,
      EqualsHashCodeKey,
      equalsFunction.name,
      session.builtinTypes.booleanType.coneType
    ) {
      valueParameter(Name.identifier("other"), equalsFunction.valueParameterTypes.single())
      modality = Modality.OPEN
    }
  }

  private fun generateHashCodeMethod(
    classSymbol: FirClassSymbol<*>
  ): FirSimpleFunction = createMemberFunction(
    classSymbol,
    EqualsHashCodeKey,
    hashCodeFunction.name,
    session.builtinTypes.intType.coneType
  ) {
    modality = Modality.OPEN
  }

  companion object {
    internal fun factory(messageCollector: MessageCollector): Factory =
      Factory { session -> EqualsAndHashcodeGenerator(session, messageCollector) }
  }

}
