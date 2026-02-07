package ao.argosidps.configurations

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.Scanner
import kotlin.system.exitProcess

private const val FILENAME = "argos.conf" //"/etc/argos/argos.conf"

// TODO should it be private for better encapsulation?
object DefaultConfiguration {
    /**
     * Valid configuration fields
     */
    object Fields {
        const val PROXY_TYPE = "proxy-type"
        const val PORT = "port"
        const val TIMEOUT = "timeout"
    }

    /**
     * Valid values for configuration values
     */
    object Values {
        const val SOCK4 = "SOCK4"
        const val DEFAULT_PORT = "3469"
        const val DEFAULT_TIMEOUT = "10000"
    }
}

/**
 * @return Returns a \n terminated bytearray from two strings
 * in the format of configuration file
 */
private fun toConfigFormat(property: String, value: String): ByteArray =
    "$property=$value\n".toByteArray()

private fun createDefaultConfiguration(configFile: File) {
    val output = FileOutputStream(configFile)
    output.write(toConfigFormat(DefaultConfiguration.Fields.PROXY_TYPE, DefaultConfiguration.Values.SOCK4))
    output.write(toConfigFormat(DefaultConfiguration.Fields.PORT, DefaultConfiguration.Values.DEFAULT_PORT))
    output.write(toConfigFormat(DefaultConfiguration.Fields.TIMEOUT, DefaultConfiguration.Values.DEFAULT_TIMEOUT))
    output.close()
}

private fun loadConfigurations(): HashMap<String, String> {
    val config = HashMap<String, String>()
    val configFile = File(FILENAME)
    val input: FileInputStream
    val scan: Scanner
    var readLine: List<String>

    if(!configFile.exists()) {
        !configFile.createNewFile()
        createDefaultConfiguration(configFile)
    }
    input = FileInputStream(configFile)
    scan = Scanner(input)
    while (scan.hasNextLine()){
        readLine = scan.nextLine().split('=')
        if (readLine.size != 2)
            throw IllegalArgumentException("Bad Property File")
        config.put(readLine[0], readLine[1])
    }
    input.close()
    return config
}

data object Configuration {
    private val params: HashMap<String, String> = try {
            loadConfigurations()
        }
        catch (e: IllegalArgumentException){
            println("Invalid configuration parameters in configuration files");
            exitProcess(1)
        }
        catch (e: IOException){
            println("Could not read configuration file");
            e.printStackTrace()
            exitProcess(1)
        }

    val proxyType = params[DefaultConfiguration.Fields.PROXY_TYPE]

    val port = try { params[DefaultConfiguration.Fields.PORT]!!.toInt() }
        catch (e: Exception){ DefaultConfiguration.Values.DEFAULT_PORT.toInt() }

    val timeout = try { params[DefaultConfiguration.Fields.TIMEOUT]!!.toInt() }
        catch (e: Exception) { DefaultConfiguration.Values.DEFAULT_TIMEOUT.toInt() }
}