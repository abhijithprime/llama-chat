package com.prime.llamachat

import android.llama.cpp.LLamaAndroid
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class MainViewModel(private val llamaAndroid: LLamaAndroid = LLamaAndroid.instance()): ViewModel() {
    // Companion object to hold static constants
    companion object {
        // Constant for converting nanoseconds to seconds
        // @JvmStatic annotation allows this constant to be accessed as a static member in Java
        @JvmStatic
        private val NanosPerSecond = 1_000_000_000.0
    }

    // This is to tag messages with the class name as is in the source code for logging purposes
    private val tag: String? = this::class.simpleName

    // Mutable state to hold the messages displayed in the UI
    // Using mutableStateOf to make it observable by Compose UI
    // This allows the UI to automatically update when messages change
    var messages by mutableStateOf(listOf("Initializing..."))
        private set

    // Mutable state to hold the current message being typed by the user
    // This is also observable by Compose UI
    var message by mutableStateOf("")
        private set

    // This function is called when the ViewModel is cleared, typically when the associated UI is destroyed
    // It launches a coroutine to unload the LLamaAndroid instance
    // This is important to free up resources and avoid memory leaks
    override fun onCleared() {
        super.onCleared()

        viewModelScope.launch {
            try {
                llamaAndroid.unload()
            } catch (exc: IllegalStateException) {
                messages += exc.message!!
            }
        }
    }

    // Function to send a message to the LLamaAndroid instance
    // It retrieves the current message, clears the input field, and adds the message to the
    // messages list to display it in the UI
    // It then launches a coroutine to send the message and handle the response
    // If an error occurs during sending, it catches the exception and adds the error message to
    // the messages list
    fun send() {
        val text = message
        message = ""

        // Add to messages console.
        messages += text
        messages += ""

        viewModelScope.launch {
            llamaAndroid.send(text)
                .catch {
                    Log.e(tag, "send() failed", it)
                    messages += it.message!!
                }
                .collect { messages = messages.dropLast(1) + (messages.last() + it) }
        }
    }

    // Function to benchmark the LLamaAndroid instance
    // It takes parameters for the number of prompts, threads, and prompt length
    // It launches a coroutine to perform the benchmark
    // It measures the time taken for a warm-up run and adds the result to the messages
    // If the warm-up time exceeds 5 seconds, it aborts the benchmark and adds a message to the messages list
    // Finally, it performs the actual benchmark and adds the result to the messages list
    // If an error occurs during the benchmark, it catches the exception and adds the error message to the messages list
    fun benchmark(pp: Int, tg: Int, pl: Int, nr: Int = 1) {
        viewModelScope.launch {
            try {
                val start = System.nanoTime()
                val warmupResult = llamaAndroid.bench(pp, tg, pl, nr)
                val end = System.nanoTime()

                messages += warmupResult

                val warmup = (end - start).toDouble() / NanosPerSecond
                messages += "Warm up time: $warmup seconds, please wait..."

                if (warmup > 5.0) {
                    messages += "Warm up took too long, aborting benchmark"
                    return@launch
                }

                messages += llamaAndroid.bench(512, 128, 1, 3)
            } catch (exc: IllegalStateException) {
                Log.e(tag, "bench() failed", exc)
                messages += exc.message!!
            }
        }
    }

    // Function to load a model into the LLamaAndroid instance
    // It takes the path to the model as a parameter
    // It launches a coroutine to load the model
    // If the model is loaded successfully, it adds a success message to the messages list
    // If an error occurs during loading, it catches the exception and adds the error message to
    // the messages list
    fun load(pathToModel: String) {
        viewModelScope.launch {
            try {
                llamaAndroid.load(pathToModel)
                messages += "Loaded $pathToModel"
            } catch (exc: IllegalStateException) {
                Log.e(tag, "load() failed", exc)
                messages += exc.message!!
            }
        }
    }

    fun updateMessage(newMessage: String) {
        message = newMessage
    }

    fun clear() {
        messages = listOf()
    }

    fun log(message: String) {
        messages += message
    }
}