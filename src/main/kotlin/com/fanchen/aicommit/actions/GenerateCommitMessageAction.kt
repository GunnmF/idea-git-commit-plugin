package com.fanchen.aicommit.actions

import com.fanchen.aicommit.services.AiGenerationService
import com.fanchen.aicommit.services.GitDiffCollector
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * Commit 输入框工具栏上的按钮：
 * - 空闲时显示 ✨，点击开始 AI 生成 Commit Message
 * - 生成中显示 ■ 停止按钮，点击中断请求
 */
class GenerateCommitMessageAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        if (AiGenerationService.getInstance(project).isRunning) {
            // 生成中：变成停止按钮
            e.presentation.text = "停止 AI 生成"
            e.presentation.description = "中断正在进行的 Commit Message 生成"
            e.presentation.icon = AllIcons.Actions.Suspend
            e.presentation.isEnabledAndVisible = true
        } else {
            // 空闲：显式从模板恢复外观（✨ 图标和文字），防止工具栏复用被改过的 presentation
            e.presentation.copyFrom(templatePresentation)
            e.presentation.isEnabledAndVisible = GitDiffCollector.hasRepository(project)
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val service = AiGenerationService.getInstance(project)

        if (service.isRunning) {
            service.cancel()
        } else {
            service.start(e.dataContext)
        }
    }
}
