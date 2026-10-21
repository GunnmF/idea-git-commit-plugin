# IDEA Git Commit 插件

一个 IntelliJ IDEA 插件，用于对接 IDE 内置的 Git 功能，自动生成 Commit Message。

## 项目目标

- 与 IDEA 内置 Git 集成，读取当前变更（staged changes / diff）
- 一键生成规范的 Commit Message，自动填充到 Commit 输入框
- 支持 Conventional Commits 规范（`feat` / `fix` / `docs` / `refactor` 等）
- 支持自定义生成规则与风格（中英文、详细/简洁）

## 技术栈

- **语言**：Kotlin
- **平台**：IntelliJ Platform SDK
- **构建工具**：Gradle + IntelliJ Platform Gradle Plugin
- **兼容 IDE**：IntelliJ IDEA（同时兼容 WebStorm、GoLand、PyCharm 等 JetBrains IDE）

## 功能规划

- [x] 在 Commit 工具窗口添加入口按钮（`Vcs.CheckinMessageToolbar`）
- [x] 获取当前 Git 仓库的 staged diff（`git diff --cached`）
- [x] 根据 diff 生成 commit message 并填充
- [x] 支持 Conventional Commits 格式
- [x] 设置页：Base URL / API Key / Model / 语言 / 风格 / 附加要求
- [x] 快捷键支持（`Alt+G`）
- [x] 设置页「测试连接」
- [ ] 支持多仓库选择 / 按文件分别生成
- [ ] 支持未暂存变更（git diff）兜底

## 项目结构

```
src/main/kotlin/com/fanchen/aicommit/
├── actions/GenerateCommitMessageAction.kt   # Commit 输入框上的按钮，后台任务生成并回填
├── services/GitDiffCollector.kt             # 通过 Git4Idea 执行 git diff --cached
├── services/AiService.kt                    # 调用 OpenAI 兼容的 /chat/completions
├── services/PromptBuilder.kt                # 根据设置拼接 system/user prompt
└── settings/
    ├── AiCommitSettings.kt                  # 配置持久化（aiGitCommit.xml）
    └── AiCommitConfigurable.kt              # Settings → Tools → AI Git Commit 设置页
```

## 开发环境

- JDK 17+
- Gradle 8+
- IntelliJ IDEA Ultimate / Community

## 快速开始

```bash
# 启动沙箱 IDE（首次会下载 IntelliJ 平台，较大）
./gradlew runIde

# 打插件包（产物在 build/distributions/*.zip，可在任意 JetBrains IDE 中安装）
# 本地打包版本号自动递增（version.properties 里的 buildNumber 每次 +1）
./gradlew buildPlugin
```

## 发版（GitHub Actions）

推 `master` 分支自动触发 `.github/workflows/release.yml`：

1. 计算版本号：`gradle.properties` 的 major.minor + CI 构建号（如 `0.1.7`）
2. 该版本 Release 已存在且有 zip 则跳过（防重复发版，挂了会自动补齐）
3. 构建插件 zip → 自动建 tag（指向被构建的 commit）→ 上传 Release

升 minor/major 版本：改 `gradle.properties` 里的 `pluginVersion` 即可。

沙箱或已安装的 IDE 中：

1. `Settings → Tools → AI Git Commit` 配置 AI 服务（Base URL / API Key / Model），可点「测试连接」
2. `git add` 后打开 Commit 工具窗口，点击输入框工具栏上的 ✨ 图标（或 `Alt+G`）
3. 生成的 commit message 会自动填充到输入框

## 参考

- [IntelliJ Platform SDK 文档](https://plugins.jetbrains.com/docs/intellij/)
- [Git4Idea 源码](https://github.com/JetBrains/intellij-community/tree/master/plugins/git4idea)
