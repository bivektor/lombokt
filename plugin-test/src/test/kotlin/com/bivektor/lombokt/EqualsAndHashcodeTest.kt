package com.bivektor.lombokt

import lombokt.EqualsAndHashCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@Suppress("unused")
class EqualsAndHashcodeTest {

  // Base class example when callSuper = true
  private open class BaseEntity(val baseId: Int) {
    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is BaseEntity) return false
      return baseId == other.baseId
    }

    override fun hashCode(): Int = calculateHashCode(baseId)
  }

  // Case: callSuper = true, includes super class properties
  @EqualsAndHashCode(callSuper = true)
  private class WithSuperClass(baseId: Int, val name: String) : BaseEntity(baseId)

  @Test
  fun `test callSuper equals and hashCode`() {
    val obj1 = WithSuperClass(1, "Alice")
    val obj2 = WithSuperClass(1, "Alice")
    val obj3 = WithSuperClass(2, "Bob")

    assertEquals(obj1, obj2)
    assertNotEquals(obj1, obj3)
    assertEquals(obj1.hashCode(), calculateHashCode(1, "Alice"))
  }

  // Case: doNotUseGetters = true
  @EqualsAndHashCode(doNotUseGetters = true)
  private class NoGetterAccess(val id: Int) {
    val name: String = "default"
  }

  @Test
  fun `test doNotUseGetters equals and hashCode`() {
    val obj1 = NoGetterAccess(1)
    val obj2 = NoGetterAccess(1)
    val obj3 = NoGetterAccess(2)

    assertEquals(obj1, obj2)
    assertNotEquals(obj1, obj3)
    assertEquals(obj1.hashCode(), calculateHashCode(1, "default"))
  }

  // Case: onlyExplicitlyIncluded = true
  @EqualsAndHashCode(onlyExplicitlyIncluded = true)
  private class ExplicitInclusion(val id: Int, val ignored: String) {
    @EqualsAndHashCode.Include
    val name: String = "included"
  }

  @Test
  fun `test onlyExplicitlyIncluded equals and hashCode`() {
    val obj1 = ExplicitInclusion(1, "ignored")
    val obj2 = ExplicitInclusion(2, "ignored")
    assertEquals(obj1, obj2)
    assertEquals(obj1.hashCode(), calculateHashCode("included"))
  }

  // Case: Excluding properties
  @EqualsAndHashCode
  private class ExcludeExample(val id: Int, @EqualsAndHashCode.Exclude val name: String = "excluded") {
    @EqualsAndHashCode.Exclude
    val ignoredField: String = "should not be included"
  }

  @Test
  fun `test excluded property`() {
    val obj1 = ExcludeExample(1)
    val obj2 = ExcludeExample(1)
    val obj3 = ExcludeExample(2)

    assertEquals(obj1, obj2)
    assertNotEquals(obj1, obj3)
    assertEquals(obj1.hashCode(), calculateHashCode(1))
  }

  // Case: Equals or hashcode already declared or final in super class
  @EqualsAndHashCode(callSuper = true)
  private open class MethodsAlreadyDeclared(val id: Int) {
    final override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is MethodsAlreadyDeclared) return false
      return id == other.id
    }

    final override fun hashCode(): Int = id
  }

  @EqualsAndHashCode(callSuper = true)
  private class MethodsAlreadyDeclaredSubclass(id: Int, val name: String = "name") : MethodsAlreadyDeclared(id)

  @Test
  fun `methods not generated when already declared`() {
    val obj1 = MethodsAlreadyDeclared(1)
    val obj2 = MethodsAlreadyDeclared(1)
    val obj3 = MethodsAlreadyDeclared(2)

    assertEquals(obj1, obj2)
    assertNotEquals(obj1, obj3)
    assertEquals(obj1.hashCode(), 1)
  }

  @Test
  fun `methods not generated when they are final in super class`() {
    val obj1 = MethodsAlreadyDeclaredSubclass(1, "a")
    val obj2 = MethodsAlreadyDeclaredSubclass(1, "b")

    assertEquals(obj1, obj2)
    assertEquals(obj1.hashCode(), 1)
  }

  @EqualsAndHashCode
  private class VariousTypes(
    val dbl: Double = 0.5,
    val flt: Float = 0.5f,
    val lng: Long = 100L,
    var int: Int = 10,
    val shrt: Short = 2,
    val byt: Byte = 1,
    val bool: Boolean = true,
    val ch: Char = 'c',
    val str: String = "some",
    val lst: List<String> = listOf("a", "b"),
    val map: Map<String, String> = mapOf("a" to "b"),
    val dblNull: Double? = null,
    val fltNull: Float? = null,
    val lngNull: Long? = null,
    val intNull: Int? = null,
    val shrtNull: Short? = null,
    val bytNull: Byte? = null,
    val boolNull: Boolean? = null,
    val chNull: Char? = null,
    val strNull: String? = null,
    val lstNull: List<String>? = null,
  )

  @Test
  fun `test various variable types`() {
    val v1 = VariousTypes()
    val v2 = VariousTypes()
    assertEquals(v1, v2)
    assertEquals(v1.hashCode(), v2.hashCode())

    val v3 = VariousTypes(dblNull = 0.1)
    assertNotEquals(v1, v3)
    assertNotEquals(v1.hashCode(), v3.hashCode())
  }

  private class Menu {
    class SubMenu {
      @EqualsAndHashCode
      class SubSubMenu(val name: String)
    }
  }

  @Test
  fun testNestedClass() {
    val v1 = Menu.SubMenu.SubSubMenu("a")
    val v2 = Menu.SubMenu.SubSubMenu("a")
    val v3 = Menu.SubMenu.SubSubMenu("b")
    assertEquals(v1, v2)
    assertNotEquals(v1, v3)
    assertEquals(v1.hashCode(), v2.hashCode())
    assertNotEquals(v1.hashCode(), v3.hashCode())
  }

  @EqualsAndHashCode
  private data class DataClass(val name: String, @EqualsAndHashCode.Exclude var age: Int) {
    var excludedByDefault: Int = 1
  }

  @EqualsAndHashCode(onlyExplicitlyIncluded = true)
  private data class DataClassExplicit(@EqualsAndHashCode.Include val name: String, var age: Int) {
    var excludedByDefault: Int = 1
  }

  @Test
  fun testDataClass() {
    val v1 = DataClass("a", 1)
    val v2 = DataClass("a", 2)
    val v3 = DataClass("b", 1)
    val v4 = DataClass("a", 1).apply { excludedByDefault = 10 }

    val explicit1 = DataClassExplicit("a", 1)
    val explicit2 = DataClassExplicit("a", 2)
    val explicit3 = DataClassExplicit("b", 1)

    assertEquals(v1, v2)
    assertEquals(v1.hashCode(), v2.hashCode())
    assertNotEquals(v1, v3)
    assertEquals(v1, v4)
    assertEquals(v1.hashCode(), v4.hashCode())
    assertEquals(explicit1, explicit2)
    assertEquals(explicit1.hashCode(), explicit2.hashCode())
    assertNotEquals(explicit1, explicit3)
    assertNotEquals(explicit1.hashCode(), explicit3.hashCode())
  }
}

private fun calculateHashCode(vararg values: Any?): Int {
  return values.fold(17) { hash, value -> 31 * hash + (value?.hashCode() ?: 0) }
}

