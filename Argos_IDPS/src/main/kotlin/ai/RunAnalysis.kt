package ao.argosidps.ai

import ao.argosidps.colors.RED
import ao.argosidps.colors.RESET
import ao.argosidps.colors.YELLOW
import org.python.icu.text.ReplaceableString
import java.io.File
import java.net.Socket

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
    ProcessBuilder(PYTHON_INTERPRETER, "-m", "pip", "install", "pandas", "numpy", "scikit-learn==1.6.1")
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
    println("${YELLOW}Freeing port 3469.${RESET}")
    ProcessBuilder("fuser", "-k", "3469/tcp")
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()
        .waitFor()
    println("${YELLOW}Starting Argos Analyst.${RESET}")
    val serverProcess = ProcessBuilder(PYTHON_INTERPRETER, PYTHON_SCRIPT)
        .redirectError(ProcessBuilder.Redirect.DISCARD)  // Suppress Python warnings
        .start()
    // Chek if server is ready
    var isReady = false
    while (!isReady) {
        try{
            val sock = Socket("localhost", 3469)
            if (sock.isConnected) {
                sock.close()
                isReady = true
            }
        }catch (_: Exception){
            Thread.sleep(3000)
            println("${RED}Failed to connect to analysis module, retrying...${RESET}")
        }
    }
    // Sets a hook to kill the server when jvm exits
    Runtime.getRuntime().addShutdownHook(Thread {
        serverProcess?.destroy()
    })
    println("${YELLOW}Argos Analyst started.${RESET}")
}

fun runAnalysis(flow: Array<Double>): String {
    val addr = "localhost"
    val port = 3469
    val bufferSize = 8192  // Increased buffer size
    val socket:Socket
    val result = ByteArray(bufferSize)
    var bytesRead = 0;
    val flowData = flow.mapIndexed { idx, value ->
        value.toString() + if (idx < flow.size - 1) "|" else ""
    }.reduce{ acc, next ->
        acc + next
    }.toByteArray()

    try {
        socket = Socket(addr, port)
        while (!socket.isConnected)
            Thread.sleep(100)
        socket.outputStream.write(flowData)
        bytesRead = socket.inputStream.read(result)
    } catch (e: Exception){
        e.printStackTrace()
        return "???"
    }
    socket.close()
    return String(result, 0, bytesRead)
}

/** Extracts attack type from JSON model response. */
fun extractAttackType(jsonResponse: String): String {
    return try {
        // Look for attack_type field in JSON (model returns "attack_type": "name")
        val patterns = listOf(
            "\"attack_type\"\\s*:\\s*\"([^\"]+)\"",
            "\"attackType\"\\s*:\\s*\"([^\"]+)\"",
            "\"attack_name\"\\s*:\\s*\"([^\"]+)\"",
            "\"attackName\"\\s*:\\s*\"([^\"]+)\"",
            "\"attack\"\\s*:\\s*\"([^\"]+)\"",
            "\"class\"\\s*:\\s*\"([^\"]+)\""
        )
        
        for (pattern in patterns) {
            val regex = Regex(pattern)
            val matchResult = regex.find(jsonResponse)
            if (matchResult != null) {
                val attackType = matchResult.groupValues[1]
                if (attackType.isNotEmpty() && attackType.lowercase() != "unknown") {
                    return attackType
                }
            }
        }
        
        ""
    } catch (e: Exception) {
        ""
    }
}

/** Checks if response indicates anomaly. */
fun isAnomalyResponse(jsonResponse: String): Boolean {
    return try {
        // Check for anomaly indicators - model returns "status": "ANOMALIA" or "status": "NORMAL"
        val anomalyPatterns = listOf(
            "\"status\"\\s*:\\s*\"ANOMALIA\"",  // Exact match for ANOMALIA
            "\"status\"\\s*:\\s*\"anomalia\"",  // Case insensitive
            "\"anomaly\"\\s*:\\s*(true|1)",
            "\"is_anomaly\"\\s*:\\s*(true|1)"
        )
        
        for (pattern in anomalyPatterns) {
            val regex = Regex(pattern, RegexOption.IGNORE_CASE)
            if (regex.containsMatchIn(jsonResponse)) {
                return true
            }
        }
        false
    } catch (e: Exception) {
        false
    }
}
