package com.fanchen.aicommit.services

import com.intellij.openapi.project.Project
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager

/**
 * 通过 IDE 内置 Git4Idea 执行 git diff --cached，拿到 staged diff
 */
object GitDiffCollector {

    /** 截断阈值，避免超大 diff 打爆 token */
    private const val MAX_DIFF_CHARS = 30_000

    fun collectStagedDiff(project: Project): String {
        val repo = currentRepository(project)
            ?: throw IllegalStateException("当前项目没有找到 Git 仓库")

        val handler = GitLineHandler(project, repo.root, GitCommand.DIFF).apply {
            addParameters("--cached", "--no-color", "--no-ext-diff", "-U3")
        }

        val result = Git.getInstance().runCommand(handler)
        if (!result.success()) {
            throw RuntimeException("git diff --cached 执行失败：${result.errorOutputAsJoinedString}")
        }

        val diff = result.outputAsJoinedString
        return if (diff.length > MAX_DIFF_CHARS) {
            diff.take(MAX_DIFF_CHARS) + "\n\n... (diff 过长，已截断)"
        } else {
            diff
        }
    }

    fun hasRepository(project: Project): Boolean =
        GitRepositoryManager.getInstance(project).repositories.isNotEmpty()

    private fun currentRepository(project: Project): GitRepository? {
        val manager = GitRepositoryManager.getInstance(project)
        return manager.repositories.firstOrNull()
    }
}
