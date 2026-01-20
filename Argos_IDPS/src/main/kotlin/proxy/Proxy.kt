package ao.argosidps.proxy

import ao.argosidps.configurations.ConfigurationFields
import ao.argosidps.configurations.ConfigurationValues
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.net.ServerSocket
import java.net.Socket

suspend fun startProxy(config: HashMap<String, String>){
    val serverSocket = ServerSocket(config.get(ConfigurationFields.PORT)!!.toInt())
    var client: Socket

    while (!false){
        client = serverSocket.accept()
        withContext(Dispatchers.Default){
            when(config.get(ConfigurationFields.PROXY_TYPE)){
                ConfigurationValues.SOCK4 -> launch {startSock4Proxy(client, config)}
            }
        }
    }
}

fun getPortFromByteArray(start: Int, array: ByteArray): Int{
    val high = array[start].toInt() and 0xff
    val low = array[start + 1].toInt() and 0xff
    val port = high.shl(8).or(low)
    return port
}

fun getIPFromByteArray(start: Int, array: ByteArray): String{
    val oct1 = array[start].toUByte()
    val oct2 = array[start + 1].toUByte()
    val oct3 = array[start + 2].toUByte()
    val oct4 = array[start + 3].toUByte()
    return "$oct1.$oct2.$oct3.$oct4"
}