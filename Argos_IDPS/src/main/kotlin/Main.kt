package ao.argosidps

import ao.argosidps.capture.startCapture
import ao.argosidps.capture.testing
import ao.argosidps.configurations.Configuration
import ao.argosidps.privileges.isRoot
import ao.argosidps.proxy.startProxy
import kotlin.system.exitProcess

object AIModelProperties{
    const val PYTHON_INTERPRETER = "python3"
    const val PYTHON_SCRIPT = "./src/main/kotlin/ai/Main.py"
}

suspend fun main() {
    /*val processBuilder = ProcessBuilder()
    val process = processBuilder.command(
        AIModelProperties.PYTHON_INTERPRETER,
        AIModelProperties.PYTHON_SCRIPT
    ).start()*/
    if (!isRoot()) {
        println("root permission required!\n")
        exitProcess(1)
    }
    startCapture()
    //val output = process.inputStream.bufferedReader().readText()
    //println(output)
}