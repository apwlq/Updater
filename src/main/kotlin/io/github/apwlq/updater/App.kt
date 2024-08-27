package io.github.apwlq.updater

import io.github.apwlq.updater.logs.LogLevel
import io.github.apwlq.updater.logs.Logs
import net.simplyrin.config.Config
import net.simplyrin.config.Configuration
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

val logger = Logs()

fun starter(args: Array<String>) {
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
    logger.logs(LogLevel.WARN,"명령어 실행: $versionCommand")
    val processBuilder = ProcessBuilder(versionCommand.split(" "))

    var nowVersion: String? = null

    try {
        // 프로세스를 시작합니다.
        val process = processBuilder.start()

        // 프로세스의 출력을 읽습니다.
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val output = StringBuilder()
        var line: String?

        while (reader.readLine().also { line = it } != null) {
            output.append(line)
        }

        // 프로세스가 종료될 때까지 대기합니다.
        val exitCode = process.destroy()
        logger.logs(LogLevel.WARN,"프로세스 종료. 종료 코드: $exitCode")

        // 만약 runner 파일이 없다면, 바로 업데이트 실행
        if(output.toString().isEmpty()) {
            logger.logs(LogLevel.ERROR, "runner 파일이 없습니다. 업데이트를 실행합니다.")
            Updater("v0.0.0").checkUpdate(config.getString("github_repo"), config.getString("download_file"))
            return
        }

        // 프로세스 출력값을 nowVersion으로 설정합니다.
        if(output.toString().isNotEmpty()) {
            nowVersion = output.toString()
        }
        logger.logs("현재 버전: ${nowVersion ?: "버전 정보를 가져올 수 없습니다."}")
    } catch (e: IOException) {
        logger.logs(LogLevel.ERROR, "프로세스를 시작할 수 없습니다.")
    } catch (e: InterruptedException) {
        logger.logs(LogLevel.ERROR, "프로세스가 종료되기 전에 대기가 중단되었습니다.")
    }

    // nowVersion이 null이 아닌 경우에만 checkUpdate 호출
    nowVersion?.let {
        val githubRepo = config.getString("github_repo")
        val downloadFile = config.getString("download_file")
        Updater(it).checkUpdate(githubRepo, downloadFile)
    } ?: run {
        logger.logs(LogLevel.ERROR, "버전 정보를 가져올 수 없어 업데이트를 확인할 수 없습니다.")
    }
}

fun main(args: Array<String>) {
    logger.logs("업데이터 시작중입니다... by apwlq")
    starter(args)
}
