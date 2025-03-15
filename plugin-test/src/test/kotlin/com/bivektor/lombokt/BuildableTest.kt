package com.bivektor.lombokt

import lombokt.Buildable
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val DEFAULT_AGE = 10
private val DEFAULT_DATA = mapOf("name" to "John")

class BuildableTest {

  @Buildable
  class Person(
    val name: String,
    val age: Int? = DEFAULT_AGE,
    var email: String? = null,
    val profile: Map<String, Any> = DEFAULT_DATA
  ) {

    @Buildable.Builder
    class Builder {
      fun name(name: String): Builder = this
      fun age(age: Int?): Builder = this
      fun email(email: String?): Builder = this
      fun profile(profile: Map<String, Any>): Builder = this
      fun build(): Person = Person("")
    }

    companion object {
      @JvmStatic
      fun builder() = Builder()
    }
  }

  @Test
  fun `build with all arguments`() {
    val profile = mapOf("name" to "Jane")
    val person = Person.builder().name("John").age(25).email("some").profile(profile).build()
    assertEquals("John", person.name)
    assertEquals(25, person.age)
    assertEquals("some", person.email)
    assertEquals(profile, person.profile)
  }

  @Test
  fun `build with required arguments`() {
    val person = Person.builder().name("John").build()
    assertEquals("John", person.name)
    assertEquals(10, person.age)
    assertNull(person.email)
    assertEquals(DEFAULT_DATA, person.profile)
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
