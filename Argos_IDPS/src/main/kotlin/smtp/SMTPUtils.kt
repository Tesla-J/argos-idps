package ao.argosidps.smtp

import ao.argosidps.configurations.Configuration
import at.quickme.kotlinmailer.delivery.mailerBuilder
import at.quickme.kotlinmailer.delivery.send
import at.quickme.kotlinmailer.email.emailBuilder
import jakarta.mail.internet.InternetAddress

public val mailer = mailerBuilder(
    host = Configuration.smtpHost!!,
    port = Configuration.smtpPort!!.toInt(),
    username = Configuration.smtpUser,
    password = Configuration.smtpPasswd
)

public val sender = InternetAddress(
    "Sebastião Carvalho <${Configuration.smtpUser}>"
)

public suspend fun sendAlert(name: String, addr:String){
    emailBuilder {
        from (sender)
        to(Configuration.adminEmail!!)
        withSubject("Argos - Anomalia Detectada!")
        withPlainText("Please check the file ")
    }.send(mailer)
}