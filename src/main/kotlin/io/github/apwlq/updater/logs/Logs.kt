package io.github.apwlq.updater.logs

import java.io.File
import java.io.OutputStream
import java.io.PrintStream
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object Logs {
    var isLogsSaved = false

    fun initRedirect() {
        System.setOut(PrintStream(LoggerStream(LogLevel.INFO), true))
        System.setErr(PrintStream(LoggerStream(LogLevel.ERROR), true))
    }

    fun logs(level: LogLevel = LogLevel.INFO, message: String) {
        val date = LocalDate.now()
        val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        val stackTrace = Thread.currentThread().stackTrace
        val caller = stackTrace.firstOrNull { it.className != Thread::class.java.name && !it.className.contains("LoggerStream") } ?: return

        val formatted = "[${date} ${time}] [$level] ${caller.className}:${caller.lineNumber} $message"
        kotlin.io.println(formatted) // 실제 콘솔 출력
        saveLog(level, message, caller.className, caller.lineNumber)
    }

    fun info(message: String) = logs(LogLevel.INFO, message)
    fun warn(message: String) = logs(LogLevel.WARN, message)
    fun error(message: String) = logs(LogLevel.ERROR, message)
    fun debug(message: String) = logs(LogLevel.DEBUG, message)

    private fun saveLog(level: LogLevel, message: String, className: String? = null, lineNumber: Int? = null) {
        if (!isLogsSaved) return

        val logDirectory = File("./logs")
        if (!logDirectory.exists() && !logDirectory.mkdirs()) {
            kotlin.io.println("Failed to create log directory.")
            return
        }

        val date = LocalDate.now()
        val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))

        try {
            val logFile = File("./logs/${date}.log")
            logFile.appendText("[${date} ${time}] [$level] ($className:$lineNumber) $message\n")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class LoggerStream(private val level: LogLevel) : OutputStream() {
    private val buffer = StringBuilder()

    override fun write(b: Int) {
        if (b.toChar() == '\n') {
            if (buffer.isNotEmpty()) {
                Logs.logs(level, buffer.toString())
                buffer.clear()
            }
        } else {
            buffer.append(b.toChar())
        }
    }
}