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

interface SomeBaseType

abstract class AbstractOfSomeBaseType : SomeBaseType

class ConcreteOfSomeBaseTypeA : AbstractOfSomeBaseType()

class ConcreteOfSomeBaseTypeB : AbstractOfSomeBaseType()

class ConcreteOfSomeBaseTypeC : SomeBaseType

class TypedMapTest {
  companion object {
    @JvmStatic
    fun getData() = listOf(
        Arguments.of({ DefaultTypedMap() } as () -> MutableTypedMap<Any>),
        Arguments.of({ ConcurrentTypedMap() } as () -> MutableTypedMap<Any>),
        Arguments.of({ RootDefaultTypedMap<Any>() } as () -> MutableTypedMap<Any>),
        Arguments.of({ RootConcurrentTypedMap<Any>() } as () -> MutableTypedMap<Any>),
    )
  }

  @ParameterizedTest
  @MethodSource("getData")
  fun worksForComplexHierarchy(factory: () -> MutableTypedMap<Any>) {
    val target = factory()
    assertNull(target.getOrNull(SomeBaseType::class))
    assertNull(target.getOrNull(AbstractOfSomeBaseType::class))
    assertNull(target.getOrNull(ConcreteOfSomeBaseTypeA::class))
    assertNull(target.getOrNull(ConcreteOfSomeBaseTypeB::class))
    assertNull(target.getOrNull(ConcreteOfSomeBaseTypeC::class))
    val aThing = ConcreteOfSomeBaseTypeA()
    target.put(aThing)
    assertEquals(aThing, target.getOrNull(SomeBaseType::class))
    assertEquals(aThing, target.getOrNull(AbstractOfSomeBaseType::class))
    assertEquals(aThing, target.getOrNull(ConcreteOfSomeBaseTypeA::class))
    assertNull(target.getOrNull(ConcreteOfSomeBaseTypeB::class))
    assertNull(target.getOrNull(ConcreteOfSomeBaseTypeC::class))
  }

  @ParameterizedTest
  @MethodSource("getData")
  fun canReturnNull(factory: () -> MutableTypedMap<Any>) {
    val target = factory()
    assertNull(target.getOrNull(String::class))
  }

  @ParameterizedTest
  @MethodSource("getData")
  fun canFindSetValue(factory: () -> MutableTypedMap<Any>) {
    val target = factory()
    target.put("hello")
    assertEquals("hello", target.getOrNull(String::class))
  }

  @ParameterizedTest
  @MethodSource("getData")
  fun canFindSubclassValue(factory: () -> MutableTypedMap<Any>) {
    val target = factory()
    target.put("hello")
    assertEquals("hello", target.getOrNull(CharSequence::class))
  }

  @ParameterizedTest
  @MethodSource("getData")
  fun canRemoveWithoutErrorOnNoValue(factory: () -> MutableTypedMap<Any>) {
    val target = factory()
    target.remove(String::class)
  }

  @ParameterizedTest
  @MethodSource("getData")
  fun canRemoveSetValue(factory: () -> MutableTypedMap<Any>) {
    val target = factory()
    target.put("hello")
    assertNotNull(target.getOrNull(String::class))
    assertEquals(1, target.size)
    target.remove(String::class)
    assertNull(target.getOrNull(String::class))
    assertEquals(0, target.size)
  }

  @ParameterizedTest
  @MethodSource("getData")
  fun canRemoveSetSubclassValue(factory: () -> MutableTypedMap<Any>) {
    val target = factory()
    target.put("hello")
    assertNotNull(target.getOrNull(String::class))
    assertEquals(1, target.size)
    target.remove(CharSequence::class)
    assertNull(target.getOrNull(String::class))
    assertEquals(0, target.size)
  }

  @ParameterizedTest
  @MethodSource("getData")
  fun canGetOnSameBaseClass(factory: () -> MutableTypedMap<Any>) {
    val target = factory()
    target.put(123.123)
    target.put(346L)

    assertEquals(123.123, target.get())
    assertEquals(346L, target[Long::class])
  }

  @ParameterizedTest
  @MethodSource("getData")
  fun errorsOnGetForCommonBase(factory: () -> MutableTypedMap<Any>) {
    val target = factory()
    target.put(123.123)
    target.put(346L)
    val error = assertFailsWith<IllegalStateException> {
      target[Number::class]
    }
    assertEquals("multiple values for key: class kotlin.Number", error.message)
  }

  @ParameterizedTest
  @MethodSource("getData")
  fun cachesCorrectly(factory: () -> MutableTypedMap<Any>) {
    val target = factory()
    assertEquals(0, target.cacheCount)
    assertFalse(String::class in target)
    assertEquals(1, target.cacheCount)
    target.put(123)
    assertEquals(0, target.cacheCount)
    assertFalse(String::class in target)
    assertEquals(1, target.cacheCount)
  }

  @Test
  fun rootTypeUsage() {
    val target = RootDefaultTypedMap<Number>()
    assertFalse(Number::class in target)
    target.put(123)
    assertTrue(Number::class in target)
    assertTrue(Int::class in target)
    assertFalse(Long::class in target)
  }
}
