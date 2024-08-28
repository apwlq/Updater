package io.github.apwlq.updater

import io.github.apwlq.updater.logs.LogLevel
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class Exec {
    fun exec(command: String): String {
        logger.logs(LogLevel.WARN,"명령어 실행: $command")
        val processBuilder = ProcessBuilder(command.split(" "))

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
            val exitCode = process.waitFor()
            logger.logs(LogLevel.WARN,"프로세스 종료. 종료 코드: $exitCode")
            return output.toString()
        } catch (e: IOException) {
            logger.logs(LogLevel.ERROR, "프로세스를 시작할 수 없습니다.")
        } catch (e: InterruptedException) {
            logger.logs(LogLevel.ERROR, "프로세스가 종료되기 전에 대기가 중단되었습니다.")
        }
        return null.toString()
    }
}