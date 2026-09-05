package com.example.smarttravelplanner.data.remote.service

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

object GmailSmtpSender {

    private const val TAG = "GmailSmtpSender"
    private const val SMTP_HOST = "smtp.gmail.com"
    private const val SMTP_PORT = 465

    /**
     * Sends an HTML verification OTP email directly through Gmail's SSL SMTP server.
     */
    suspend fun sendOtpEmail(
        senderEmail: String,
        senderAppPassword: String,
        recipientEmail: String,
        otp: String,
        isPasswordReset: Boolean = false
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cleanPassword = senderAppPassword.replace(" ", "").trim()
            val cleanSender = senderEmail.trim()
            val cleanRecipient = recipientEmail.trim()

            if (cleanSender.isBlank() || cleanPassword.isBlank()) {
                throw IllegalStateException(
                    "Please configure your Gmail address and 16-character App Password in EmailConfig.kt or local.properties"
                )
            }

            Log.d(TAG, "Connecting to $SMTP_HOST:$SMTP_PORT via SSL...")
            val socketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
            val socket = socketFactory.createSocket() as SSLSocket
            socket.connect(InetSocketAddress(SMTP_HOST, SMTP_PORT), 15000)
            socket.soTimeout = 15000

            val reader = BufferedReader(InputStreamReader(socket.inputStream, Charsets.UTF_8))
            val writer = PrintWriter(OutputStreamWriter(socket.outputStream, Charsets.UTF_8), true)

            fun readResponse(): String {
                val sb = StringBuilder()
                var line = reader.readLine()
                    ?: throw IllegalStateException("Connection closed unexpectedly by Gmail SMTP server.")
                sb.append(line)
                // Multi-line SMTP responses have '-' at index 3 (e.g. 250-something)
                while (line.length >= 4 && line[3] == '-') {
                    line = reader.readLine() ?: break
                    sb.append("\n").append(line)
                }
                return sb.toString().trim()
            }

            fun sendCommand(cmd: String, hideLog: Boolean = false): String {
                if (hideLog) Log.d(TAG, "SMTP --> [REDACTED]") else Log.d(TAG, "SMTP --> $cmd")
                writer.print(cmd + "\r\n")
                writer.flush()
                val resp = readResponse()
                Log.d(TAG, "SMTP <-- $resp")
                return resp
            }

            // 1. Read initial greeting (e.g. 220 smtp.gmail.com ESMTP...)
            val banner = readResponse()
            Log.d(TAG, "SMTP <-- $banner")
            if (!banner.startsWith("220")) {
                throw IllegalStateException("Failed to connect to Gmail SMTP: $banner")
            }

            // 2. EHLO
            val ehloResp = sendCommand("EHLO localhost")
            if (!ehloResp.startsWith("250")) {
                throw IllegalStateException("EHLO greeting rejected: $ehloResp")
            }

            // 3. AUTH LOGIN
            val authResp = sendCommand("AUTH LOGIN")
            if (!authResp.startsWith("334")) {
                throw IllegalStateException("AUTH LOGIN rejected: $authResp")
            }

            // 4. Send Base64 Username
            val userB64 = Base64.encodeToString(cleanSender.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            val userResp = sendCommand(userB64, hideLog = true)
            if (!userResp.startsWith("334")) {
                throw IllegalStateException("Gmail username rejected: $userResp")
            }

            // 5. Send Base64 App Password
            val passB64 = Base64.encodeToString(cleanPassword.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            val passResp = sendCommand(passB64, hideLog = true)
            if (!passResp.startsWith("235")) {
                throw IllegalStateException(
                    "Gmail authentication failed. Please verify your 16-character Google App Password in EmailConfig.kt or local.properties."
                )
            }

            // 6. MAIL FROM
            val mailFromResp = sendCommand("MAIL FROM:<$cleanSender>")
            if (!mailFromResp.startsWith("250")) {
                throw IllegalStateException("MAIL FROM rejected: $mailFromResp")
            }

            // 7. RCPT TO
            val rcptToResp = sendCommand("RCPT TO:<$cleanRecipient>")
            if (!rcptToResp.startsWith("250")) {
                throw IllegalStateException("Recipient email rejected by Gmail: $rcptToResp")
            }

            // 8. DATA
            val dataResp = sendCommand("DATA")
            if (!dataResp.startsWith("354")) {
                throw IllegalStateException("DATA command rejected: $dataResp")
            }

            // 9. Send MIME message
            val subjectTitle = if (isPasswordReset) "$otp is your password reset code" else "$otp is your Smart Travel Planner code"
            val htmlContent = buildEmailHtml(otp, isPasswordReset)
            val message = buildString {
                append("From: \"Smart Travel Planner\" <$cleanSender>\r\n")
                append("To: <$cleanRecipient>\r\n")
                append("Subject: $subjectTitle\r\n")
                append("MIME-Version: 1.0\r\n")
                append("Content-Type: text/html; charset=UTF-8\r\n")
                append("Content-Transfer-Encoding: 8bit\r\n")
                append("\r\n")
                append(htmlContent)
                append("\r\n.")
            }

            val finishResp = sendCommand(message, hideLog = true)
            if (!finishResp.startsWith("250")) {
                throw IllegalStateException("Failed to send message content: $finishResp")
            }

            // 10. QUIT
            sendCommand("QUIT")
            socket.close()

            Log.i(TAG, "Successfully delivered OTP email to $cleanRecipient via Gmail SMTP!")
            Result.success(Unit)
        } catch (e: UnknownHostException) {
            Log.e(TAG, "Host resolution failed for $SMTP_HOST", e)
            Result.failure(IllegalStateException("Unable to reach email server. Please check your internet connection.", e))
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Socket connection/read timed out", e)
            Result.failure(IllegalStateException("Connection to email server timed out. Please check your network connection and try again.", e))
        } catch (e: ConnectException) {
            Log.e(TAG, "Connection refused or unreachable", e)
            Result.failure(IllegalStateException("Could not connect to email server. Please check your network and try again.", e))
        } catch (e: SSLException) {
            Log.e(TAG, "SSL handshake failed", e)
            Result.failure(IllegalStateException("Secure connection to email server failed. Please try again.", e))
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to send OTP email", e)
            Result.failure(e)
        }
    }

    private fun buildEmailHtml(otp: String, isPasswordReset: Boolean): String {
        val heading = if (isPasswordReset) "Reset Your Password" else "Verify Your Email Address"
        val description = if (isPasswordReset)
            "Use the verification code below to reset your Smart Travel Planner password. This code is valid for 5 minutes."
        else
            "Use the verification code below to complete creating your account. This code is valid for 5 minutes."

        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="utf-8"></head>
            <body style="margin:0;padding:0;background-color:#0D0D0D;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;">
              <table width="100%" border="0" cellspacing="0" cellpadding="0" style="background-color:#0D0D0D;padding:40px 10px;">
                <tr>
                  <td align="center">
                    <table width="100%" max-width="520" border="0" cellspacing="0" cellpadding="0" style="max-width:520px;background-color:#1A1A1A;border:1px solid #2C2C2C;border-radius:20px;padding:36px 28px;text-align:center;">
                      <tr>
                        <td align="center">
                          <div style="font-size:28px;font-weight:bold;color:#E8A020;letter-spacing:1px;margin-bottom:8px;">
                            Smart Travel Planner
                          </div>
                          <div style="font-size:14px;color:#AAAAAA;margin-bottom:28px;">
                            Every journey starts here.
                          </div>
                          <div style="font-size:16px;color:#FFFFFF;margin-bottom:12px;font-weight:600;">
                            $heading
                          </div>
                          <div style="font-size:14px;color:#8E8E8E;line-height:1.5;margin-bottom:24px;">
                            $description
                          </div>
                          <div style="background-color:#242424;border:1.5px solid #E8A020;border-radius:14px;padding:18px 24px;margin-bottom:24px;display:inline-block;">
                            <span style="font-size:34px;font-weight:bold;letter-spacing:8px;color:#FFFFFF;font-family:monospace;">
                              $otp
                            </span>
                          </div>
                          <div style="font-size:12px;color:#666666;line-height:1.5;">
                            If you did not request this code, you can safely ignore this email.
                          </div>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
        """.trimIndent()
    }
}
