package io.github.apwlq.updater

import io.github.apwlq.updater.logs.LogLevel
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class Updater(private val currentVersion: String?) {

    private val client = OkHttpClient()

    fun checkUpdate(repoUrl: String, downloadFile: String) {
        logger.logs("업데이트 확인 중...")
        val latestVersion = fetchLatestVersion(repoUrl)
        if (latestVersion != null) {
            if (isUpdateAvailable(latestVersion)) {
                logger.logs("최신 버전: $latestVersion")
                update(repoUrl, downloadFile)
            } else {
                logger.logs("현재 최신 버전을 사용 중: $currentVersion")
            }
        } else {
            logger.logs(LogLevel.ERROR, "업데이트 확인에 실패하였습니다.")
        }
    }

    private fun fetchLatestVersion(repoUrl: String): String? {
        val (owner, repo) = parseRepoUrl(repoUrl) ?: return null

        val request = Request.Builder()
            .url("https://api.github.com/repos/$owner/$repo/releases/latest")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")

                val jsonData = response.body?.string() ?: return null
                val jsonObject = JSONObject(jsonData)
                jsonObject.getString("tag_name") // Assuming the tag_name represents the version
            }
        } catch (e: IOException) {
            logger.logs(LogLevel.ERROR, "업데이트 서버에 접근할 수 없거나 리미트가 초과되었습니다.")
            logger.logs(LogLevel.ERROR, "$e")
            null
        } catch (e: Exception) {
            logger.logs(LogLevel.WARN, "업데이트 서버에 접근할 수 없습니다.")
            logger.logs(LogLevel.WARN, "$e")
            null
        }
    }

    private fun parseRepoUrl(repoUrl: String): Pair<String, String>? {
        val cleanUrl = repoUrl.removeSuffix(".git")
        val regex = Regex("https://github.com/([^/]+)/([^/]+)")
        val matchResult = regex.find(cleanUrl)
        return matchResult?.destructured?.toList()?.let { (owner, repo) -> owner to repo }
    }

    private fun isUpdateAvailable(latestVersion: String): Boolean {
        return currentVersion != latestVersion
    }

    private fun update(repoUrl: String, downloadFile: String) {
        logger.logs("업데이트 중...")
        val (owner, repo) = parseRepoUrl(repoUrl) ?: return updateFailed()

        val request = Request.Builder()
            .url("https://api.github.com/repos/$owner/$repo/releases/latest")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")

                val jsonData = response.body?.string() ?: return updateFailed()
                val jsonObject = JSONObject(jsonData)
                val assets: JSONArray = jsonObject.getJSONArray("assets")

                var downloadUrl: String? = null

                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    if (asset.getString("name") == downloadFile) {
                        downloadUrl = asset.getString("browser_download_url")
                        break
                    }
                }

                if (downloadUrl != null) {
                    downloadFile(downloadUrl, downloadFile)
                    updateSuccess(downloadFile)
                } else {
                    logger.logs(LogLevel.ERROR, "($downloadFile) 파일을 찾을 수 없습니다.")
                    updateFailed()
                }
            }
        } catch (e: IOException) {
            logger.logs(LogLevel.ERROR, "업데이트 중 오류: ${e.message}")
            updateFailed()
        } catch (e: Exception) {
            logger.logs(LogLevel.ERROR, "예기치 않은 오류 발생: ${e.message}")
            updateFailed()
        }
    }

    private fun downloadFile(url: String, outputFileName: String) {
        val request = Request.Builder().url(url).build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("파일을 다운로드하지 못했습니다: $response")

                val file = File(outputFileName)
                response.body?.byteStream()?.use { inputStream ->
                    FileOutputStream(file).use { outputStream ->
                        copyStream(inputStream, outputStream)
                    }
                }
            }
        } catch (e: IOException) {
            logger.logs(LogLevel.ERROR, "파일 다운로드 실패: ${e.message}")
        } catch (e: Exception) {
            logger.logs(LogLevel.ERROR, "예기치 않은 오류 발생: ${e.message}")
        }
    }

    private fun copyStream(input: InputStream, output: FileOutputStream) {
        val buffer = ByteArray(4 * 1024) // 4KB buffer
        var bytesRead: Int
        while (input.read(buffer).also { bytesRead = it } != -1) {
            output.write(buffer, 0, bytesRead)
        }
        output.flush()
    }

    private fun updateSuccess(downloadFile: String) {
        logger.logs(LogLevel.INFO, "업데이트 완료!")
        val oldFile = File(downloadFile)
        val newFile = File("runner.jar")

        val oldFilePath = oldFile.absolutePath
        val newFilePath = newFile.absolutePath

        logger.logs(LogLevel.INFO, "변경 전 파일 경로: $oldFilePath")
        logger.logs(LogLevel.INFO, "변경 후 파일 경로: $newFilePath")

        if (oldFile.exists()) {
            if (newFile.exists()) {
                logger.logs(LogLevel.WARN, "대상 파일이 이미 존재합니다. 기존 파일을 삭제합니다.")
                if (!newFile.delete()) {
                    logger.logs(LogLevel.ERROR, "대상 파일 삭제에 실패했습니다.")
                    return
                }
            }

            val success = oldFile.renameTo(newFile)
            if (success) {
                logger.logs(LogLevel.INFO, "업데이트 완료! 파일 이름이 ${newFile.name}으로 변경되었습니다.")
            } else {
                logger.logs(LogLevel.ERROR, "파일 이름 변경에 실패했습니다.")
            }
        } else {
            logger.logs(LogLevel.WARN, "변경할 파일이 존재하지 않습니다.")
        }
    }

    private fun updateFailed() {
        logger.logs(LogLevel.WARN, "업데이트 실패!")
    }
}
