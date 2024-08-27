package io.github.apwlq.updater.logs

import java.io.File
import java.time.format.DateTimeFormatter

class Logs {

    var isLogsSaved = false

    fun logs(message: String) {
        val level = LogLevel.INFO
        println("[$level] $message")
        saveLog(level, message)
    }

    fun logs(level: LogLevel = LogLevel.INFO, message: String) {
        println("[$level] $message")
        saveLog(level, message)
    }

    fun logs(level: LogLevel, message: String, throwable: Throwable) {
        println("[$level] $message")
        throwable.printStackTrace()
        saveLog(level, message + " " + throwable.message)
    }

    fun logs(level: LogLevel, throwable: Throwable) {
        throwable.printStackTrace()
        saveLog(level, throwable.message ?: "No message")
    }

    private fun saveLog(level: LogLevel, message: String) {
        if (!isLogsSaved) {
            return
        }
        val logDirectory = File("./logs")
        // 폴더가 존재하지 않으면 생성
        if (!logDirectory.exists()) {
            if (!logDirectory.mkdirs()) {
                println("Failed to create log directory.")
                return
            }
        }

        val date = java.time.LocalDate.now()
        val time = java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        try {
            // 현재 날자를 기준으로 파일명을 생성
            val logFile = File("./logs/${date}.log")
            logFile.appendText("[${date} ${time}] [$level] $message\n")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
