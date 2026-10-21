package com.fanchen.aicommit.services

import com.fanchen.aicommit.settings.AiCommitSettings

/**
 * 根据设置拼接 system / user prompt
 */
object PromptBuilder {

    fun buildSystemPrompt(s: AiCommitSettings.State): String = buildString {
        append("你是一名资深工程师，负责根据 git diff 生成高质量的 commit message。")

        if (s.useConventionalCommits) {
            append(
                " 严格遵循 Conventional Commits 规范，格式为 <type>(<scope>): <subject>，" +
                    "type 必须从 feat、fix、docs、style、refactor、perf、test、build、ci、chore、revert 中选取，" +
                    "scope 可省略。"
            )
        }
        append(" 使用").append(s.language).append("输出。")

        if (s.style == "详细") {
            append(" 风格：第一行为 subject，空一行后写 body，用列表逐条说明主要改动。")
        } else {
            append(" 风格：只输出一行简洁的 subject。")
        }

        append(" 只输出 commit message 本身，不要输出任何解释、前后缀或 Markdown 代码块。")

        if (s.customPrompt.isNotBlank()) {
            append(" 额外要求：").append(s.customPrompt.trim())
        }
    }

    fun buildUserPrompt(diff: String): String =
        "以下是本次提交的 staged diff，请生成 commit message：\n\n$diff"
}
