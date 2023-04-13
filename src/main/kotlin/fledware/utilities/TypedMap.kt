package fledware.utilities

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * The access for a typed map.
 *
 * @param R the root type for access
 */
interface TypedMap<R : Any> {
  val size: Int
  val cacheCount: Int
  val entries: Set<Map.Entry<KClass<*>, R>>
  val keys: Set<KClass<*>>
  val values: Collection<R>

  /**
   * Returns true if there is a value that extends [key]
   *
   * @param key the key to search for
   * @throws IllegalStateException if multiple values are instances of [key]
   * @return true if the value exists.
   */
  operator fun <T : R> contains(key: KClass<T>): Boolean

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
  operator fun <T : R> get(key: KClass<T>): T

  /**
   * Get the first value that is an instance of [key]. If there
   * are multiple values that are instances of [key], an exception
   * is thrown. Returns null if no value in instance of [key].
   *
   * @param key the key to search for
   * @throws IllegalStateException if there are no values that extend [key]
   * @throws IllegalStateException if multiple values are instances of [key]
   * @return the value that implements [key]
   */
  fun <T : R> getOrNull(key: KClass<T>): T?

  /**
   * Searches for the exact type. This will not do any instance
   * of checks.
   *
   * @param key the key check for
   * @throws IllegalStateException if there are no values that extend [key]
   * @return the value that is the [key] type
   */
  fun <T : R> getExact(key: KClass<T>): T

  /**
   * Searches for the exact type. This will not do any instance
   * of checks.
   *
   * @param key the key check for
   * @return the value that is the [key] type
   */
  fun <T : R> getExactOrNull(key: KClass<T>): T?
}

inline fun <reified T : Any> TypedMap<Any>.get() = get(T::class)

inline fun <reified T : Any> TypedMap<Any>.getOrNull() = getOrNull(T::class)

inline fun <reified T : Any> TypedMap<Any>.getExact() = getExact(T::class)

inline fun <reified T : Any> TypedMap<Any>.getExactOrNull() = getExactOrNull(T::class)

/**
 * The base implementation for a [TypedMap].
 */
abstract class AbstractTypedMap<R : Any> : TypedMap<R> {
  private val cacheEmptyMarker = Any()
  protected abstract val data: MutableMap<KClass<*>, R>
  protected abstract val cache: MutableMap<KClass<*>, Any>

  override val size get() = data.size

  override val cacheCount get() = cache.size

  override val entries: Set<Map.Entry<KClass<*>, R>> get() = data.entries

  override val keys: Set<KClass<*>> get() = data.keys

  override val values: Collection<R> get() = data.values

  override operator fun <T : R> contains(key: KClass<T>): Boolean = getOrNull(key) != null

  override operator fun <T : R> get(key: KClass<T>): T =
      getOrNull(key) ?: throw IllegalStateException("key not found: $key")

  @Suppress("UNCHECKED_CAST")
  override fun <T : R> getExact(key: KClass<T>): T =
      data[key] as? T ?: throw IllegalStateException("key not found: $key")

  @Suppress("UNCHECKED_CAST")
  override fun <T : R> getExactOrNull(key: KClass<T>): T? =
      data[key] as? T

  override fun <T : R> getOrNull(key: KClass<T>): T? = findValueOrFillCache(key)

  @Suppress("UNCHECKED_CAST")
  protected fun <T : Any> findValueOrFillCache(key: KClass<T>): T? {
    val check = cache[key]
    if (check === cacheEmptyMarker) return null
    if (check != null) return check as T

    var resultValue: T? = null
    for (value in data.values) {
      if (key.isInstance(value)) {
        if (resultValue != null)
          throw IllegalStateException("multiple values for key: $key")
        resultValue = value as T
      }
    }
    cache[key] = resultValue ?: cacheEmptyMarker
    return resultValue
  }
}

/**
 * The mutable part of a [TypedMap].
 *
 * We want to split this to give the option for public
 * interfaces to be immutable.
 */
interface MutableTypedMap<R : Any> : TypedMap<R> {
  /**
   * puts the new value, and returns the old if exists.
   *
   * @param value the value to put
   * @return the previous value, if there was one.
   */
  fun <T : R> put(value: T): T?

  /**
   * gets the value or puts if not available.
   *
   * there is a chance that there could be double inserts
   * if this method is called frequently. Make sure the
   * value from [block] can be worked on lazily and won't
   * break things if multiple puts happen.
   *
   * Basically, this is not an atomic operation and there
   * is a chance the returned value may have been replaced
   * by the time this method returns.
   */
  fun <T : R> getOrPut(key: KClass<T>, block: () -> T): T

  /**
   * Adds the new value, but if the value already exists, throws
   * and exception.
   *
   * @param value the value to add
   * @throws IllegalStateException if value already exists
   */
  fun <T : R> add(value: T)

  /**
   * Removes the given type. This will do the standard check
   * and look through all entries and find the first instance
   * that extends the class. If there are multiple, an exception
   * is thrown.
   *
   * @param key the type to remove
   * @throws IllegalStateException if multiple values are instances of [key]
   * @return the value that got removed, if any
   */
  fun <T : R> remove(key: KClass<T>): T?

  /**
   * clears all data
   */
  fun clear()
}

/**
 * The base implementation of the mutable typed map.
 */
abstract class AbstractMutableTypedMap<R : Any> : AbstractTypedMap<R>(), MutableTypedMap<R> {

  override fun <T : R> put(value: T): T? {
    val result = data.put(value::class, value)
    cache.clear()
    @Suppress("UNCHECKED_CAST")
    return result as T?
  }

  override fun <T : R> getOrPut(key: KClass<T>, block: () -> T): T {
    val check = getOrNull(key)
    if (check != null) return check
    put(block())
    return get(key)
  }

  override fun <T : R> add(value: T) {
    if (data.putIfAbsent(value::class, value) != null)
      throw IllegalStateException("key already exists: $value")
    cache.clear()
  }

  override fun clear() {
    data.clear()
    cache.clear()
  }

  override fun <T : R> remove(key: KClass<T>): T? {
    val value = findValueOrFillCache(key) ?: return null
    data.remove(value::class)
    cache.clear()
    return value
  }
}


class DefaultTypedMap : AbstractMutableTypedMap<Any>() {
  override val data = mutableMapOf<KClass<*>, Any>()
  override val cache = mutableMapOf<KClass<*>, Any>()
}

class ConcurrentTypedMap : AbstractMutableTypedMap<Any>() {
  override val data = ConcurrentHashMap<KClass<*>, Any>()
  override val cache = ConcurrentHashMap<KClass<*>, Any>()
}


class RootDefaultTypedMap<R : Any> : AbstractMutableTypedMap<R>() {
  override val data = mutableMapOf<KClass<*>, R>()
  override val cache = mutableMapOf<KClass<*>, Any>()
}

class RootConcurrentTypedMap<R : Any> : AbstractMutableTypedMap<R>() {
  override val data = ConcurrentHashMap<KClass<*>, R>()
  override val cache = ConcurrentHashMap<KClass<*>, Any>()
}


