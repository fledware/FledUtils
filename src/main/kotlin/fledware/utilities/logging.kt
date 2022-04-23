package fledware.utilities

import org.slf4j.Logger
import kotlin.system.measureTimeMillis


inline fun Logger.trace(throwable: Throwable? = null, message: () -> String) =
    Unit.also { if (isTraceEnabled) trace(message(), throwable) }

inline fun Logger.debug(throwable: Throwable? = null, message: () -> String) =
    Unit.also { if (isDebugEnabled) debug(message(), throwable) }

inline fun Logger.info(throwable: Throwable? = null, message: () -> String) =
    Unit.also { if (isInfoEnabled) info(message(), throwable) }

inline fun Logger.warn(throwable: Throwable? = null, message: () -> String) =
    Unit.also { if (isWarnEnabled) warn(message(), throwable) }

inline fun Logger.error(throwable: Throwable? = null, message: () -> String) =
    Unit.also { if (isErrorEnabled) error(message(), throwable) }

inline fun Logger.traceMeasure(stage: String, block: () -> Unit) =
    measureTimeMillis(block).also { trace { "$stage took $it ms" } }

inline fun Logger.debugMeasure(stage: String, block: () -> Unit) =
    measureTimeMillis(block).also { debug { "$stage took $it ms" } }

inline fun Logger.infoMeasure(stage: String, block: () -> Unit) =
    measureTimeMillis(block).also { info { "$stage took $it ms" } }
