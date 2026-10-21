package com.fanchen.aicommit.services

import com.intellij.ide.ActivityTracker
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsDataKeys
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture

/**
 * 管理当前项目正在进行的 AI 生成任务：
 * - [isRunning] 供 Action 决定显示 ✨ 还是停止按钮
 * - [cancel] 中断底层 HTTP 请求
 */
@Service(Service.Level.PROJECT)
class AiGenerationService(private val project: Project) {

    @Volatile
    private var future: CompletableFuture<String>? = null

    val isRunning: Boolean get() = future != null

    fun start(dataContext: DataContext) {
        if (isRunning) return

        ProgressManager.getInstance().run(
            object : Task.Backgroundable(project, "AI 正在生成 Commit Message…", true) {
                override fun run(indicator: ProgressIndicator) {
                    indicator.isIndeterminate = true
                    try {
                        val diff = GitDiffCollector.collectStagedDiff(project)
                        if (diff.isBlank()) {
                            notify("没有 staged 变更，请先 git add 需要提交的文件。", NotificationType.WARNING)
                            return
                        }

                        val f = AiService.generateCommitMessageAsync(diff)
                        future = f
                        refreshActions() // 状态变更：✨ → 停止

                        val message = f.join()

                        ApplicationManager.getApplication().invokeLater {
                            val commitMessage = VcsDataKeys.COMMIT_MESSAGE_CONTROL.getData(dataContext)
                            if (commitMessage != null) {
                                commitMessage.setCommitMessage(message)
                            } else {
                                notify("生成成功但找不到 Commit 输入框，请打开 Commit 工具窗口后重试。", NotificationType.WARNING)
                            }
                        }
                    } catch (e: CancellationException) {
                        notify("已停止生成。", NotificationType.INFORMATION)
                    } catch (e: Exception) {
                        notify(e.cause?.message ?: e.message ?: "生成失败", NotificationType.ERROR)
                    } finally {
                        future = null
                        refreshActions() // 状态变更：停止 → ✨
                    }
                }

                override fun onCancel() {
                    // 状态栏进度条上的取消入口也走同一逻辑
                    this@AiGenerationService.cancel()
                }
            }
        )
    }

    fun cancel() {
        future?.cancel(true)
    }

    /** 强制刷新 Action 工具栏，让 update() 重新执行以切换图标 */
    private fun refreshActions() {
        ApplicationManager.getApplication().invokeLater {
            ActivityTracker.getInstance().inc()
        }
    }

    private fun notify(content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Lazy Commit")
            .createNotification(content, type)
            .notify(project)
    }

    companion object {
        fun getInstance(project: Project): AiGenerationService =
            project.getService(AiGenerationService::class.java)
    }
}
