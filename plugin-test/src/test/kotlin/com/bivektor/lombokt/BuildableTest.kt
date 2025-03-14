package com.bivektor.lombokt

import lombokt.Buildable
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BuildableTest {

  @Buildable
  class Person(val name: String, val age: Int? = 10, var email: String? = null) {

    @Buildable.Builder
    class Builder {
      fun name(name: String): Builder = this
      fun age(age: Int?): Builder = this
      fun email(email: String?): Builder = this
      fun build(): Person = Person("", 0)
    }

    companion object {
      @JvmStatic
      fun builder() = Builder()
    }
  }

  @Test
  fun `build with all arguments`() {
    val person = Person.builder().name("John").age(25).email("some").build()
    assertEquals("John", person.name)
    assertEquals(25, person.age)
    assertEquals("some", person.email)
  }

  @Test
  fun `build without optional arguments`() {
    val person = Person.builder().name("John").build()
    assertEquals("John", person.name)
    assertEquals(10, person.age)
    assertNull(person.email)
  }

  @Test
  fun `set nullable property with non-null default value`() {
    val person = Person.builder().name("John").age(null).build()
    assertNull(person.age)
  }

  @Test
  fun `without required argument`() {
    assertThrows<IllegalArgumentException> { Person.builder().build() }
  }
}
