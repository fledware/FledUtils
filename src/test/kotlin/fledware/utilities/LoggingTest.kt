package fledware.utilities

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.system.measureTimeMillis
import kotlin.test.Test

class LoggingTest {
  lateinit var logger: Logger
  @Test
  fun sillyTest() {
    measureTimeMillis {
      logger = LoggerFactory.getLogger(LoggingTest::class.java)
    }.also { println("logger load: $it ms") }
    measureTimeMillis {
      logger.info { "hello" }
    }.also { println("logger info: $it ms") }
  }
}