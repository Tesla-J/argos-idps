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

private const val FILENAME = "/etc/argos/argos.conf"

// TODO should it be private for better encapsulation?
object DefaultConfiguration {
    /**
     * Valid configuration fields
     */
    object Fields {
        const val NIF_ADDR = "nif_addr"
        const val NIF_NETMASK = "nif_netmask"
        const val ADMIN_EMAIL = "admin_email"
        const val SMTP_HOST = "smtp_host"
        const val SMTP_PORT = "25"
        const val SMTP_USERNAME = "smtp_username"
        const val SMTP_PASSWD = "smtp_passwd"
    }

    /**
     * Valid values for configuration values
     */
    object Values {
        const val NIF_ADDR_DEFAULT = "127.0.0.1"
        const val NIF_NETMASK_DEFAULT = "255.0.0.0"
        const val ADMIN_EMAIL_DEFAULT = "please@change.me"
        const val SMTP_HOST_DEFAULT = "smtp.domail.com"
        const val SMTP_PORT_DEFAULT = "25"
        const val SMTP_USERNAME_DEFAULT = "argos@domain.com"
        const val SMTP_PASSWRD_DEFAULT = "secretpassword"
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
    output.write(toConfigFormat(DefaultConfiguration.Fields.ADMIN_EMAIL, DefaultConfiguration.Values.ADMIN_EMAIL_DEFAULT))
    output.write(toConfigFormat(DefaultConfiguration.Fields.SMTP_HOST, DefaultConfiguration.Values.SMTP_HOST_DEFAULT))
    output.write(toConfigFormat(DefaultConfiguration.Fields.SMTP_PORT, DefaultConfiguration.Values.SMTP_PORT_DEFAULT))
    output.write(toConfigFormat(DefaultConfiguration.Fields.SMTP_USERNAME, DefaultConfiguration.Values.SMTP_USERNAME_DEFAULT))
    output.write(toConfigFormat(DefaultConfiguration.Fields.SMTP_PASSWD, DefaultConfiguration.Values.SMTP_PASSWRD_DEFAULT))
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
        configFile.parentFile.mkdirs()
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
    val adminEmail = params[DefaultConfiguration.Fields.ADMIN_EMAIL]
    val smtpHost = params[DefaultConfiguration.Fields.SMTP_HOST]
    val smtpPort = params[DefaultConfiguration.Fields.SMTP_PORT]
    val smtpUser = params[DefaultConfiguration.Fields.SMTP_USERNAME]
    val smtpPasswd = params[DefaultConfiguration.Fields.SMTP_PASSWD]
}