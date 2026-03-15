package ao.argosidps.configurations

import ao.argosidps.colors.RED
import ao.argosidps.colors.RESET
import ao.argosidps.colors.YELLOW
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
        const val NIF_ADDR = "nif_addr"
        const val NIF_NETMASK = "nif_netmask"
    }

    /**
     * Valid values for configuration values
     */
    object Values {
        const val NIF_ADDR_DEFAULT = "127.0.0.1"
        const val NIF_NETMASK_DEFAULT = "255.0.0.0"
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
    output.write(toConfigFormat(DefaultConfiguration.Fields.NIF_ADDR, DefaultConfiguration.Values.NIF_ADDR_DEFAULT))
    output.write(toConfigFormat(DefaultConfiguration.Fields.NIF_NETMASK, DefaultConfiguration.Values.NIF_NETMASK_DEFAULT))
    output.close()
}

private fun loadConfigurations(): HashMap<String, String> {
    val config = HashMap<String, String>()
    val configFile = File(FILENAME)
    val input: FileInputStream
    val scan: Scanner
    var readLine: List<String>

    if(!configFile.exists()) {
        println("${YELLOW}Configuration file not found, generating...${RESET}")
        !configFile.createNewFile()
        createDefaultConfiguration(configFile)
        println("${YELLOW}Default configurations generated.${RESET}")

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
            println("${RED}Invalid configuration parameters in configuration files${RESET}");
            exitProcess(1)
        }
        catch (e: IOException){
            println("${RED}Could not read configuration file${RESET}");
            e.printStackTrace()
            exitProcess(1)
        }

    val nifAddr = params[DefaultConfiguration.Fields.NIF_ADDR]
    val nifNetmask = params[DefaultConfiguration.Fields.NIF_NETMASK]
}