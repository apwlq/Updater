package io.github.apwlq.updater

import io.github.apwlq.updater.logs.LogLevel
import io.github.apwlq.updater.logs.Logs
import net.simplyrin.config.Config
import net.simplyrin.config.Configuration
import java.io.File
import java.io.IOException
import kotlin.system.exitProcess

val logger = Logs()

fun starter() {
    logger.logs(LogLevel.INFO, "시작 중...")
    val file = File("config.yml")
    var config: Configuration

    if (!file.exists()) {
        try {
            file.createNewFile()
        } catch (e: IOException) {
            logger.logs(LogLevel.ERROR, "설정 파일을 생성할 수 없습니다.")
        }

        config = Config.getConfig(file)
        config.set("github_repo", "YOUR_GITHUB_REPO_HERE")
        config.set("auto_update", true)
        config.set("start_command", "java -jar runner.jar")
        config.set("version_command", "java -jar runner.jar --version")
        config.set("download_file", "runner.jar")
        config.set("save_logs", false)
        Config.saveConfig(config, file)
    }


    config = Config.getConfig(file)
    logger.isLogsSaved = config.getBoolean("save_logs")


    val versionCommand = config.getString("version_command")
    var nowVersion: String? = null

    Exec().exec(versionCommand).let {
        nowVersion = it
        logger.logs("현재 버전: $nowVersion")
        // 만약 runner 파일이 없다면, 바로 업데이트 실행
        if(nowVersion!!.isEmpty()) {
            logger.logs(LogLevel.ERROR, "runner 파일이 없습니다. 업데이트를 실행합니다.")
            Updater("v0.0.0").checkUpdate(config.getString("github_repo"), config.getString("download_file"))
            return
        }
    }

    // nowVersion이 null이 아닌 경우에만 checkUpdate 호출
    nowVersion?.let {
        val githubRepo = config.getString("github_repo")
        val downloadFile = config.getString("download_file")
        Updater(it).checkUpdate(githubRepo, downloadFile)
    } ?: run {
        logger.logs(LogLevel.ERROR, "버전 정보를 가져올 수 없어 업데이트를 확인할 수 없습니다.")
    }

    val startCommand = config.getString("start_command")
    Exec().exec(startCommand)
}

fun main(args: Array<String>) {
    if(args.isNotEmpty() && args[0] == "--version") {
        println("v1.0.0-a")
        stop()
    }
    logger.logs("프로그램 시작중입니다... by apwlq")
    starter()
}
fun stop(status: Int = 0) {
    logger.logs(LogLevel.INFO, "프로그램을 종료합니다.")
    exitProcess(status)
}
