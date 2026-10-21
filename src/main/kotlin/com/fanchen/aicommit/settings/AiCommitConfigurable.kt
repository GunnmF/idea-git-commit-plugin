package com.fanchen.aicommit.settings

import com.fanchen.aicommit.services.AiService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.Messages
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

/**
 * Settings → Tools → AI Git Commit 设置页
 */
class AiCommitConfigurable : Configurable {

    private val state get() = AiCommitSettings.getInstance().state
    private var root: com.intellij.ui.dsl.builder.DialogPanel? = null

    override fun getDisplayName(): String = "AI Git Commit"

    override fun createComponent(): JComponent {
        val s = state
        val p = panel {
            group("AI 服务配置（OpenAI 兼容接口）") {
                row("Base URL:") {
                    textField()
                        .bindText(s::baseUrl)
                        .align(Align.FILL)
                        .comment("例：https://api.openai.com/v1 、https://api.deepseek.com/v1 、https://api.moonshot.cn/v1")
                }
                row("API Key:") {
                    passwordField()
                        .bindText(s::apiKey)
                        .align(Align.FILL)
                }
                row("Model:") {
                    textField()
                        .bindText(s::model)
                        .align(Align.FILL)
                        .comment("例：gpt-4o-mini、deepseek-chat、moonshot-v1-8k")
                }
                row {
                    button("测试连接") { testConnection() }
                }
            }
            group("生成规则") {
                row("语言:") {
                    comboBox(listOf("中文", "English"))
                        .bindItem({ s.language }, { s.language = it ?: "中文" })
                }
                row("风格:") {
                    comboBox(listOf("简洁", "详细"))
                        .bindItem({ s.style }, { s.style = it ?: "简洁" })
                        .comment("简洁 = 单行 subject；详细 = subject + 正文逐条说明")
                }
                row {
                    checkBox("遵循 Conventional Commits 规范（feat / fix / docs / refactor ...）")
                        .bindSelected(s::useConventionalCommits)
                }
                row("附加要求:") {
                    textArea()
                        .bindText(s::customPrompt)
                        .align(Align.FILL)
                        .rows(3)
                        .comment("可选，会追加到 system prompt 末尾，例如：scope 一律用模块名")
                }.resizableRow()
            }
        }
        root = p
        return p
    }

    private fun testConnection() {
        root?.apply() // 先保存当前输入再测
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val reply = AiService.testConnection()
                ApplicationManager.getApplication().invokeLater {
                    Messages.showInfoMessage("连接成功，模型返回：$reply", "AI Git Commit")
                }
            } catch (e: Exception) {
                ApplicationManager.getApplication().invokeLater {
                    Messages.showErrorDialog("连接失败：${e.message}", "AI Git Commit")
                }
            }
        }
    }

    override fun isModified(): Boolean = root?.isModified() ?: false

    override fun apply() {
        root?.apply()
    }

    override fun reset() {
        root?.reset()
    }

    override fun disposeUIResources() {
        root = null
    }
}
