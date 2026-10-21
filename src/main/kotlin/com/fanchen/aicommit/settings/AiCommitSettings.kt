package com.fanchen.aicommit.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

/**
 * AI 对接配置，持久化到 aiGitCommit.xml
 */
@Service(Service.Level.APP)
@State(name = "AiGitCommitSettings", storages = [Storage("aiGitCommit.xml")])
class AiCommitSettings : PersistentStateComponent<AiCommitSettings.State> {

    class State {
        /** OpenAI 兼容接口地址，如 https://api.openai.com/v1 、https://api.deepseek.com/v1 */
        var baseUrl: String = "https://api.openai.com/v1"
        var apiKey: String = ""
        var model: String = "gpt-4o-mini"

        /** 生成语言：中文 / English */
        var language: String = "中文"

        /** 风格：简洁（单行 subject）/ 详细（带 body） */
        var style: String = "简洁"

        /** 是否遵循 Conventional Commits 规范 */
        var useConventionalCommits: Boolean = true

        /** 自定义附加要求，会拼接到 system prompt 末尾 */
        var customPrompt: String = ""
    }

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    companion object {
        fun getInstance(): AiCommitSettings =
            ApplicationManager.getApplication().getService(AiCommitSettings::class.java)
    }
}
