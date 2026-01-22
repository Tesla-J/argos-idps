package ao.argosidps.proxy

import ao.argosidps.configurations.ConfigurationFields
import ao.argosidps.log.NetOps
import ao.argosidps.log.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.ConnectException
import java.net.InetAddress
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import kotlin.experimental.and
import kotlin.text.toInt

suspend fun startSock4Proxy(client: Socket, config: HashMap<String, String>){
    val clientInputStream = client.inputStream
    val clientOutputStream = client.outputStream
    var destination: Socket? = null
    val buffer = ByteArray(1024)
    val response = byteArrayOf(0, 90, // Connection granted response
        buffer[2], buffer[3], // destination Port
        buffer[4], buffer[5], buffer[6], buffer[7])

    if (clientInputStream.read(buffer) <= 0){
        println("Nothing received")
        return
    }
    try {
        destination = Socket(
            getIPFromByteArray(4, buffer),
            getPortFromByteArray(2, buffer)
        )
        log(client, destination, byteArrayOf(), NetOps.CONNECT)
        destination.soTimeout = config[ConfigurationFields.TIMEOUT]!!.toInt()
        clientOutputStream.write(response) //destination IP
        log(client, destination, byteArrayOf(), NetOps.SEND)
        withContext(Dispatchers.Default){
            val sender = launch(){
                transmit(client, destination, true)
            }
            val receiver = launch(){
                transmit(destination, client, false)
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
        response[2] = 91
        clientOutputStream.write(response)
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

private suspend fun processBinding(client: Socket, request: ByteArray) {
    val ip = getIPFromByteArray(4, request)
    val port = getPortFromByteArray(2, request)
    var destination: Socket? = null
    val outputStream: OutputStream = client.outputStream
    val response = byteArrayOf(0, 90,
        request[2], request[3],
        request[4], request[5], request[6], request[7]
    )
    var readBytes = 0
    val buffer = ByteArray(1024) // TODO load from configurations

    try {
        destination = Socket(ip, port)
        destination.soTimeout = 5000 // TODO load timeout from configs
        if (!destination.isConnected)
            throw SocketException()
        log(client, destination, byteArrayOf(), NetOps.BIND)
        withContext(Dispatchers.Default){
            val send = launch { transmit(client, destination, true) }
            val receive = launch { transmit(destination, client, false) }
            send.join()
            receive.join()
        }
    }
    catch (e: SocketException) {
        response[1] = 91
        outputStream.write(response)
        outputStream.flush()
    }
    catch (e: SocketTimeoutException){
        response[1] = 91
        outputStream.write(response)
        outputStream.flush()
    }
    finally{
        destination?.close()
        client.close()
    }
}

private suspend fun transmit(source: Socket, destination: Socket, isFromClient: Boolean){
    val buffer =  ByteArray(1024)
    val inputStream = source.inputStream
    val outputStream = destination.outputStream
    var readBytes = 0

    while (!false){
        readBytes = inputStream.read(buffer)
        if (readBytes < 0) {
            destination.shutdownOutput()
            break // TODO should I really continue?
        }
        if (isFromClient && isBindRequest(buffer))
        {
            withContext(Dispatchers.Default){
                launch{
                    println("Binding...")
                    //processBinding(source, buffer)
                }.start()
            }
        }
        //println("DEBUG: transmission data\n${String(buffer)}")
        outputStream.write(buffer.copyOf(readBytes))
        outputStream.flush()
        log(source, destination, buffer.copyOf(readBytes), NetOps.SEND)
    }
}

private fun isBindRequest(data: ByteArray): Boolean {
    if (data.size < 8 || data[2] != 2.toByte())
        return false
    return true
}