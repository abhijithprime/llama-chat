package com.prime.llamachat

import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.text.format.Formatter
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Button
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import com.prime.llamachat.database.ObjectBox
import com.prime.llamachat.ui.theme.LLAMACHATTheme
import java.io.File
import kotlin.getValue
import kotlin.text.clear

class MainActivity(
    activityManager: ActivityManager? = null,
    clipboardManager: ClipboardManager? = null,
) : ComponentActivity() {
    // Current Class name for logging and debugging purposes
    private val tag: String? = this::class.simpleName

    private val activityManager by lazy { activityManager ?: getSystemService<ActivityManager>()!! }
    private val clipboardManager by lazy {
        clipboardManager ?: getSystemService<ClipboardManager>()!!
    }

    private val viewModel: MainViewModel by viewModels()

    // Get a MemoryInfo object for the device's current memory status.
    private fun availableMemory(): ActivityManager.MemoryInfo {
        return ActivityManager.MemoryInfo().also { memoryInfo ->
            activityManager.getMemoryInfo(memoryInfo)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ObjectBox.init(this)

        StrictMode.setVmPolicy(
            VmPolicy.Builder(StrictMode.getVmPolicy())
                .detectLeakedClosableObjects()
                .build()
        )

        val free = Formatter.formatFileSize(this, availableMemory().availMem)
        val total = Formatter.formatFileSize(this, availableMemory().totalMem)

        viewModel.log("Current memory: $free / $total")
        viewModel.log("Downloads directory: ${getExternalFilesDir(null)}")

        val extFilesDir = getExternalFilesDir(null)
        val modelPath = File(extFilesDir, "llama-160m-chat-v1.q8_0.gguf").absolutePath

        enableEdgeToEdge()
        setContent {
            LLAMACHATTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.surfaceBright
                ) { innerPadding ->
                    ChatScreen(
                        viewModel,
                        clipboardManager,
                        modelPath,
                        Modifier.padding(innerPadding).background(color = MaterialTheme.colorScheme.surfaceBright)
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}

@Preview
@Composable
fun ChatScreenAltPreview() {
    LLAMACHATTheme {
        ChatScreenAlt()
    }
}

@Composable
fun ChatScreenAlt() {

}

@Preview
@Composable
fun ChatBoxPreview() {
    LLAMACHATTheme {
        ChatBox(
            modifier = Modifier.background(color = MaterialTheme.colorScheme.surfaceBright)
        )
    }
}

@Composable
fun ChatBox(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.medium.copy(
                    bottomEnd = CornerSize(0.dp),
                    topStart = CornerSize(10.dp),
                    topEnd = CornerSize(10.dp),
                    bottomStart = CornerSize(10.dp)
                )
            )
            .padding(16.dp).background(
                color = Color(0xFFD35454) // Light gray background
            )

    ) {
        Text(
            text = "Chat Box Placeholder",
            style = MaterialTheme.typography.bodyLarge.copy(color = LocalContentColor.current)
        )
    }
}

@Preview
@Composable
fun ChatScreenPreview() {
    LLAMACHATTheme {
        ChatScreen(
            viewModel = MainViewModel(),
            clipboard = LocalClipboardManager.current as ClipboardManager,
            modelPath = "path/to/model",
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun ChatScreen(
    viewModel: MainViewModel,
    clipboard: ClipboardManager,
    modelPath: String,
    modifier: Modifier,
) {
    Column(modifier = modifier) {
        val scrollState = rememberLazyListState()

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(state = scrollState) {
                items(viewModel.messages) {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyLarge.copy(color = LocalContentColor.current),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        OutlinedTextField(
            value = viewModel.message,
            onValueChange = { viewModel.updateMessage(it) },
            label = { Text("Message") },
        )
        Row {
            Button({ viewModel.send() }) { Text("Send") }
            Button({ viewModel.benchmark(8, 4, 1) }) { Text("Bench") }
            Button({ viewModel.load(modelPath) }) { Text("Load") }
            Button({
                viewModel.messages.joinToString("\n").let {
                    clipboard.setPrimaryClip(ClipData.newPlainText("", it))
                }
            }) { Text("Copy") }
        }
    }
}