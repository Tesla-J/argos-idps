package ao.argosidps.log

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.Socket

enum class NetOps(action: String){
    CONNECT("Connected to"),
    BIND("Bound to"),
    SEND("Sent %d bytes of data to"),
    RECEIVE("Received %d bytes from"),
    CLOSE("Closed connection"),
}

suspend fun log(source:Socket, destination: Socket, data: ByteArray, action: NetOps) {
    // TODO send to log file
    withContext(Dispatchers.Default){
        launch {
            var logMessage = "\u001b[32m${source.inetAddress.toString()} %s ${destination.inetAddress.toString()}\u001b[0m".format(action)
            if (action == NetOps.SEND || action == NetOps.RECEIVE)
                logMessage = logMessage.format(data.size)
            println(logMessage)
        }.start()
    }
}