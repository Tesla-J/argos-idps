package ao.argosidps

import ao.argosidps.configurations.Configuration
import ao.argosidps.proxy.startProxy

object AIModelProperties{
    const val PYTHON_INTERPRETER = "python3"
    const val PYTHON_SCRIPT = "./src/main/kotlin/ai/Main.py"
}

suspend fun main() {
    val processBuilder = ProcessBuilder()
    val process = processBuilder.command(
        AIModelProperties.PYTHON_INTERPRETER,
        AIModelProperties.PYTHON_SCRIPT
    ).start()
    startProxy()
    //val output = process.inputStream.bufferedReader().readText()
    //println(output)
}