package ao.argosidps.display

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** Shared display-control state (thread-safe). */
object DisplayState {
    /** When true the capture loop keeps running but nothing is printed. */
    val paused      = AtomicBoolean(false)
    /** When true the whole JVM should exit. */
    val quit        = AtomicBoolean(false)
    /** Monotonically increasing flow row counter for the table. */
    val rowCounter  = AtomicInteger(0)
}
