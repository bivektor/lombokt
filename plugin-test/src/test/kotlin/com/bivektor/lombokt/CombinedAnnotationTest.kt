package com.bivektor.lombokt

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import lombokt.Buildable
import lombokt.EqualsAndHashCode
import lombokt.ToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class CombinedAnnotationTest {

  @Suppress("unused")
  @Buildable
  @ToString
  @EqualsAndHashCode
  @Serializable
  class Person(val name: String = "x", var surname: String = "y") {

    fun some() = 10

    @Buildable.Builder
    class Builder {
      fun name(name: String) = this
      fun surname(surname: String) = this
      fun build() = Person()
    }
  }

  @Test
  fun testAllAnnotations() {
    val p1 = Person.Builder().build()
    val p2 = Person.Builder().build()
    assertEquals(p1, p2)
    assertEquals(p1.hashCode(), p2.hashCode())
    assertEquals("Person(name=x, surname=y)", p1.toString())
    assertNotEquals(p1, Person.Builder().name("a").build())
  }

  @Test
  fun testSerializationNotBroken() {
    val p = Person.Builder().name("John").surname("Doe").build()
    assertEquals("John", p.name)
    assertEquals("Doe", p.surname)
    val serialized = Json.encodeToString(Person.serializer(), p)
    val deserialized = Json.decodeFromString(Person.serializer(), serialized)
    assertEquals(p, deserialized)
  }

  @Test
  fun allOpenNotBroken() {
    class PersonDerived : Person() {
      override fun some(): Int = 11
    }

    val p = PersonDerived()
    assertEquals(11, p.some())
  }
}
