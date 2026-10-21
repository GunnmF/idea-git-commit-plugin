package com.fanchen.aicommit.settings

import com.fanchen.aicommit.services.AiService
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.Messages
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.rows
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import javax.swing.DefaultComboBoxModel
import javax.swing.JButton
import javax.swing.JComponent

/**
 * Settings → Tools → AI Git Commit 设置页
 */
class AiCommitConfigurable : Configurable {

    private val state get() = AiCommitSettings.getInstance().state
    private var root: DialogPanel? = null

    /** 可编辑的模型下拉框：可手输，也可通过「获取模型列表」填充 */
    private lateinit var modelCombo: ComboBox<String>
    private lateinit var testButton: JButton
    private lateinit var fetchModelsButton: JButton

    /** 进行中的请求，非空表示正在执行，按钮变为取消入口 */
    private var testFuture: CompletableFuture<String>? = null
    private var fetchFuture: CompletableFuture<List<String>>? = null

    override fun getDisplayName(): String = "AI Git Commit"

    override fun createComponent(): JComponent {
        val s = state
        modelCombo = ComboBox(DefaultComboBoxModel(arrayOf(s.model))).apply { isEditable = true }

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
                    cell(modelCombo)
                        .bindItem({ s.model }, { s.model = it ?: "" })
                        .align(Align.FILL)
                        .comment("可手动输入，也可点右侧按钮从接口拉取")
                    fetchModelsButton = button("获取模型列表") { onFetchModelsClicked() }.component
                }
                row {
                    testButton = button("测试连接") { onTestClicked() }.component
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

    // ---------- 测试连接（带 loading + 可取消） ----------

    private fun onTestClicked() {
        testFuture?.let {
            it.cancel(true)
            return
        }

        root?.apply() // 先保存当前输入再测
        setBusy(testButton, busy = true, idleText = "测试连接", busyText = "取消测试")

        val f = AiService.testConnectionAsync()
        testFuture = f
        f.whenComplete { reply, error ->
            invokeLater {
                if (testFuture !== f) return@invokeLater // 已被更新的请求取代
                testFuture = null
                setBusy(testButton, busy = false, idleText = "测试连接", busyText = "取消测试")
                when {
                    error == null -> Messages.showInfoMessage("连接成功，模型返回：$reply", "AI Git Commit")
                    error.isCancel() -> balloon("已取消测试连接。", NotificationType.INFORMATION)
                    else -> Messages.showErrorDialog("连接失败：${error.rootMessage()}", "AI Git Commit")
                }
            }
        }
    }

    // ---------- 获取模型列表（带 loading + 可取消） ----------

    private fun onFetchModelsClicked() {
        fetchFuture?.let {
            it.cancel(true)
            return
        }

        root?.apply()
        setBusy(fetchModelsButton, busy = true, idleText = "获取模型列表", busyText = "取消获取")

        val f = AiService.fetchModelsAsync()
        fetchFuture = f
        f.whenComplete { models, error ->
            invokeLater {
                if (fetchFuture !== f) return@invokeLater
                fetchFuture = null
                setBusy(fetchModelsButton, busy = false, idleText = "获取模型列表", busyText = "取消获取")
                when {
                    error == null -> fillModels(models)
                    error.isCancel() -> balloon("已取消获取模型列表。", NotificationType.INFORMATION)
                    else -> Messages.showErrorDialog("获取失败：${error.rootMessage()}", "AI Git Commit")
                }
            }
        }
    }

    private fun fillModels(models: List<String>) {
        if (models.isEmpty()) {
            balloon("接口返回的模型列表为空。", NotificationType.WARNING)
            return
        }
        val current = modelCombo.item
        modelCombo.removeAllItems()
        models.forEach { modelCombo.addItem(it) }
        // 保留当前已选/已输入的值；为空时默认选第一个
        modelCombo.item = current.takeIf { it.isNotBlank() } ?: models.first()
        balloon("已获取 ${models.size} 个模型。", NotificationType.INFORMATION)
    }

    // ---------- 工具方法 ----------

    private fun setBusy(button: JButton, busy: Boolean, idleText: String, busyText: String) {
        button.text = if (busy) busyText else idleText
        button.icon = if (busy) AnimatedIcon.Default() else null
    }

    private fun invokeLater(action: () -> Unit) {
        ApplicationManager.getApplication().invokeLater(action)
    }

    private fun Throwable?.isCancel(): Boolean {
        var t: Throwable? = this
        while (t != null) {
            if (t is CancellationException) return true
            t = t.cause
        }
        return false
    }

    private fun Throwable.rootMessage(): String {
        var t: Throwable = this
        while (t.cause != null) t = t.cause!!
        return t.message ?: t.toString()
    }

    private fun balloon(content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("AI Git Commit")
            .createNotification(content, type)
            .notify(null)
    }

    // ---------- Configurable 生命周期 ----------

    override fun isModified(): Boolean = root?.isModified() ?: false

    override fun apply() {
        root?.apply()
    }

    override fun reset() {
        root?.reset()
    }

    override fun disposeUIResources() {
        // 关闭设置页时，中断还在进行的请求
        testFuture?.cancel(true)
        fetchFuture?.cancel(true)
        testFuture = null
        fetchFuture = null
        root = null
    }
}
