package ao.argosidps.ai

import ao.argosidps.colors.RESET
import ao.argosidps.colors.YELLOW
import org.python.icu.text.ReplaceableString
import java.io.File

const val PYTHON_INTERPRETER = "/tmp/argos/env/bin/python3"
const val PYTHON_SCRIPT: String = "/tmp/argos/model.py"

fun loadModel() {
    val tmpFolder = "/tmp/argos/"
    File(tmpFolder).mkdirs()
    val features = File(tmpFolder + "features.pkl")
    val isoForest = File(tmpFolder + "iso_forest.pkl")
    val labelEncoder = File(tmpFolder + "label_encoder.pkl")
    val model = File(tmpFolder + "model.py")
    val rfClassifier = File(tmpFolder + "rf_classifier.pkl")
    val scaler = File(tmpFolder + "scaler.pkl")

    // Creating environment
    ProcessBuilder("python3", "-m", "venv", "/tmp/argos/env")
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()
        .waitFor()
    // Enabling environment
    //ProcessBuilder("/bin/bash", "-c", "source /tmp/argos/env/bin/activate").start()
    // Installing python libs
    ProcessBuilder("/tmp/argos/env/bin/pip3", "install", "pandas", "numpy", "scikit-learn==1.6.1")
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()
        .waitFor()
    println("${YELLOW}Extracting model files.${RESET}")
    features.outputStream().use{ output ->
        object {}
            .javaClass
            .getResourceAsStream("/features.pkl")!!
            .use{ data ->
                data.copyTo(output)
            }
    }
    isoForest.outputStream().use{ output ->
        object {}
            .javaClass
            .getResourceAsStream("/iso_forest.pkl")!!
            .use{ data ->
                data.copyTo(output)
            }
    }
    labelEncoder.outputStream().use{ output ->
        object {}
            .javaClass
            .getResourceAsStream("/label_encoder.pkl")!!
            .use{ data ->
                data.copyTo(output)
            }
    }
    model.outputStream().write(
        object {}
            .javaClass
            .getResourceAsStream("/model.py")!!
            .bufferedReader()
            .readText()
            .toByteArray()
    )
    rfClassifier.outputStream().use{ output ->
        object {}
            .javaClass
            .getResourceAsStream("/rf_classifier.pkl")!!
            .use{ data ->
                data.copyTo(output)
            }
    }
    scaler.outputStream().use{ output ->
        object {}
            .javaClass
            .getResourceAsStream("/scaler.pkl")!!
            .use{ data ->
                data.copyTo(output)
            }
    }
    println("${YELLOW}Model files extracted.${RESET}")
}

fun runAnalysis(flow: Array<Double>): String =
    ProcessBuilder(PYTHON_INTERPRETER, "-u", PYTHON_SCRIPT, *(flow.map { it.toString() }.toTypedArray()))
        .start()
        .inputStream
        .bufferedReader()
        .readText()
        .trim()
    /*val stderr = process.errorStream.bufferedReader().readText()
    val stdout = process.inputStream.bufferedReader().readText()
    println("ERROR: $stderr")
    println("OUPUT: $stdout")
    return "Nothin'"*/
