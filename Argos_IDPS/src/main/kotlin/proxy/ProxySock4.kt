package ao.argosidps.proxy

import ao.argosidps.configurations.Configuration
import ao.argosidps.log.NetOps
import ao.argosidps.log.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.ConnectException
import java.net.InetAddress
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import kotlin.experimental.and
import kotlin.text.toInt

private val proxyScobe = CoroutineScope(Dispatchers.IO + SupervisorJob())

suspend fun startSock4Proxy(client: Socket){
    val clientInputStream = client.inputStream
    val clientOutputStream = client.outputStream
    var destination: Socket? = null
    val buffer = ByteArray(4096 * 4)
    val response = byteArrayOf(0, 90, // Connection granted response
        buffer[2], buffer[3], // destination Port
        buffer[4], buffer[5], buffer[6], buffer[7])
    var readBytes = clientInputStream.read(buffer)
    if (readBytes <= 0){
        println("Nothing received")
        return
    }
    //buffer.forEachIndexed{i, n -> if (i < readBytes ) println(n)}
    //println("Data size = ${readBytes}")
    //println(Configuration.timeout)
    //try {
        if (isConnect(buffer))
            runConcurrentProxyOperation {
                processConnection(client, buffer)
            }
        else if (isBindRequest(buffer))
            runConcurrentProxyOperation {
                processBinding(client, buffer)
            }
        else println("None above!")
        /*destination = Socket(
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
        }*/
        println("=================================================================================================")
    /*}
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
    }*/
    //clientOutputStream.write(byteArrayOf(90, ))
}

private suspend fun runConcurrentProxyOperation(operation: suspend () -> Unit) =
    proxyScobe.launch{
        operation()
    }

private suspend fun processBinding(client: Socket, request: ByteArray) { println("Processing binding")
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
        outputStream.write(response)
        outputStream.flush()
        destination = Socket(ip, port)
        destination.soTimeout = Configuration.port
        if (!destination.isConnected)
            throw SocketException()
        outputStream.write(response)
        outputStream.flush()
        log(client, destination, byteArrayOf(), NetOps.BIND)
        withContext(Dispatchers.IO){
            val send = launch { transmit(client, destination, true) }
            val receive = launch { transmit(destination, client, false) }
            //send.join()
            //receive.join()
            select<Unit>{
                send.onJoin{receive.cancel()}
                receive.onJoin{send.cancel()}
            }
        }
    }
    catch (e: SocketException) {
        response[1] = 91
    }
    catch (e: SocketTimeoutException){
        response[1] = 91
    }
    finally{
        destination?.close()
        outputStream.write(response)
        outputStream.flush()
        outputStream.close()
        client.close()
    }
}

private suspend fun processConnection(client: Socket, request: ByteArray) { println("Processing connection")
    val ip = getIPFromByteArray(4, request)
    val port = getPortFromByteArray(2, request)
    var destination: Socket? = null
    val outputStream: OutputStream = client.outputStream
    val response = byteArrayOf(0, 90,
        request[2], request[3],
        request[4], request[5], request[6], request[7]
    )
    var hadException = false
    //var readBytes = 0
    //val buffer = ByteArray(1024) // TODO load from configurations

    try {
        println("$ip:$port")
        destination = Socket(ip, port)
        destination.soTimeout = Configuration.port
        if (!destination.isConnected)
            throw SocketException()
        log(client, destination, byteArrayOf(), NetOps.CONNECT)
        outputStream.write(response)
        outputStream.flush()
        withContext(Dispatchers.IO){
            val send = launch { transmit(client, destination, true) }
            val receive = launch { transmit(destination, client, false) }
            send.join()
            receive.join()
        }
    }
    catch (e: SocketException) {
        response[1] = 91
        hadException = true
    }
    catch (e: SocketTimeoutException){
        response[1] = 91
        hadException = true
    }
    catch (e: ConnectException){
        response[1] = 91
        hadException = true
    }
    finally{
        destination?.close()
        if (hadException)
            outputStream.write(response)
        outputStream.flush()
        outputStream.close()
        client.close()
    }
}

private suspend fun transmit(source: Socket, destination: Socket, isFromClient: Boolean){
    val buffer =  ByteArray(4096 * 4)
    val inputStream = source.inputStream
    val outputStream = destination.outputStream
    var readBytes = 0

    while (!false){
        readBytes = inputStream.read(buffer)
        if (readBytes < 0) {
            break // TODO should I really continue?
        }
        /*if (isFromClient && isBindRequest(buffer))
        {
            withContext(Dispatchers.Default){
                launch{
                    println("Binding...")
                    //processBinding(source, buffer)
                }.start()
            }
        }*/
        //println("DEBUG: transmission data\n${String(buffer)}")
        outputStream.write(buffer.copyOf(readBytes))
        //outputStream.flush()
        log(source, destination, buffer.copyOf(readBytes), NetOps.SEND)
    }
    outputStream.flush()
    destination.shutdownOutput()
}

private fun isBindRequest(data: ByteArray): Boolean = !(data.size < 8 || data[1] != 2.toByte())

private fun isConnect(data: ByteArray): Boolean = !(data.size < 8 || data [1] != 1.toByte())