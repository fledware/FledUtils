package fledware.utilities

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

/**
 * A special variant of [TypedMap] that only holds the classes
 * instead of instances.
 */
interface HierarchyMap<R : Any> {
  val size: Int
  val cacheCount: Int
  val values: Collection<KClass<out R>>

  /**
   * Returns true if there is a value that extends [key]
   *
   * @param key the key to search for
   * @throws IllegalStateException if multiple values are instances of [key]
   * @return true if the value exists.
   */
  operator fun contains(key: KClass<out R>): Boolean

  /**
   * Get the first value that is an instance of [key]. If there
   * are multiple values that are instances of [key] or no values
   * that extend [key], an exception is thrown.
   *
   * @param key the key to search for
   * @throws IllegalStateException if there are no values that extend [key]
   * @throws IllegalStateException if multiple values are instances of [key]
   * @return the value that implements [key]
   */
  operator fun <T : R> get(key: KClass<T>): KClass<out T>

  /**
   * Get the first type that is an instance of [key]. If there
   * are multiple values that are instances of [key], an exception
   * is thrown.
   *
   * @param key the key to search for
   * @throws IllegalStateException if there are no values that extend [key]
   * @throws IllegalStateException if multiple values are instances of [key]
   * @return the value that implements [key]
   */
  fun <T : R> getOrNull(key: KClass<T>): KClass<out T>?
}

inline fun <reified T : Any> HierarchyMap<Any>.get() = get(T::class)

inline fun <reified T : Any> HierarchyMap<Any>.getOrNull() = getOrNull(T::class)

/**
 * The base implementation for a [HierarchyMap].
 */
abstract class AbstractHierarchyMap<R : Any> : HierarchyMap<R> {
  private val cacheEmptyMarker = Any()
  protected abstract val data: MutableSet<KClass<out R>>
  protected abstract val cache: MutableMap<KClass<*>, Any>

  override val size get() = data.size

  override val cacheCount get() = cache.size

  override val values: Collection<KClass<out R>> get() = data

  override operator fun contains(key: KClass<out R>): Boolean = getOrNull(key) != null

  override operator fun <T : R> get(key: KClass<T>): KClass<out T> =
      getOrNull(key) ?: throw IllegalStateException("key not found: $key")

  @Suppress("UNCHECKED_CAST")
  override fun <T : R> getOrNull(key: KClass<T>): KClass<out T>? = findValueOrFillCache(key)

  @Suppress("UNCHECKED_CAST")
  protected fun <T : R> findValueOrFillCache(key: KClass<T>): KClass<out T>? {
    val check = cache[key]
    if (check === cacheEmptyMarker) return null
    if (check != null) return check as KClass<out T>

    var resultValue: KClass<out T>? = null
    for (type in data) {
      if (key.isSuperclassOf(type)) {
        if (resultValue != null)
          throw IllegalStateException("multiple values for key: $key")
        resultValue = type as KClass<out T>
      }
    }
    cache[key] = resultValue ?: cacheEmptyMarker
    return resultValue
  }
}

/**
 * The mutable part of a [HierarchyMap].
 *
 * We want to split this to give the option for public
 * interfaces to be immutable.
 */
interface MutableHierarchyMap<R : Any> : HierarchyMap<R> {

  /**
   * Adds the new value. If the value already exists, then
   * no mutation happens.
   *
   * @param value the value to add
   * @return true if the value didn't exist and was added
   */
  fun add(value: KClass<out R>): Boolean

  /**
   * Removes the given type. This will do the standard check
   * and look through all entries and find the first instance
   * that extends the class. If there are multiple, an exception
   * is thrown.
   *
   * @param key the type to remove
   * @throws IllegalStateException if multiple values are instances of [key]
   * @return true if the value was removed
   */
  fun remove(key: KClass<out R>): Boolean

  /**
   * clears all data
   */
  fun clear()
}

/**
 * The base implementation of the [MutableHierarchyMap].
 */
abstract class AbstractMutableHierarchyMap<R : Any> : AbstractHierarchyMap<R>(), MutableHierarchyMap<R> {

  override fun add(value: KClass<out R>): Boolean {
    val result = data.add(value)
    if (result) cache.clear()
    return result
  }

  override fun clear() {
    data.clear()
    cache.clear()
  }

  override fun remove(key: KClass<out R>): Boolean {
    val value = findValueOrFillCache(key) ?: return false
    data.remove(value)
    cache.clear()
    return true
  }
}

class ConcurrentHierarchyMap : AbstractMutableHierarchyMap<Any>() {
  override val data = ConcurrentHashMap.newKeySet<KClass<out Any>>()!!
  override val cache = ConcurrentHashMap<KClass<*>, Any>()
}

class RootConcurrentHierarchyMap<R : Any> : AbstractMutableHierarchyMap<R>() {
  override val data = ConcurrentHashMap.newKeySet<KClass<out R>>()!!
  override val cache = ConcurrentHashMap<KClass<*>, Any>()
}
