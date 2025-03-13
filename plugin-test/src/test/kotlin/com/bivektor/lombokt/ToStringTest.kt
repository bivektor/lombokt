package com.bivektor.lombokt

import lombokt.ToString
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@Suppress("unused")
@ToString
class ToStringTest {

  val p = Person()

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

  @Test
  fun callSuper() {
    @ToString(callSuper = true)
    class Subject(val extra: String = "extra") : Person()

    @ToString(callSuper = false)
    class Subject2(val extra: String = "extra") : Person()

    @ToString(callSuper = true)
    class Subject4(val extra: String = "extra")


    assertEquals("Subject(super=${Person()}, extra=extra)", Subject().toString())
    assertEquals("Subject2(extra=extra)", Subject2().toString())

    val subject4String = Subject4().toString()
    assertNotEquals("Subject4(extra=extra)", subject4String)
    assertContains(subject4String, "super=com.bivektor.lombokt.ToStringTest\$callSuper")
  }

  @Test
  fun explicitInclude() {
    assertEquals(
      "PersonExplicitInclude(name=John, weight=3.5, fullName=John Doe)",
      PersonExplicitInclude().toString()
    )
  }

  @Test
  fun doNotUseGetters() {
    @ToString(doNotUseGetters = true)
    class TestClass(val id: String = "id", private val createdBy: String = "user") {

      private val createdDate: Long = 100

      val name: String = "John"
        get() = field.uppercase()

      @ToString.Include
      val age: Int get() = 10
    }

    assertEquals("TestClass(id=id, createdBy=user, createdDate=100, name=John, age=10)", TestClass().toString())
  }

  @Test
  fun doNotUseGettersExplicitInclude() {
    @ToString(doNotUseGetters = true, onlyExplicitlyIncluded = true)
    class TestClass(val id: String = "id") {

      private val createdDate: Long = 100

      @ToString.Include
      val name: String = "John"
        get() = field.uppercase()

      @ToString.Exclude
      val age: Int get() = 10
    }

    assertEquals("TestClass(name=John)", TestClass().toString())
  }

  @Test
  fun customIncludeName() {
    @ToString
    class IncludeNames(
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

    assertEquals(
      "IncludeNames(namex=John, agex=10, surnamex=Doe, emailx=null, computedx=computed)",
      IncludeNames().toString()
    )
  }

  @Test
  fun shouldSkipIfToStringAlreadyDeclared() {
    @ToString
    class John(var name: String = "John") {
      override fun toString(): String {
        return "Jane"
      }
    }

    assertEquals("Jane", John().toString())
  }

  @Test
  fun objectTests() {
    @Suppress("RemoveRedundantQualifierName")
    assertEquals("TestObject(id=id, createdBy=user, surname=Doe, email=null, computed=computed)", TestObject.toString())
    assertEquals("declared", TestObjectWithDeclaredMethod.toString())
  }

  @Test
  fun shouldSkipWhenSuperMethodFinal() {
    open class ToStringFinal {
      final override fun toString(): String {
        return "fromSuper"
      }
    }

    @ToString
    open class Subject(val extra: String = "extra") : ToStringFinal()

    assertEquals("fromSuper", Subject().toString())
  }

  @ToString
  open class Person(
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
}
