package com.example.mobGPT

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.mobGPT.ui.theme.MobGPTJavaTheme
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MobGPTJavaTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    val uri = Uri.parse("https://toppng.com/uploads/preview/share-png-file-share-icon-free-download-1156313309811bbndeiii.png")
                    val request = DownloadManager.Request(uri).apply {
                        setTitle("Downloading File")
                        setDescription("Downloading your file...")
                        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "your-file-name.png")
                    }
                    val downloadId = downloadManager.enqueue(request)

                    // Create shell script content to echo file path
                    val scriptContent = """
                        #!/system/bin/sh
                    
                        # Echo path of the downloaded file
                        echo "$(contentResolver.query(Uri.parse("content://downloads/public_downloads"), null, "_id=$downloadId", null, null)?.use { cursor ->
                            cursor.moveToFirst()
                            cursor.getString(cursor.getColumnIndexOrThrow("_data"))
                        })"
                    """.trimIndent()

                    // Write script content to a file
                    val scriptFile = File(filesDir, "echo_file_path.sh")
                    scriptFile.writeText(scriptContent)
                    scriptFile.setExecutable(true)

                    // Execute shell script and capture output
                    val scriptPath = scriptFile.absolutePath
                    val commandOutput = ShellCommandExecutor.executeCommand("/system/bin/sh $scriptPath")
                    Greeting("Android", commandOutput)
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, commandOutput: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!\n$commandOutput",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MobGPTJavaTheme {
        Greeting("Android", "Example Output")
    }
}
