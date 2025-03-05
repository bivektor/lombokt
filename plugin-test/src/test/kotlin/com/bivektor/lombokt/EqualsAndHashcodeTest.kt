package com.bivektor.lombokt

import lombokt.EqualsAndHashCode
import lombokt.ToString
import org.junit.jupiter.api.Nested
import java.util.Objects
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@Suppress("unused")
class EqualsAndHashcodeTest {

  /**
   * Test basic functionality without special options
   */
  @Nested
  inner class BasicTests {

    @Test
    fun `test basic equals and hashCode with primary constructor properties`() {

      @EqualsAndHashCode
      class Person(val name: String, val age: Int)

      val person1 = Person("John", 30)
      val person2 = Person("John", 30)
      val person3 = Person("Jane", 30)

      assertEquals(person1, person2)
      assertNotEquals(person1, person3)
      assertEquals(person1.hashCode(), person2.hashCode())
      assertNotEquals(person1.hashCode(), person3.hashCode())

      // Verify hashCode algorithm
      val expectedHash = 31 * (31 * 17 + Objects.hashCode("John")) + Objects.hashCode(30)
      assertEquals(expectedHash, person1.hashCode())
    }

    @Test
    fun `test equals and hashCode with class body properties`() {
      @EqualsAndHashCode
      class Person {
        val name: String = "John"
        var age: Int = 30
      }

      val person1 = Person()
      val person2 = Person()
      person2.age = 30
      val person3 = Person()
      person3.age = 25

      assertEquals(person1, person2)
      assertNotEquals(person1, person3)
      assertEquals(person1.hashCode(), person2.hashCode())
      assertNotEquals(person1.hashCode(), person3.hashCode())
    }

    @Test
    fun `test with mixed primary constructor and class body properties`() {
      @EqualsAndHashCode
      class Person(val name: String) {
        var age: Int = 30
      }

      val person1 = Person("John")
      val person2 = Person("John")
      val person3 = Person("Jane")
      val person4 = Person("John")
      person4.age = 25

      assertEquals(person1, person2)
      assertNotEquals(person1, person3)
      assertNotEquals(person1, person4)
    }
  }

