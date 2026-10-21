package com.fanchen.aicommit.actions

import com.fanchen.aicommit.services.AiService
import com.fanchen.aicommit.services.GitDiffCollector
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.vcs.VcsDataKeys

/**
 * Commit 输入框工具栏上的「AI 生成 Commit Message」按钮
 */
class GenerateCommitMessageAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabledAndVisible =
            project != null && GitDiffCollector.hasRepository(project)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        // 提前拿到 Commit 输入框上下文，后台线程结束后回填
        val dataContext: DataContext = e.dataContext

        ProgressManager.getInstance().run(
            object : Task.Backgroundable(project, "AI 正在生成 Commit Message…", true) {
                override fun run(indicator: ProgressIndicator) {
                    indicator.isIndeterminate = true

                    val diff = GitDiffCollector.collectStagedDiff(project)
                    if (diff.isBlank()) {
                        notify("没有 staged 变更，请先 git add 需要提交的文件。", NotificationType.WARNING)
                        return
                    }

                    val message = AiService.generateCommitMessage(diff)

                    ApplicationManager.getApplication().invokeLater {
                        val commitMessage = VcsDataKeys.COMMIT_MESSAGE_CONTROL.getData(dataContext)
                        if (commitMessage != null) {
                            commitMessage.setCommitMessage(message)
                        } else {
                            notify("生成成功但找不到 Commit 输入框，请打开 Commit 工具窗口后重试。", NotificationType.WARNING)
                        }
                    }
                }

                override fun onThrowable(error: Throwable) {
                    notify(error.message ?: "生成失败", NotificationType.ERROR)
                }
            }
        )
    }

    private fun notify(content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("AI Git Commit")
            .createNotification(content, type)
            .notify(null)
    }
}
