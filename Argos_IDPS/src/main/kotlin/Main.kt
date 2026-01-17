package ao.argosidps

import org.python.util.PythonInterpreter

fun main() {
    val python = "python3"
    val pyMain = "./src/main/kotlin/ai/Main.py"
    val processBuilder = ProcessBuilder()
    val process = processBuilder.command(python, pyMain).start()
    val output = process.inputStream.bufferedReader().readText()
    println(output)
    //Runtime.getRuntime().exec("$python $pyMain")
    //val interpreter = PythonInterpreter()
    //interpreter.execfile("./src/main/kotlin/ai/Main.py")
}