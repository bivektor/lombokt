package example.lombokt

import lombokt.EqualsAndHashCode
import lombokt.ToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class CombinedAnnotationsTest {
  @Suppress("unused")
  @ToString
  @EqualsAndHashCode
  class Person(val name: String = "x", var surname: String = "y")

  @Test
  fun testAllAnnotations() {
    val p1 = Person()
    val p2 = Person()
    assertEquals("Person(name=x, surname=y)", p1.toString())
    assertEquals(p1, p2)
    assertEquals(p1.hashCode(), p2.hashCode())
    assertNotEquals(Person("a"), Person("b"))
    assertNotEquals(Person(surname = "a"), Person(surname = "b"))
  }
}
