package fledware.utilities

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class GlobMatchTest {
  @Test
  fun testHappyPath() {
    val regex = "hello/*.*".globToRegex()
    assertTrue(regex.matches("hello/world.txt"))
    assertFalse(regex.matches("hello/world/lala.txt"))
  }

  @Test
  fun testDepth() {
    val regex = "hello/**.*".globToRegex()
    assertTrue(regex.matches("hello/world.txt"))
    assertTrue(regex.matches("hello/world/lala.txt"))
  }

  @Test
  fun testExtensionGroups() {
    val regex = "hello/**.{txt,lala,json}".globToRegex()
    assertTrue(regex.matches("hello/world.txt"))
    assertTrue(regex.matches("hello/world.json"))
    assertTrue(regex.matches("hello/world/lala.txt"))
    assertFalse(regex.matches("hello/world/lala.ok"))
  }

  @Test
  fun testClassChecks() {
    val regex = "hell[ok]/**.{txt,lala,json}".globToRegex()
    assertTrue(regex.matches("hello/world.txt"))
    assertTrue(regex.matches("hellk/world.txt"))
    assertTrue(regex.matches("hello/world.json"))
    assertTrue(regex.matches("hellk/world.json"))
    assertTrue(regex.matches("hello/world/lala.txt"))
    assertTrue(regex.matches("hellk/world/lala.txt"))
    assertFalse(regex.matches("hellok/world/lala.txt"))
    assertFalse(regex.matches("hello/world/lala.ok"))
    assertFalse(regex.matches("hellk/world/lala.ok"))
  }

  @Test
  fun testRootFind() {
    val regex = "*.config.*".globToRegex()
    assertFalse(regex.matches("hello/world.txt"))
    assertFalse(regex.matches("hello/world.config.txt"))
    assertTrue(regex.matches("world.config.txt"))
  }
}