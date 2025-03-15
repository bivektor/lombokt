package com.bivektor.lombokt

import lombokt.EqualsAndHashCode
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class EqualsAndHashCodeLateInitTest {
  @EqualsAndHashCode(includeLateInits = true)
  private class LateInitsGlobalInclude {
    lateinit var included: String

    @EqualsAndHashCode.Exclude
    lateinit var excluded: String

    override fun toString(): String {
      return "$included/$excluded"
    }
  }

  @Test
  fun testLateInitsGlobalInclude() {
    val a = LateInitsGlobalInclude().apply {
      included = "a"
      excluded = "b"
    }

    val b = LateInitsGlobalInclude().apply {
      included = "a"
      excluded = "c"
    }

    val c = LateInitsGlobalInclude().apply {
      included = "b"
      excluded = "b"
    }

    assertEquals(a, b)
    assertEquals(a.hashCode(), b.hashCode())
    assertNotEquals(a, c)
    assertNotEquals(a.hashCode(), c.hashCode())
  }

  @EqualsAndHashCode
  private class LateInitsExplicitIncludeOnProp {
    @EqualsAndHashCode.Include
    lateinit var included: String

    @EqualsAndHashCode.Exclude
    lateinit var excluded: String

    override fun toString(): String {
      return "$included/$excluded"
    }
  }

  @Test
  fun testLateInitsExplicitIncludeOnProp() {
    val a = LateInitsExplicitIncludeOnProp().apply {
      included = "a"
      excluded = "b"
    }

    val b = LateInitsExplicitIncludeOnProp().apply {
      included = "a"
      excluded = "c"
    }

    val c = LateInitsExplicitIncludeOnProp().apply {
      included = "b"
      excluded = "b"
    }

    assertEquals(a, b)
    assertEquals(a.hashCode(), b.hashCode())
    assertNotEquals(a, c)
    assertNotEquals(a.hashCode(), c.hashCode())
  }

  @EqualsAndHashCode(onlyExplicitlyIncluded = true)
  private class LateInitsExcludedByDefault {

    @EqualsAndHashCode.Include
    lateinit var included: String
    lateinit var excluded: String

    override fun toString(): String {
      return "$included/$excluded"
    }
  }

  @Test
  fun testLateInitsExcludedByDefault() {
    val a = LateInitsExcludedByDefault().apply {
      included = "a"
      excluded = "b"
    }

    val b = LateInitsExcludedByDefault().apply {
      included = "a"
      excluded = "c"
    }

    val c = LateInitsExcludedByDefault().apply {
      included = "b"
      excluded = "b"
    }

    assertEquals(a, b)
    assertEquals(a.hashCode(), b.hashCode())
    assertNotEquals(a, c)
    assertNotEquals(a.hashCode(), c.hashCode())
  }
}
