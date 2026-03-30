package ao.argosidps.ai

import ao.argosidps.colors.RESET
import ao.argosidps.colors.YELLOW
import org.python.icu.text.ReplaceableString
import java.io.File

const val PYTHON_INTERPRETER = "/usr/bin/python3"
const val PYTHON_SCRIPT: String = "/tmp/args/model.py"

fun loadModel() {
    val tmpFolder = "/tmp/args/"
    File(tmpFolder).mkdirs()
    val features = File(tmpFolder + "features.pkl")
    val isoForest = File(tmpFolder + "iso_forest.pkl")
    val labelEncoder = File(tmpFolder + "label_encoder.pkl")
    val model = File(tmpFolder + "model.py")
    val rfClassifier = File(tmpFolder + "rf_classifier.pkl")
    val scaler = File(tmpFolder + "scaler.pkl")

    println("${YELLOW}Extracting model files.${RESET}")
    features.outputStream().write(
        object {}
            .javaClass
            .getResourceAsStream("/features.pkl")!!
            .bufferedReader()
            .readText()
            .toByteArray()
    )
    isoForest.outputStream().write(
        object {}
            .javaClass
            .getResourceAsStream("/iso_forest.pkl")!!
            .bufferedReader()
            .readText()
            .toByteArray()
    )
    labelEncoder.outputStream().write(
        object {}
            .javaClass
            .getResourceAsStream("/label_encoder.pkl")!!
            .bufferedReader()
            .readText()
            .toByteArray()
    )
    model.outputStream().write(
        object {}
            .javaClass
            .getResourceAsStream("/model.py")!!
            .bufferedReader()
            .readText()
            .toByteArray()
    )
    rfClassifier.outputStream().write(
        object {}
            .javaClass
            .getResourceAsStream("/rf_classifier.pkl")!!
            .bufferedReader()
            .readText()
            .toByteArray()
    )
    scaler.outputStream().write(
        object {}
            .javaClass
            .getResourceAsStream("/scaler.pkl")!!
            .bufferedReader()
            .readText()
            .toByteArray()
    )
    println("${YELLOW}Model files extracted.${RESET}")
}

fun runAnalysis(flow: Array<Double>): String {
    val process = ProcessBuilder(PYTHON_INTERPRETER, "-u", PYTHON_SCRIPT, *(flow.map { it.toString() }.toTypedArray()))
        .start()
        /*.inputStream
        .bufferedReader()
        .readText()
        .trim()*/
    val stderr = process.errorStream.bufferedReader().readText()
    val stdout = process.inputStream.bufferedReader().readText()
    println("ERROR: $stderr")
    println("OUPUT: $stdout")
    return "Nothin'"
}