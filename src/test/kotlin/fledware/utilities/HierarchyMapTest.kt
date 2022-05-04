package fledware.utilities

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HierarchyMapTest {
  companion object {
    @JvmStatic
    fun getData() = listOf(
        Arguments.of({ ConcurrentHierarchyMap() } as () -> MutableHierarchyMap<Any>),
        Arguments.of({ RootConcurrentHierarchyMap<Any>() } as () -> MutableHierarchyMap<Any>),
    )
  }

  @ParameterizedTest
  @MethodSource("getData")
  fun canReturnNull(factory: () -> MutableHierarchyMap<Any>) {
    val target = factory()
    assertNull(target.getMaybe(String::class))
  }

  @ParameterizedTest
  @MethodSource("getData")
  fun canFindSetValue(factory: () -> MutableHierarchyMap<Any>) {
    val target = factory()
    target.add(String::class)
    assertNotNull(target.getMaybe<String>())
  }

  @ParameterizedTest
  @MethodSource("getData")
  fun canFindSubclassValue(factory: () -> MutableHierarchyMap<Any>) {
    val target = factory()
    target.add(String::class)
    assertEquals(String::class, target.getMaybe<CharSequence>())
  }

  @ParameterizedTest
  @MethodSource("getData")
  fun canRemoveWithoutErrorOnNoValue(factory: () -> MutableHierarchyMap<Any>) {
    val target = factory()
    target.remove(String::class)
  }

  @ParameterizedTest
  @MethodSource("getData")
  fun canRemoveSetValue(factory: () -> MutableHierarchyMap<Any>) {
    val target = factory()
    target.add(String::class)
    assertEquals(String::class, target.getMaybe(String::class))
    assertEquals(1, target.size)
    target.remove(String::class)
    assertNull(target.getMaybe(String::class))
    assertEquals(0, target.size)
  }

  @ParameterizedTest
  @MethodSource("getData")
  fun canRemoveSetSubclassValue(factory: () -> MutableHierarchyMap<Any>) {
    val target = factory()
    target.add(String::class)
    assertEquals(String::class, target.getMaybe(CharSequence::class))
    assertEquals(String::class, target.getMaybe(String::class))
    assertEquals(1, target.size)
    assertEquals(2, target.cacheCount)
    target.remove(CharSequence::class)
    assertNull(target.getMaybe(String::class))
    assertNull(target.getMaybe(CharSequence::class))
    assertEquals(0, target.size)
    assertEquals(2, target.cacheCount)
  }

  @ParameterizedTest
  @MethodSource("getData")
  fun canGetOnSameBaseClass(factory: () -> MutableHierarchyMap<Any>) {
    val target = factory()
    target.add(Int::class)
    target.add(Long::class)

    assertEquals(Int::class, target.get())
    assertEquals(Long::class, target[Long::class])
  }

  @ParameterizedTest
  @MethodSource("getData")
  fun errorsOnGetForCommonBase(factory: () -> MutableHierarchyMap<Any>) {
    val target = factory()
    target.add(Int::class)
    target.add(Long::class)
    val error = assertFailsWith<IllegalStateException> {
      target[Number::class]
    }
    assertEquals("multiple values for key: class kotlin.Number", error.message)
  }

  @ParameterizedTest
  @MethodSource("getData")
  fun cachesCorrectly(factory: () -> MutableHierarchyMap<Any>) {
    val target = factory()
    assertEquals(0, target.cacheCount)
    assertFalse(String::class in target)
    assertEquals(1, target.cacheCount)
    target.add(String::class)
    assertEquals(0, target.cacheCount)
    assertTrue(CharSequence::class in target)
    assertEquals(1, target.cacheCount)
  }

  @Test
  fun rootTypeUsage() {
    val target = RootConcurrentHierarchyMap<Collection<*>>()
    assertFalse(Set::class in target)
    target.add(MutableList::class)
    assertFalse(Set::class in target)
    assertTrue(List::class in target)
    assertTrue(MutableList::class in target)
  }
}