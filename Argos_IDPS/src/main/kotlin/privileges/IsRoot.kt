package ao.argosidps.privileges

fun isRoot(): Boolean =
    ProcessBuilder("/bin/bash", "-c", "id -u")
        .start()
        .inputStream
        .bufferedReader()
        .readText()
        .trim() == "0"