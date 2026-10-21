import java.util.Properties

plugins {
    kotlin("jvm") version "2.0.21"
    id("org.jetbrains.intellij.platform") version "2.2.1"
}

group = providers.gradleProperty("pluginGroup").get()

// 版本号 = gradle.properties 里的基础版本（major.minor）+ version.properties 里自增的 buildNumber
// 每次执行 buildPlugin 成功后 buildNumber 自动 +1，保证每次打包版本号都递增
val buildNumberFile = rootProject.file("version.properties")
val buildNumber: Int = run {
    val props = Properties()
    if (buildNumberFile.exists()) buildNumberFile.inputStream().use { props.load(it) }
    props.getProperty("buildNumber", "0").toIntOrNull() ?: 0
}
val baseVersion = providers.gradleProperty("pluginVersion").get().substringBeforeLast('.')
version = "$baseVersion.$buildNumber"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // 本地开发沙箱使用的 IDE
        create(providers.gradleProperty("platformType"), providers.gradleProperty("platformVersion"))
        // 依赖 IDE 内置 Git 插件（Git4Idea），用于读取仓库和 staged diff
        bundledPlugin("Git4Idea")

        instrumentationTools()
        pluginVerifier()
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = providers.gradleProperty("pluginUntilBuild")
                .map { it.ifBlank { null } }
        }
    }
}

kotlin {
    jvmToolchain(17)
}

tasks {
    wrapper {
        gradleVersion = "8.10.2"
    }

    // 打包成功后 buildNumber +1，下次构建自动用新版本号
    named("buildPlugin") {
        doLast {
            val props = Properties()
            props.setProperty("buildNumber", (buildNumber + 1).toString())
            buildNumberFile.outputStream().use { props.store(it, "auto-incremented by buildPlugin") }
            logger.lifecycle("buildNumber bumped to ${buildNumber + 1} (next build will be $baseVersion.${buildNumber + 1})")
        }
    }
}
