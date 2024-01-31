package com.example.mobGPT
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.mobGPT.ui.theme.MobGPTJavaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MobGPTJavaTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val llamaFile = File(filesDir, "llama")
                    llamaFile.setExecutable(true)
                    val tinyLlamaFile = File(filesDir, "tinyllama.gguf")
                    tinyLlamaFile.setExecutable(true)
                    DownloadFilesContent(
                        listOf(
                            DownloadInfo(
                                "https://github.com/LordMilan/llama.cpp-android/raw/master/main",
                                llamaFile,
                                "llama.cpp"
                            ),
                            DownloadInfo(
                                "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v0.3-GGUF/resolve/main/tinyllama-1.1b-chat-v0.3.Q8_0.gguf?download=true",
                                tinyLlamaFile,
                                "tinyllama.gguf"
                            )
                        )
                    )
                }
            }
        }
    }
}

data class DownloadInfo(val url: String, val file: File, val fileName: String)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DownloadFilesContent(downloadInfos: List<DownloadInfo>) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.padding(16.dp)) {
            downloadInfos.forEach { downloadInfo ->
                DownloadFileContent(downloadInfo)
            }

            // User input field placed here
            var userInput by remember { mutableStateOf("") }
            var scriptOutput by remember { mutableStateOf("") } // Mutable state variable for script output
            val keyboardController = LocalSoftwareKeyboardController.current

            TextField(
                value = userInput,
                onValueChange = { userInput = it },
                label = { Text("Enter your message") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 16.dp),
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        scriptOutput = executeScriptWithUserInput(downloadInfos.first().file, downloadInfos[1].file, userInput) // Execute script with user input
                        userInput = ""
                        keyboardController?.hide()
                    }
                )
            )

            // Text to display script output
            Text(text = scriptOutput)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DownloadFileContent(downloadInfo: DownloadInfo) {
    var downloadProgress by remember { mutableStateOf(0f) }
    var scriptOutput by remember { mutableStateOf("") } // New mutable state variable for script output
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(true) {
        downloadFile(downloadInfo.url, downloadInfo.file) { progress ->
            downloadProgress = progress
        }
    }

    Column {
        Text(text = "Download files:")
        Text(text = downloadInfo.fileName)
        LinearProgressIndicator(progress = downloadProgress)
        Text(text = "Progress: ${(downloadProgress * 100).toInt()}%")


    }
}

private suspend fun downloadFile(fileUrl: String, file: File, onProgress: (Float) -> Unit) {
    withContext(Dispatchers.IO) {
        var output: FileOutputStream? = null
        var connection: HttpURLConnection? = null
        try {
            val url = URL(fileUrl)
            connection = url.openConnection() as HttpURLConnection
            val fileSize = connection.contentLength.toFloat()
            val input = connection.inputStream
            output = FileOutputStream(file)

            val buffer = ByteArray(1024)
            var totalBytesRead = 0L
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                val progress = totalBytesRead.toFloat() / fileSize
                onProgress(progress)
            }

            output.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            output?.close()
            connection?.disconnect()

            // Grant execute permission to the downloaded file
            file.setExecutable(true)
        }
    }
}
private fun executeScriptWithUserInput(file: File, model: File, userInput: String): String {
    // Your shell script content here...
    val scriptContent = """
        echo "Question: $userInput"
        ./${file.absolutePath} --log-disable --seed -1 --threads 4 --n_predict 60 --model ./${model.absolutePath} --top_k 90 --top_p 0.9 --temp 0.1 --repeat_last_n 64 --repeat_penalty 1.3 -p "$userInput"
    """.trimIndent()

    // Write script content to a file
    val scriptFile = File(file.parentFile, "execute_script_with_input.sh")
    scriptFile.writeText(scriptContent)
    scriptFile.setExecutable(true)

    // Execute shell script and capture output
    val scriptPath = scriptFile.absolutePath
    val commandOutput = ShellCommandExecutor.executeCommand("/system/bin/sh $scriptPath")
    return commandOutput
}

