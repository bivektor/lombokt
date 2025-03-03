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