  open class BasePerson {
    val id: Int = 100

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is BasePerson) return false
      return id == other.id
    }

    override fun hashCode(): Int {
      return id.hashCode()
    }
  }

  /**
   * Test callSuper parameter
   */
  @Nested
  inner class CallSuperTests {

    @Test
    fun `test with callSuper=false (default)`() {
      @EqualsAndHashCode
      class Person(val name: String, val age: Int) : BasePerson()

      val person1 = Person("John", 30)
      val person2 = Person("John", 30)
      val person3 = Person("Jane", 30)

      assertEquals(person1, person2)
      assertNotEquals(person1, person3)

      // Verify the superclass fields are NOT included
      val expectedHash = 31 * (31 * 17 + Objects.hashCode("John")) + Objects.hashCode(30)
      assertEquals(expectedHash, person1.hashCode())
    }

    @Test
    fun `test with callSuper=true`() {

      @EqualsAndHashCode(callSuper = true)
      class Person(val name: String, val age: Int) : BasePerson()

      val person1 = Person("John", 30)
      val person2 = Person("John", 30)
      val person3 = Person("Jane", 30)

      assertEquals(person1, person2)
      assertNotEquals(person1, person3)

      // Verify the superclass fields are included via super.hashCode()
      val superHash = person1.id.hashCode()
      val expectedHash = 31 * (31 * superHash + Objects.hashCode("John")) + Objects.hashCode(30)
      assertEquals(expectedHash, person1.hashCode())
    }
  }

  /**
   * Test Include and Exclude annotations
   */
  @Nested
  inner class IncludeExcludeTests {

    @Test
    fun `test Exclude annotation on property`() {
      @EqualsAndHashCode
      class Person(
        val name: String,
        @EqualsAndHashCode.Exclude val age: Int,
        val email: String
      )

      val person1 = Person("John", 30, "john@example.com")
      val person2 = Person("John", 25, "john@example.com")
      val person3 = Person("John", 30, "john.doe@example.com")

      // age is excluded, so different ages should be equal
      assertEquals(person1, person2)
      // different emails should not be equal
      assertNotEquals(person1, person3)

      // Verify hashCode algorithm (age is excluded)
      val expectedHash = 31 * (31 * 17 + Objects.hashCode("John")) + Objects.hashCode("john@example.com")
      assertEquals(expectedHash, person1.hashCode())
    }

    @Test
    fun `test with onlyExplicitlyIncluded=true`() {
      @EqualsAndHashCode(onlyExplicitlyIncluded = true)
      class Person(
        @EqualsAndHashCode.Include val name: String,
        val age: Int,
        @EqualsAndHashCode.Include val email: String
      )

      val person1 = Person("John", 30, "john@example.com")
      val person2 = Person("John", 25, "john@example.com")
      val person3 = Person("John", 30, "john.doe@example.com")

      // age is not included, so different ages should be equal
      assertEquals(person1, person2)
      // different emails should not be equal
      assertNotEquals(person1, person3)

      // Verify hashCode algorithm (only name and email are included)
      val expectedHash = 31 * (31 * 17 + Objects.hashCode("John")) + Objects.hashCode("john@example.com")
      assertEquals(expectedHash, person1.hashCode())
    }

    @Test
    fun `test Include and Exclude on same field (Exclude takes precedence)`() {
      @EqualsAndHashCode(onlyExplicitlyIncluded = true)
      class Person(
        @EqualsAndHashCode.Include val name: String,
        @EqualsAndHashCode.Include @EqualsAndHashCode.Exclude val age: Int
      )

      val person1 = Person("John", 30)
      val person2 = Person("John", 25)
      val person3 = Person("Jane", 30)

      // age should be excluded even though it has Include annotation
      assertEquals(person1, person2)
      assertNotEquals(person1, person3)

      // Verify hashCode algorithm (only name is included, age is excluded)
      val expectedHash = 31 * 17 + Objects.hashCode("John")
      assertEquals(expectedHash, person1.hashCode())
    }
  }

  /**
   * Test get-only and set-only properties
   */
  @Nested
  inner class GetterSetterTests {

    @Test
    fun `test get-only property`() {
      @EqualsAndHashCode
      class Person(private val _name: String) {
        val name: String get() = _name
      }

      val person1 = Person("John")
      val person2 = Person("John")
      val person3 = Person("Jane")

      assertEquals(person1, person2)
      assertNotEquals(person1, person3)
    }

    @Test
    fun `test set-only property`() {
      @EqualsAndHashCode
      class Person {
        private var _name: String = ""
        var name: String
          get() = TODO()
          set(value) { _name = value }
      }

      val person1 = Person()
      person1.name = "John"
      val person2 = Person()
      person2.name = "John"
      val person3 = Person()
      person3.name = "Jane"

      assertEquals(person1, person2)
      assertNotEquals(person1, person3)
    }

    @Test
    fun `test Include on property`() {
      @EqualsAndHashCode(onlyExplicitlyIncluded = true)
      class Person(val name: String, @EqualsAndHashCode.Include val age: Int)

      val person1 = Person("John", 30)
      val person2 = Person("Jane", 30)
      val person3 = Person("John", 25)

      // name is not included (no Include annotation)
      // age is included, so comparing age
      assertEquals(person1, person2)
      assertNotEquals(person1, person3)
    }

    @Test
    fun `test Exclude on property`() {
      @EqualsAndHashCode
      class Person(val name: String, @EqualsAndHashCode.Exclude val age: Int)

      val person1 = Person("John", 30)
      val person2 = Person("John", 25)
      val person3 = Person("Jane", 30)

      assertEquals(person1, person2)
      assertNotEquals(person1, person3)
    }
  }

  enum class Role { USER, ADMIN }

  /**
   * Test nullability and different types
   */
  @Nested
  inner class NullabilityAndTypesTests {

    @Test
    fun `test nullable primitive types`() {
      @EqualsAndHashCode
      class TestClass(val intValue: Int?, val doubleValue: Double?)

      val obj1 = TestClass(10, 20.5)
      val obj2 = TestClass(10, 20.5)
      val obj3 = TestClass(null, 20.5)
      val obj4 = TestClass(10, null)

      assertEquals(obj1, obj2)
      assertNotEquals(obj1, obj3)
      assertNotEquals(obj1, obj4)
      assertNotEquals(obj3, obj4)
    }

    @Test
    fun `test enum types`() {
      @EqualsAndHashCode
      class User(val name: String, val role: Role)

      val user1 = User("John", Role.ADMIN)
      val user2 = User("John", Role.ADMIN)
      val user3 = User("John", Role.USER)

      assertEquals(user1, user2)
      assertNotEquals(user1, user3)
    }

    @Test
    fun `test nullable enum types`() {
      @EqualsAndHashCode
      class User(val name: String, val role: Role?)

      val user1 = User("John", Role.ADMIN)
      val user2 = User("John", Role.ADMIN)
      val user3 = User("John", null)

      assertEquals(user1, user2)
      assertNotEquals(user1, user3)
    }

    @Test
    fun `test nested objects`() {
      @EqualsAndHashCode
      class Address(val street: String, val city: String)

      @EqualsAndHashCode
      class Person(val name: String, val address: Address)

      val address1 = Address("123 Main St", "New York")
      val address2 = Address("123 Main St", "New York")
      val address3 = Address("456 Oak St", "Boston")

      val person1 = Person("John", address1)
      val person2 = Person("John", address2)
      val person3 = Person("John", address3)

      assertEquals(person1, person2)
      assertNotEquals(person1, person3)
    }

    @Test
    fun `test nullable nested objects`() {
      @EqualsAndHashCode
      class Address(val street: String, val city: String)

      @EqualsAndHashCode
      class Person(val name: String, val address: Address?)

      val address1 = Address("123 Main St", "New York")
      val address2 = Address("456 Oak St", "Boston")

      val person1 = Person("John", address1)
      val person2 = Person("John", address1)
      val person3 = Person("John", address2)
      val person4 = Person("John", null)

      assertEquals(person1, person2)
      assertNotEquals(person1, person3)
      assertNotEquals(person1, person4)
    }

    @Test
    fun `test collections`() {
      @EqualsAndHashCode
      class Person(val name: String, val hobbies: List<String>)

      val person1 = Person("John", listOf("Reading", "Hiking"))
      val person2 = Person("John", listOf("Reading", "Hiking"))
      val person3 = Person("John", listOf("Swimming", "Cycling"))
      val person4 = Person("John", emptyList())

      assertEquals(person1, person2)
      assertNotEquals(person1, person3)
      assertNotEquals(person1, person4)
    }
  }

  /**
   * Test complex scenarios combining multiple features
   */
  @Nested
  inner class ComplexScenariosTests {

    @Test
    fun `test complex class with multiple annotations`() {
      open class BaseEntity {
        val id: Long = 1L

        override fun equals(other: Any?): Boolean {
          if (this === other) return true
          if (other !is BaseEntity) return false
          return id == other.id
        }

        override fun hashCode(): Int {
          return id.hashCode()
        }
      }

      @EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
      class User(
        @EqualsAndHashCode.Include val username: String,
        @EqualsAndHashCode.Exclude val password: String,
        val email: String,
        @EqualsAndHashCode.Include val active: Boolean,
        val lastLogin: String?
      ) : BaseEntity() {

        @EqualsAndHashCode.Include
        var temporaryToken: String? = null
      }

      val user1 = User("john_doe", "secret1", "John@example.com", true, "2023-01-01")
      val user2 = User("john_doe", "secret2", "john@EXAMPLE.com", true, "2022-12-31")
      val user3 = User("jane_doe", "secret1", "Jane@example.com", true, "2023-01-01")
      val user4 = User("john_doe", "secret1", "John@example.com", false, "2023-01-01")

      user1.temporaryToken = "token123"
      user2.temporaryToken = "token123"
      user3.temporaryToken = "token123"

      // user1 and user2 should be equal:
      // - same username
      // - passwords are excluded
      // - email is not included
      // - same active status
      // - lastLogin is not included
      // - temporaryToken is included and they are equal
      // - id from BaseEntity is included due to callSuper=true and they are equal
      assertEquals(user1, user2)
      assertEquals(user1.hashCode(), user2.hashCode())

      // Different username
      assertNotEquals(user1, user3)

      // Different active status
      assertNotEquals(user1, user4)

      // Verify hashCode
      val baseHash = user1.id.hashCode()
      var expectedHash = baseHash
      expectedHash = 31 * expectedHash + Objects.hashCode(user1.username)
      expectedHash = 31 * expectedHash + Objects.hashCode(user1.active)
      expectedHash = 31 * expectedHash + Objects.hashCode(user1.temporaryToken)

      assertEquals(expectedHash, user1.hashCode())
    }
  }

  @EqualsAndHashCode
  interface PersonDao

  @Nested
  inner class DataClassTests {
    @Test
    fun `default settings`() {

      @EqualsAndHashCode
      data class Person(val name: String, @EqualsAndHashCode.Exclude val age: Int) {
        var address: String = "some st."
      }

      val person1 = Person("John", 30)
      val person2 = Person("John", 40)
      val person3 = Person("Jane", 30)
      val person4 = Person("John", 30).apply { address = "other st." }

      assertEquals(person1, person2)
      assertEquals(person1.hashCode(), person2.hashCode())
      assertNotEquals(person1, person3)
      assertNotEquals(person1.hashCode(), person3.hashCode())
      assertNotEquals(person1, person4)
      assertNotEquals(person1.hashCode(), person4.hashCode())
    }

    @Test
    fun `only explicitly included`() {

      @EqualsAndHashCode(onlyExplicitlyIncluded = true)
      data class Person(@EqualsAndHashCode.Include val name: String, val age: Int)

      val person1 = Person("John", 30)
      val person2 = Person("John", 40)
      val person3 = Person("Jane", 30)

      assertEquals(person1, person2)
      assertEquals(person1.hashCode(), person2.hashCode())
      assertNotEquals(person1, person3)
      assertNotEquals(person1.hashCode(), person3.hashCode())
    }

    @Test
    fun `call super`() {

      @EqualsAndHashCode
      open class Base {
        var id: String = "1"
      }

      @EqualsAndHashCode(callSuper = true)
      data class Person(val name: String, val age: Double) : Base()

      val person1 = Person("John", 30.1)
      val person2 = Person("John", 30.1)
      val person3 = Person("John", 30.1).apply { id = "2" }

      assertEquals(person1, person2)
      assertEquals(person1.hashCode(), person2.hashCode())
      assertNotEquals(person1, person3)
      assertNotEquals(person1.hashCode(), person3.hashCode())
    }
  }

  @Test
  fun `should skip if equals and hashcode already declared`() {
    @EqualsAndHashCode
    class Person(val name: String) {

      override fun equals(other: Any?): Boolean {
        return (other as Person?)?.name == "John"
      }

      override fun hashCode(): Int {
        return 99
      }
    }

    assertEquals(Person("Jane"), Person("John"))
    assertEquals(99, Person("Jane").hashCode())
  }
}
