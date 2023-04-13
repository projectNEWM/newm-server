package io.newm.shared.ktx

import org.slf4j.Logger

inline fun Logger.trace(message: () -> String) {
    if (isTraceEnabled) trace(message())
}

inline fun Logger.trace(throwable: Throwable, message: () -> String) {
    if (isTraceEnabled) trace(message(), throwable)
}

inline fun Logger.debug(message: () -> String) {
    if (isDebugEnabled) debug(message())
}

inline fun Logger.debug(throwable: Throwable, message: () -> String) {
    if (isDebugEnabled) debug(message(), throwable)
}

inline fun Logger.info(message: () -> String) {
    if (isInfoEnabled) info(message())
}

inline fun Logger.info(throwable: Throwable, message: () -> String) {
    if (isInfoEnabled) info(message(), throwable)
}

inline fun Logger.warn(message: () -> String) {
    if (isWarnEnabled) warn(message())
}

inline fun Logger.warn(throwable: Throwable, message: () -> String) {
    if (isWarnEnabled) warn(message(), throwable)
}

inline fun Logger.error(message: () -> String) {
    if (isErrorEnabled) error(message())
}

inline fun Logger.error(throwable: Throwable, message: () -> String) {
    if (isErrorEnabled) error(message(), throwable)
}
