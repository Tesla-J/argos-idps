package ao.argosidps.proxy

import ao.argosidps.configurations.ConfigurationFields
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.ConnectException
import java.net.InetAddress
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import kotlin.experimental.and

suspend fun startSock4Proxy(client: Socket, config: HashMap<String, String>){
    val clientInputStream = client.inputStream
    val clientOutputStream = client.outputStream
    var destination: Socket? = null
    val buffer = ByteArray(1024)

    if (clientInputStream.read(buffer) <= 0){
        println("Nothing received")
        return
    }
    try {
        destination = Socket(
            getIPFromByteArray(4, buffer),
            getPortFromByteArray(2, buffer)
        )
        destination.soTimeout = config[ConfigurationFields.TIMEOUT]!!.toInt()
        clientOutputStream.write(byteArrayOf(0, 90, // Connection granted response
            buffer[2], buffer[3], // destination Port
            buffer[4], buffer[5], buffer[6], buffer[7])) //destination IP
        withContext(Dispatchers.Default){
            val sender = launch(){
                transmit(client, destination)
            }
            val receiver = launch(){
                transmit(destination, client)
            }
            sender.join()
            receiver.join()
            // TODO expect binding requests
        }
        println("=================================================================================================")
    }
    catch (e: SocketException){
        e.printStackTrace()
        // TODO only send when trying to connect
        // TODO check connection request
        // TODO check blacklist
        // Notify client that the request failed
        clientOutputStream.write(byteArrayOf(0, 91, // Connection refused/failed response
            buffer[2], buffer[3], // destination Port
            buffer[4], buffer[5], buffer[6], buffer[7])) //destination IP
    }
    catch (e: SocketTimeoutException){
        println("Connection Timeout!")
    }
    catch (e: ConnectException){
        println("Connection Refused")
    }
    finally {
        destination?.close()
        client.close()
    }
    //clientOutputStream.write(byteArrayOf(90, ))
}

private suspend fun transmit(client: Socket, destination: Socket){
    val buffer =  ByteArray(1024)
    val inputStream = client.inputStream
    val outputStream = destination.outputStream
    var readBytes = 0

    while (!false){
        readBytes = inputStream.read(buffer)
        if (readBytes < 0)
            continue // TODO should I really continue?
        println("DEBUG: transmission data\n${String(buffer)}")
        outputStream.write(buffer.copyOf(readBytes))
        outputStream.flush()
    }
}
