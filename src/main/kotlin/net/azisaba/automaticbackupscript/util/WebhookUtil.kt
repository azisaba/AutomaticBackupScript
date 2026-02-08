package net.azisaba.automaticbackupscript.util

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object WebhookUtil {
    private val client = HttpClient(CIO)

    suspend fun executeWebhook(
        url: String,
        content: String,
        username: String? = null,
    ) = client.post(url) {
        setBody(Json.encodeToString(MessageBody(content, username)))
        header("User-Agent", "https://github.com/azisaba/AutomaticBackupScript / 1.x")
        contentType(ContentType.parse("application/json"))
    }
}

@Serializable
data class MessageBody(
    val content: String,
    val username: String?,
)
