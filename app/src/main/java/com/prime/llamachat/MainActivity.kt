package com.prime.llamachat

import android.app.ActivityManager
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import com.prime.llamachat.database.ObjectBox
import com.prime.llamachat.ui.theme.LLAMACHATTheme
import kotlinx.coroutines.launch
import java.io.File

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

    private lateinit var embedder: BertEmbedder

    // Get a MemoryInfo object for the device's current memory status.
    private fun availableMemory(): ActivityManager.MemoryInfo {
        return ActivityManager.MemoryInfo().also { memoryInfo ->
            activityManager.getMemoryInfo(memoryInfo)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ObjectBox.init(this)

        embedder = BertEmbedder(this)

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
//        val modelName = "llama-160m-chat-v1.q8_0.gguf" // 160M Model
        val modelName = "Llama-3.2-1B-Instruct-Q4_K_S.gguf" // 1B Model
//        val modelName = "tinymistral-248m-alpaca.q4_k_m.gguf"
        val modelPath = File(extFilesDir, modelName).absolutePath
        val embeddingPath = File(extFilesDir, "embeddings.json").absolutePath

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
                        Modifier
                            .padding(innerPadding)
                            .background(color = MaterialTheme.colorScheme.surfaceBright),
                        embedder,
                        embeddingPath
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
            .padding(16.dp)
            .background(
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
            modifier = Modifier.fillMaxSize(),
            embedder = BertEmbedder(LocalContext.current),
            embeddingPath = ""
        )
    }
}

@Composable
fun ChatMessageBubble(message: String, isUserMessage: Boolean) {
    val bubbleShape: Shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isUserMessage) 16.dp else 0.dp,
        bottomEnd = if (isUserMessage) 0.dp else 16.dp
    )

    val backgroundColor =
        if (isUserMessage) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val textColor =
        if (isUserMessage) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = if (isUserMessage) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .background(backgroundColor, shape = bubbleShape)
                .padding(12.dp)
        ) {
            Text(
                text = message,
                color = textColor,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Preview
@Composable
fun ChatMessageBubblePreview() {
    LLAMACHATTheme {
        Column {
            ChatMessageBubble(message = "Hello, this is a user message.", isUserMessage = true)
            ChatMessageBubble(
                message = "Hello, this is a response from the AI.",
                isUserMessage = false
            )
        }
    }
}

private fun extractUserMessage(prompt: String): String {
    val userHeader = "user<|end_header_id|>\n\n"
    val contextHeader = "\n\nContext information is below."
    val startIndex = prompt.indexOf(userHeader)
    if (startIndex == -1) return prompt

    val messageStartIndex = startIndex + userHeader.length
    val endIndex = prompt.indexOf(contextHeader, startIndex = messageStartIndex)

    return if (endIndex != -1) {
        prompt.substring(messageStartIndex, endIndex)
    } else {
        prompt // fallback
    }
}

@Composable
fun ChatScreen(
    viewModel: MainViewModel,
    clipboard: ClipboardManager,
    modelPath: String,
    modifier: Modifier,
    embedder: BertEmbedder,
    embeddingPath: String,
) {
    val scope = rememberCoroutineScope()
    Column(modifier = modifier.padding(16.dp)) {
        val scrollState = rememberLazyListState()

        // Messages area
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            itemsIndexed(viewModel.messages) { index, message ->
                val isUserMessage = index % 2 == 0
                val displayMessage = if (isUserMessage) {
                    extractUserMessage(message)
                } else {
                    message
                }
                ChatMessageBubble(message = displayMessage, isUserMessage = isUserMessage)
            }
        }

        // Input area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = viewModel.message,
                onValueChange = { viewModel.updateMessage(it) },
                label = { Text("Message") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                if (viewModel.message.isNotBlank()) {
                    scope.launch {
                        val userMessage = viewModel.message
                        val embedding = embedder.saveAndGetEmbedding(userMessage)
                        val preparedPrompt = embedder.preparePrompt(userMessage, embedding)
                        viewModel.updateMessage(viewModel.message)
                        viewModel.send(preparedPrompt)
                        viewModel.updateMessage("") // Clear input field immediately
                    }
                }
            }) {
                Text("Send")
            }
        }

        // Utility buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button({ embedder.importFromJson(embeddingPath) }) { Text("Import") }
            Button({ viewModel.load(modelPath) }) { Text("Load") }
            Button({
                viewModel.benchmark(pp = 512, tg = 128, pl = 1, nr = 3)
            }) { Text("Bench") }
        }
    }
}
