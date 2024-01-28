package com.example.mobGPT

import java.io.BufferedReader
import java.io.InputStreamReader

object ShellCommandExecutor {
    fun executeCommand(command: String): String {
        val output = StringBuilder()

        try {
            val process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }

            var errorLine: String?
            while (errorReader.readLine().also { errorLine = it } != null) {
                output.append(errorLine).append("\n")
            }

            reader.close()
            errorReader.close()
            process.waitFor()

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return output.toString()
    }
}
