package com.bivektor.lombokt

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
  class Person(val name: String = "x", var surname: String = "y") {

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
}
