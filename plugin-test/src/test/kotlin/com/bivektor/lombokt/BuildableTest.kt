package com.bivektor.lombokt

import lombokt.Buildable
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val DEFAULT_AGE = 10
private val DEFAULT_DATA = mapOf("name" to "John")

class BuildableTest {

  @Suppress("unused")
  @Buildable
  private class Person private constructor(
    val name: String,
    prefix: String? = "Dr.",
    val age: Int? = DEFAULT_AGE,
    var email: String? = null,
    val profile: Map<String, Any> = DEFAULT_DATA
  ) {

    val fullName = "$prefix $name"

    @Buildable.Builder
    class Builder {
      fun name(name: String) = this
      fun age(age: Int?) = this
      fun email(email: String?) = this
      fun profile(profile: Map<String, Any>) = this
      fun prefix(prefix: String?) = this
      fun build() = Person("")
    }

    companion object {
      @JvmStatic
      fun builder() = Builder()
    }
  }

  @Test
  fun `build with all arguments`() {
    val profile = mapOf("name" to "Jane")
    val person = Person.builder().name("John").prefix("Mr.").age(25).email("some").profile(profile).build()
    assertEquals("John", person.name)
    assertEquals(25, person.age)
    assertEquals("some", person.email)
    assertEquals(profile, person.profile)
    assertEquals("Mr. John", person.fullName)
  }

  @Test
  fun `build with required arguments`() {
    val person = Person.builder().name("John").build()
    assertEquals("John", person.name)
    assertEquals(10, person.age)
    assertNull(person.email)
    assertEquals(DEFAULT_DATA, person.profile)
    assertEquals("Dr. John", person.fullName)
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

  @Suppress("unused")
  @Buildable
  @ConsistentCopyVisibility
  private data class PersonData private constructor(
    val name: String,
    val age: Int? = DEFAULT_AGE,
    var email: String? = null,
    val profile: Map<String, Any> = DEFAULT_DATA
  ) {

    @Buildable.Builder
    class Builder {
      fun name(name: String) = this
      fun age(age: Int?) = this
      fun email(email: String?) = this
      fun profile(profile: Map<String, Any>) = this
      fun build() = PersonData("")
    }

    companion object {
      @JvmStatic
      fun builder() = Builder()
    }
  }

  @Test
  fun dataClassTests() {
    val profile = mapOf("name" to "Jane")
    val person = PersonData.builder().name("John").age(25).email("some").profile(profile).build()
    assertEquals("John", person.name)
    assertEquals(25, person.age)
    assertEquals("some", person.email)
    assertEquals(profile, person.profile)
    assertEquals("PersonData(name=John, age=25, email=some, profile={name=Jane})", person.toString())
  }
}
