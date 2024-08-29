package io.github.apwlq.updater

import io.github.apwlq.updater.logs.LogLevel
import java.io.BufferedReader
import java.io.IOException

class Exec {

    fun exec(command: String): String {
        logger.logs(LogLevel.WARN, "명령어 실행: $command")

        val processBuilder = ProcessBuilder(command.split(" "))

        return try {
            val process = processBuilder.start()

            // 프로세스의 표준 출력 스트림 읽기
            val output = process.inputStream.bufferedReader().use(BufferedReader::readText)

            // 프로세스의 표준 에러 스트림 읽기
            val errorOutput = process.errorStream.bufferedReader().use(BufferedReader::readText)

            val exitCode = process.waitFor()
            logger.logs(LogLevel.WARN, "프로세스 종료. 종료 코드: $exitCode")

            if (exitCode != 0) {
                logger.logs(LogLevel.ERROR, "프로세스가 비정상적으로 종료되었습니다. 오류 메시지: $errorOutput")
            }

            output
        } catch (e: IOException) {
            logger.logs(LogLevel.ERROR, "프로세스를 시작할 수 없습니다: ${e.message}")
            ""
        } catch (e: InterruptedException) {
            logger.logs(LogLevel.ERROR, "프로세스가 종료되기 전에 대기가 중단되었습니다: ${e.message}")
            Thread.currentThread().interrupt() // 인터럽트 상태를 복원합니다.
            ""
        }
    }
}
