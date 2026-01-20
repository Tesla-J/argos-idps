package ao.argosidps.configurations

/**
 * Valid configuration fields
 */
object ConfigurationFields {
    const val PROXY_TYPE = "proxy-type"
    const val PORT = "port"
    const val TIMEOUT = "timeout"
}

/**
 * Valid values for configuration values
 */
object ConfigurationValues {
    const val SOCK4 = "SOCK4"
    const val DEFAULT_PORT = "3469"
    const val DEFAULT_TIMEOUT = "5000"
}

fun loadConfigurations(): HashMap<String, String>{
    val config = HashMap<String, String>()

    // TODO load from file
    // TODO all values must be returned properly
    config.put(ConfigurationFields.PROXY_TYPE, ConfigurationValues.SOCK4)
    config.put(ConfigurationFields.PORT, ConfigurationValues.DEFAULT_PORT)
    config.put(ConfigurationFields.TIMEOUT, ConfigurationValues.DEFAULT_TIMEOUT)
    return config
}