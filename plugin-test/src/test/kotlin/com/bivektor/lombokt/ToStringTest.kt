package com.bivektor.lombokt

import lombokt.ToString
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@Suppress("unused")
class ToStringTest {

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

    assertEquals(
      "Subject(super=${Person()}, extra=extra)",
      Subject().toString()
    )
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
      @ToString.Include(name="agex") private val age: Int = 10,
    ) {
      @ToString.Include(name="surnamex")
      var surname: String? = "Doe"

      @ToString.Include(name="emailx")
      private val email: String? = null

      @ToString.Include(name="computedx")
      val computed: String get() = "computed"
    }

    assertEquals("IncludeNames(namex=John, agex=10, surnamex=Doe, emailx=null, computedx=computed)", IncludeNames().toString())
  }

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

    val fullName: String get() = "$name $surname"
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
  }
}