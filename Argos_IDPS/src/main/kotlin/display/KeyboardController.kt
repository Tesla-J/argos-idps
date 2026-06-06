package ao.argosidps.display

import ao.argosidps.colors.BOLD
import ao.argosidps.colors.CYAN
import ao.argosidps.colors.GREEN
import ao.argosidps.colors.RED
import ao.argosidps.colors.RESET
import ao.argosidps.colors.YELLOW
import kotlin.system.exitProcess

/**
 * Starts a daemon thread that reads single keypresses from stdin without echo or buffering.
 * The terminal is set to raw mode so characters are read immediately without waiting for Enter.
 * Characters are NOT displayed on screen.
 *
 *  p  ->  pause  display
 *  c  ->  continue / resume display
 *  q  ->  quit the whole program
 */
fun startKeyboardController() {
    // Set terminal to raw mode (no echo, character-at-a-time reading)
    try {
        Runtime.getRuntime().exec(arrayOf("/bin/sh", "-c", "stty -echo -icanon < /dev/tty")).waitFor()
    } catch (e: Exception) {
        System.err.println("[ARGOS] Warning: Could not set terminal to raw mode")
    }

    // Restore terminal to normal mode on JVM exit
    Runtime.getRuntime().addShutdownHook(Thread {
        try {
            Runtime.getRuntime().exec(arrayOf("/bin/sh", "-c", "stty echo icanon < /dev/tty")).waitFor()
        } catch (e: Exception) {
            // Ignore errors during cleanup
        }
    })

    val thread = Thread {
        try {
            println("${CYAN}${BOLD}[ARGOS] Keyboard controls: p=pause, c=continue, q=quit${RESET}")
            
            val inputStream = System.`in`
            while (!DisplayState.quit.get()) {
                try {
                    val byte = inputStream.read()
                    if (byte == -1) break
                    
                    val key = byte.toChar().lowercaseChar()
                    when (key) {
                        'p' -> {
                            if (!DisplayState.paused.get()) {
                                DisplayState.paused.set(true)
                                println()
                                println("$YELLOW${BOLD}[ARGOS] Display PAUSED — press 'c' to resume, 'q' to quit.$RESET")
                            }
                        }
                        'c' -> {
                            if (DisplayState.paused.get()) {
                                DisplayState.paused.set(false)
                                println("$GREEN${BOLD}[ARGOS] Display RESUMED.$RESET")
                                printTableHeader()
                            }
                        }
                        'q' -> {
                            println()
                            println("$RED${BOLD}[ARGOS] Shutting down...$RESET")
                            DisplayState.quit.set(true)
                            exitProcess(0)
                        }
                        else -> { /* ignore other keys */ }
                    }
                } catch (e: Exception) {
                    break
                }
            }
        } catch (e: Exception) {
            System.err.println("[ARGOS] Keyboard controller error: ${e.message}")
        }
    }
    thread.isDaemon = true
    thread.name = "argos-keyboard"
    thread.start()
}
