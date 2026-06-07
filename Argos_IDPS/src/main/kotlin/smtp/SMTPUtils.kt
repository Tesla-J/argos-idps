package ao.argosidps.smtp

import ao.argosidps.colors.RED
import ao.argosidps.colors.RESET
import ao.argosidps.colors.YELLOW
import ao.argosidps.configurations.Configuration
import com.sun.jdi.connect.spi.TransportService
import org.simplejavamail.api.mailer.config.TransportStrategy
import org.simplejavamail.email.EmailBuilder
import org.simplejavamail.mailer.MailerBuilder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private fun useTemplate(anomaly:String, ip:String, filename:String, date: String): String = """
    <!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
</head>
<body style="margin:0; padding:0; background-color:#0d1117; font-family: 'Segoe UI', -apple-system, BlinkMacSystemFont, sans-serif;">
    <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#0d1117;">
        <tr>
            <td align="center" style="padding: 40px 20px;">
                <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="max-width:600px; width:100%;">
                    
                    <!-- Header -->
                    <tr>
                        <td style="background: linear-gradient(135deg, #ff4444 0%, #cc0000 100%); border-radius: 12px 12px 0 0; padding: 30px 40px; text-align: center;">
                            <table role="presentation" width="100%" cellpadding="0" cellspacing="0">
                                <tr>
                                    <td align="center" style="font-size: 48px; line-height: 1; margin-bottom: 10px;">🛡️</td>
                                </tr>
                                <tr>
                                    <td align="center" style="color: #ffffff; font-size: 28px; font-weight: 800; letter-spacing: 1px; text-transform: uppercase; padding-top: 10px;">
                                        ARGOS ALERT
                                    </td>
                                </tr>
                                <tr>
                                    <td align="center" style="color: #ffcccc; font-size: 16px; font-weight: 400; padding-top: 8px;">
                                        An anomaly was detected
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>

                    <!-- Body -->
                    <tr>
                        <td style="background-color: #161b22; border-left: 1px solid #30363d; border-right: 1px solid #30363d; padding: 40px;">
                            
                            <!-- Anomaly Type Badge -->
                            <table role="presentation" cellpadding="0" cellspacing="0" style="margin: 0 auto 30px auto;">
                                <tr>
                                    <td style="background-color: #21262d; border: 1px solid #ff4444; border-radius: 8px; padding: 12px 24px; text-align: center;">
                                        <span style="color: #ff6b6b; font-size: 14px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px;">
                                            ⚠ $anomaly
                                        </span>
                                    </td>
                                </tr>
                            </table>

                            <!-- Main Message -->
                            <table role="presentation" cellpadding="0" cellspacing="0" width="100%">
                                <tr>
                                    <td style="color: #c9d1d9; font-size: 16px; line-height: 1.7; padding-bottom: 8px;">
                                        A anomaly of type <strong style="color: #ff6b6b;">$anomaly</strong> was detected from host 
                                        <strong style="color: #58a6ff;">$ip</strong>. The host has been blocked.
                                    </td>
                                </tr>
                                <tr>
                                    <td style="color: #c9d1d9; font-size: 16px; line-height: 1.7; padding-top: 8px;">
                                        Check the logs in the file <strong style="color: #d2a8ff;">$filename</strong> for more details.
                                    </td>
                                </tr>
                            </table>

                            <!-- Spacer -->
                            <table role="presentation" cellpadding="0" cellspacing="0" width="100%">
                                <tr><td style="height: 24px;"></td></tr>
                            </table>

                            <!-- Divider -->
                            <table role="presentation" cellpadding="0" cellspacing="0" width="100%">
                                <tr>
                                    <td style="border-bottom: 1px solid #30363d;"></td>
                                </tr>
                            </table>

                            <!-- Spacer -->
                            <table role="presentation" cellpadding="0" cellspacing="0" width="100%">
                                <tr><td style="height: 24px;"></td></tr>
                            </table>

                            <!-- NFTables Note -->
                            <table role="presentation" cellpadding="0" cellspacing="0" width="100%">
                                <tr>
                                    <td style="background-color: #1c2128; border: 1px solid #30363d; border-left: 4px solid #58a6ff; border-radius: 6px; padding: 16px 20px;">
                                        <table role="presentation" cellpadding="0" cellspacing="0">
                                            <tr>
                                                <td style="color: #8b949e; font-size: 14px; line-height: 1.6;">
                                                    💡 If it was a mistake, check the <strong style="color: #58a6ff; font-family: 'Cascadia Code', 'Fira Code', monospace;">argos</strong> table in 
                                                    <strong style="color: #58a6ff; font-family: 'Cascadia Code', 'Fira Code', monospace;">nftables</strong>.
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>

                    <!-- Footer -->
                    <tr>
                        <td style="background-color: #0d1117; border: 1px solid #30363d; border-top: none; border-radius: 0 0 12px 12px; padding: 20px 40px; text-align: center;">
                            <table role="presentation" cellpadding="0" cellspacing="0" width="100%">
                                <tr>
                                    <td style="color: #484f58; font-size: 12px;">
                                        This is an automated message from <strong style="color: #8b949e;">Argos IDPS</strong> • 
                                        <span style="color: #484f58;">${date}</span>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="color: #484f58; font-size: 12px; padding-top: 4px;">
                                        Intrusion Detection & Prevention System
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>

                </table>
            </td>
        </tr>
    </table>
</body>
</html>
""".trimIndent()

private val mailer = MailerBuilder
    .withSMTPServer(
        Configuration.smtpHost,
        Configuration.smtpPort!!.toInt(),
        Configuration.smtpUser,
        Configuration.smtpPasswd
    )
    //.withTransportStrategy(TransportStrategy.SMTP)
    //.withTransportStrategy(TransportStrategy.SMTPS)
    .withTransportStrategy(TransportStrategy.SMTP_TLS)
    //.async()
    .buildMailer()
    //.withTransportStrategy(TransportStrategy.SMTP_OAUTH2)


public suspend fun sendAlert(
    anomaly: String,
    ip:String,
    filename: String,
    date: String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
){
    try {
        val email = EmailBuilder
            .startingBlank()
            .from("Argos IDPS", Configuration.smtpUser!!)
            .to(Configuration.adminEmail!!)
            .withSubject("⚠\uFE0F ARGOS ALERT ⚠\uFE0F - An Anomaly Was Detected")
            .withHTMLText(useTemplate(anomaly, ip, filename, date))
            .buildEmail()
        mailer.sendMail(email)
        println("$YELLOW[ARGOS] Alert send successfully!$RESET")
    }catch (e: Exception){
        System.err.println("$RED[ARGOS] Error while sending email ${e.message}!$RESET")
    }
}