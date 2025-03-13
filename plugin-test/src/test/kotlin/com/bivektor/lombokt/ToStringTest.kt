package com.bivektor.lombokt

import lombokt.ToString
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@Suppress("unused")
@ToString
class ToStringTest {

  @ToString
  private open class Person(
    open var name: String = "John",
    private val age: Int = 10,
    @ToString.Exclude val weight: Double = 3.5
  ) {
    var surname: String? = "Doe"
    private val address: String = "some st."
    private val address2: String? = null

    @ToString.Exclude
    private val address3: String = "excluded"

    @ToString.Include
    val fullName: String get() = "$name $surname"

    val computed: String get() = "computed"
  }

  @ToString
  object TestObject {
    private val id: String = "id"
    private var createdBy: String = "user"

    @ToString.Exclude
    val name: String = "John"
    var surname: String? = "Doe"
    val email: String? = null

    @ToString.Include
    val computed: String get() = "computed"
  }

  @ToString
  object TestObjectWithDeclaredMethod {
    private val id: String = "id"

    override fun toString(): String {
      return "declared"
    }
  }

  private val p = Person()

  @Test
  fun testSelf() {
    assertEquals("ToStringTest(p=$p)", toString())
  }

  @Test
  fun defaultSettings() {
    assertEquals(
      "Person(name=John, age=10, surname=Doe, address=some st., address2=null, fullName=John Doe)",
      Person().toString()
    )
  }

  @ToString(callSuper = true)
  private class CallSuperTrue(val extra: String = "extra") : Person()

  @ToString(callSuper = false)
  private class CallSuperFalse(val extra: String = "extra") : Person()

  @ToString(callSuper = true)
  private class CallSuperTrueNoSuperClass(val extra: String = "extra")

  @Test
  fun callSuper() {
    assertEquals("CallSuperTrue(super=${Person()}, extra=extra)", CallSuperTrue().toString())
    assertEquals("CallSuperFalse(extra=extra)", CallSuperFalse().toString())

    val callSuperNoSuper = CallSuperTrueNoSuperClass().toString()
    assertNotEquals("CallSuperTrueNoSuperClass(extra=extra)", callSuperNoSuper)
    assertContains(callSuperNoSuper, ", extra=extra")
    assertContains(callSuperNoSuper, "CallSuperTrueNoSuperClass(super=com.bivektor.lombokt.ToStringTest\$CallSuperTrueNoSuperClass")
  }

  @ToString(onlyExplicitlyIncluded = true)
  private class PersonExplicitInclude(
    @ToString.Include var name: String = "John",
    private val age: Int = 10,
    @ToString.Include private val weight: Double = 3.5
  ) {
    var surname: String? = "Doe"
    private val address: String = "some st."
    private val address2: String? = null

    @ToString.Include
    val fullName: String get() = "$name $surname"

    val computed: String get() = "computed"
  }

  @Test
  fun explicitInclude() {
    assertEquals(
      "PersonExplicitInclude(name=John, weight=3.5, fullName=John Doe)",
      PersonExplicitInclude().toString()
    )
  }

  @ToString(doNotUseGetters = true)
  private class DoNotUseGetters(val id: String = "id", private val createdBy: String = "user") {
    private val createdDate: Long = 100

    val name: String = "John"
      get() = field.uppercase()

    @ToString.Include
    val age: Int get() = 10
  }

  @Test
  fun doNotUseGetters() {
    assertEquals("DoNotUseGetters(id=id, createdBy=user, createdDate=100, name=John, age=10)", DoNotUseGetters().toString())
  }

  @ToString(doNotUseGetters = true, onlyExplicitlyIncluded = true)
  private class DoNotUseGettersExplicit(val id: String = "id") {

    private val createdDate: Long = 100

    @ToString.Include
    val name: String = "John"
      get() = field.uppercase()

    @ToString.Exclude
    val age: Int get() = 10
  }

  @Test
  fun doNotUseGettersExplicitInclude() {
    assertEquals("DoNotUseGettersExplicit(name=John)", DoNotUseGettersExplicit().toString())
  }

  @ToString
  private class IncludeNames(
    @ToString.Include(name = "namex") var name: String = "John",
    @ToString.Include(name = "agex") private val age: Int = 10,
  ) {
    @ToString.Include(name = "surnamex")
    var surname: String? = "Doe"

    @ToString.Include(name = "emailx")
    private val email: String? = null

    @ToString.Include(name = "computedx")
    val computed: String get() = "computed"
  }

  @Test
  fun customIncludeName() {
    assertEquals(
      "IncludeNames(namex=John, agex=10, surnamex=Doe, emailx=null, computedx=computed)",
      IncludeNames().toString()
    )
  }

  @ToString
  private class ToStringDeclared(var name: String = "John") {
    override fun toString(): String {
      return "Jane"
    }
  }

  @Test
  fun shouldSkipIfToStringAlreadyDeclared() {
    assertEquals("Jane", ToStringDeclared().toString())
  }

  @Test
  fun objectTests() {
    @Suppress("RemoveRedundantQualifierName")
    assertEquals("TestObject(id=id, createdBy=user, surname=Doe, email=null, computed=computed)", TestObject.toString())
    assertEquals("declared", TestObjectWithDeclaredMethod.toString())
  }

  private open class ToStringFinal {
    final override fun toString(): String {
      return "fromSuper"
    }
  }

  @ToString
  private class SuperMethodFinal(val extra: String = "extra") : ToStringFinal()

  @Test
  fun shouldSkipWhenSuperMethodFinal() {
    assertEquals("fromSuper", SuperMethodFinal().toString())
  }
}
