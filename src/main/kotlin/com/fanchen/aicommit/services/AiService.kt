package com.fanchen.aicommit.services

import com.fanchen.aicommit.settings.AiCommitSettings
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * 调用 OpenAI 兼容的 chat/completions 接口。
 * 兼容 OpenAI / DeepSeek / Moonshot / 通义 / Ollama 等同类服务。
 */
object AiService {

    private val gson = Gson()
    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build()

    fun generateCommitMessage(diff: String): String {
        val s = AiCommitSettings.getInstance().state
        return chat(
            model = s.model,
            system = PromptBuilder.buildSystemPrompt(s),
            user = PromptBuilder.buildUserPrompt(diff)
        )
    }

    /** 设置页「测试连接」用 */
    fun testConnection(): String =
        chat(model = AiCommitSettings.getInstance().state.model, system = null, user = "只回复：ok")

    private fun chat(model: String, system: String?, user: String): String {
        val s = AiCommitSettings.getInstance().state
        require(s.apiKey.isNotBlank()) { "请先在 Settings → Tools → AI Git Commit 中配置 API Key" }
        require(s.baseUrl.isNotBlank()) { "请先配置 Base URL" }

        val messages = mutableListOf<Map<String, String>>()
        if (!system.isNullOrBlank()) messages += mapOf("role" to "system", "content" to system)
        messages += mapOf("role" to "user", "content" to user)

        val payload = JsonObject().apply {
            addProperty("model", model)
            add("messages", gson.toJsonTree(messages))
            addProperty("temperature", 0.3)
            addProperty("stream", false)
        }

        val request = HttpRequest.newBuilder()
            .uri(URI.create(s.baseUrl.trimEnd('/') + "/chat/completions"))
            .timeout(Duration.ofSeconds(90))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer ${s.apiKey}")
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw RuntimeException("AI 请求失败 (HTTP ${response.statusCode()})：${response.body().take(500)}")
        }

        val content = gson.fromJson(response.body(), JsonObject::class.java)
            .getAsJsonArray("choices")[0].asJsonObject
            .getAsJsonObject("message")["content"].asString

        // 有些模型爱套一层 ``` 代码块，剥掉
        return content.trim()
            .removePrefix("```").removeSuffix("```")
            .trim()
    }
}
