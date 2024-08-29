package io.github.apwlq.updater

import io.github.apwlq.updater.logs.LogLevel
import io.github.apwlq.updater.logs.Logs
import net.simplyrin.config.Config
import net.simplyrin.config.Configuration
import java.io.File
import java.io.IOException
import java.util.*
import java.util.jar.JarFile
import kotlin.system.exitProcess

val logger = Logs()
lateinit var config: Configuration // 초기화는 main 함수 내에서 진행

fun starter(isUpdateBool: Boolean? = null) {
    logger.isLogsSaved = config.getBoolean("save_logs")

    val versionCommand = config.getString("version_command")
    val nowVersion = Exec().exec(versionCommand).trim()
    val isUpdate = isUpdateBool ?: config.getBoolean("auto_update")

    if (!isUpdate) {
        logger.logs(LogLevel.INFO, "업데이트가 비활성화되어 있습니다.")
        return
    } else {
        if (nowVersion.isEmpty()) {
            logger.logs(LogLevel.ERROR, "실행 파일이 없습니다. 업데이트를 실행합니다.")
            Updater("v0.0.0").checkUpdate(config.getString("github_repo"), config.getString("download_file"))
        } else {
            logger.logs(LogLevel.INFO, "현재 버전: $nowVersion")
            val githubRepo = config.getString("github_repo")
            val downloadFile = config.getString("download_file")

            Updater(nowVersion).checkUpdate(githubRepo, downloadFile)
        }
    }

    val startCommand = config.getString("start_command")
    Exec().exec(startCommand)
}

fun main(args: Array<String>) {
    val jarFilePath = System.getProperty("java.class.path").split(":").firstOrNull { it.endsWith(".jar") }
    val version = jarFilePath?.let {
        try {
            JarFile(it).use { jar ->
                val manifest = jar.manifest
                manifest.mainAttributes.getValue("Implementation-Version") ?: "unknown"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "unknown"
        }
    } ?: "unknown"

    if (args.isNotEmpty() && args[0] == "--version") {
        println(version)
        return
    }

    val file = File("config.yml")

    if (!file.exists()) {
        try {
            file.createNewFile()
        } catch (e: IOException) {
            logger.logs(LogLevel.ERROR, "설정 파일을 생성할 수 없습니다.")
            stop(1)  // 설정 파일 생성 실패 시 프로그램을 종료
        }

        config = Config.getConfig(file)
        config.set("github_repo", "YOUR_GITHUB_REPO_HERE")
        config.set("auto_update", true)
        config.set("start_command", "java -jar runner.jar")
        config.set("version_command", "java -jar runner.jar --version")
        config.set("download_file", "YOUR_APP_NAME.jar")
        config.set("runner_file", "runner.jar")
        config.set("save_logs", false)
        Config.saveConfig(config, file)
    } else {
        config = Config.getConfig(file)
    }

    starter(true)
    stop()
}

fun stop(status: Int = 0) {
    logger.logs(LogLevel.INFO, "프로그램을 종료합니다.")
    exitProcess(status)
}
