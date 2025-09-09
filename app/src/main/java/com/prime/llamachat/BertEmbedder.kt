package com.prime.llamachat

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import com.prime.llamachat.database.ObjectBox
import com.prime.llamachat.database.entities.DocChunkEntity
import com.prime.llamachat.database.entities.DocChunkEntity_
import com.prime.llamachat.database.entities.QuestionsEntity
import io.objectbox.Box
import io.objectbox.kotlin.boxFor
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.util.PriorityQueue
import kotlin.math.sqrt

class BertEmbedder(context: Context) {

    private val embedder: TextEmbedder =
        TextEmbedder.createFromFile(context.applicationContext, "bert_embedder.tflite")
    private val questionBox: Box<QuestionsEntity> = ObjectBox.store.boxFor()
    private val documentBox: Box<DocChunkEntity> = ObjectBox.store.boxFor()

    // Generate embedding for a text
    private fun getEmbedding(text: String): FloatArray {
        val result = embedder.embed(text)
        val embedding: FloatArray = result.embeddingResult().embeddings()[0].floatEmbedding()
//        return embedding
        // L2 normalize on mobile
        val norm = sqrt(embedding.map { it * it }.sum().toDouble())
        return embedding.map { (it / norm).toFloat() }.toFloatArray()
    }

    // Save question with its embedding
    fun saveAndGetEmbedding(text: String): FloatArray {
        // Split the text into words
        val words = text.split("\\s+".toRegex()).toMutableList()

        // Desired chunk size
        val chunkSize = 100

        // Pad with [PAD] tokens if necessary
        while (words.size < chunkSize) {
            words.add("[PAD]")
        }

        // Rejoin into a single string for embedding
        val paddedText = words.joinToString(" ")

        // Generate embedding
        val embedding = getEmbedding(paddedText)

        // Save to ObjectBox
        val question = QuestionsEntity(text = paddedText, embedding = embedding)
        questionBox.put(question)

        return embedding
    }

    /**
     * Compute cosine similarity between two vectors (optimized)
     *
     * @param a First vector.
     * @param b Second vector.
     * @return Cosine similarity score between -1 and 1.
     * @throws IllegalArgumentException if vectors have different dimensions.
     */
    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) {
            throw IllegalArgumentException("Vectors must have the same dimension: ${a.size} vs ${b.size}")
        }
        if (a.isEmpty()) {
            return 0f  // Or throw if empty vectors are invalid in your pipeline
        }

        var dot = 0f
        var normA = 0f
        var normB = 0f
        repeat(a.size) { i ->
            val ai = a[i]
            val bi = b[i]
            dot += ai * bi
            normA += ai * ai
            normB += bi * bi
        }

        val denom = sqrt(normA) * sqrt(normB)
        return if (denom > 0f) dot / denom else 0f  // Handle zero-norm vectors gracefully
    }

    // Find most similar question from DB (optimized)
    fun findMostSimilar(query: String): QuestionsEntity? {
        val queryEmbedding = getEmbedding(query)
        var bestMatch: QuestionsEntity? = null
        var bestScore = Float.NEGATIVE_INFINITY

        for (question in questionBox.all) {
            val score = cosineSimilarity(question.embedding, queryEmbedding)
            if (score > bestScore) {
                bestScore = score
                bestMatch = question
            }
        }

        return bestMatch
    }

    /**
     * Finds the top N most similar DocChunkEntity objects to the query based on cosine similarity.
     * Uses a min-heap for efficient top-k selection. Optimized for small datasets (e.g., 100-150 chunks).
     *
     * @param queryEmbedding The embedding of the input query.
     * @param docs List of DocChunkEntity to search within.
     * @param n Number of top results to return (default 3).
     *
     * @return List of top N DocChunkEntity sorted by descending similarity.
     */
    fun findTopNSimilar(queryEmbedding: FloatArray, docs:List<DocChunkEntity>, n: Int = 3): List<DocChunkEntity> {

        val minHeap = PriorityQueue<Pair<DocChunkEntity, Float>>(compareBy { it.second })

        for (chunk in docs) {
            val score = cosineSimilarity(queryEmbedding, chunk.embedding)
            if (minHeap.size < n) {
                minHeap.add(chunk to score)
            } else if (score > (minHeap.peek()?.second ?: 0f)) {
                minHeap.poll()
                minHeap.add(chunk to score)
            }
        }

        return minHeap.sortedByDescending { it.second }
            .map { it.first }
    }

    /**
     * Search for similar embeddings in the DB using ObjectBox's ANN search and refine with cosine similarity.
     * Optimized for scalable performance.
     *
     * Uses ObjectBox's built-in nearest neighbor search to quickly narrow down candidates,
     * then applies cosine similarity to find the top K most similar chunks.
     *
     * @param queryEmbedding The embedding of the input query.
     * @param topK Number of top results to return (default 2).
     * @return List of top K DocChunkEntity sorted by descending similarity.
     * @throws IllegalArgumentException if queryEmbedding is empty or has invalid dimensions.
     * @throws IllegalStateException if ObjectBox query fails.
     * @see findTopNSimilar
     */
    fun searchSimilarEmbeddings(
        queryEmbedding: FloatArray,
        topK: Int = 2
    ): List<DocChunkEntity> {
        val query =
            documentBox.query().nearestNeighbors(DocChunkEntity_.embedding, queryEmbedding, 15)
                .build()
        val annChunks = query.find()
        val topChunks = findTopNSimilar(queryEmbedding, annChunks, topK)
        for (result in topChunks) {
            Log.i("BertEmbedder", "Found id: ${result.id} with score: ${result.chunkText}")
        }
        return topChunks
    }

    fun preparePrompt(text: String, embedding: FloatArray): String {
        // Search for similar embeddings in the DB
        val similar = searchSimilarEmbeddings(embedding, 2)
        for (s in similar) {
            Log.i("BertEmbedder", "similar chunk: ${s.chunkText}")
        }
        // Generate a Llama 3.2-1B friendly prompt using chat format
        val contextBlocks = similar.joinToString("\n") { it.chunkText }
        val prompt = buildString {
            if (contextBlocks.isNotBlank()) {
                append("\n<|system|>\nYou are a helpful assistant. Use the following context to answer the user's question. If the answer is not in the context, say 'I don't know, Please contact Ackcio team for further support.'\n")
                append("<|context|>\n")
                append(contextBlocks)
                append("\n")
            } else {
                append("\n<|system|>\nYou are a helpful assistant.\n")
            }
            append("<|user|>\n")
            append(text)
            append("\n<|assistant|>\n")
        }
        Log.i("BertEmbedder", "Prepared Llama3.2-1B prompt: $prompt")
        return prompt
    }

    fun importFromJson(jsonPath: String) {
        try {
            val jsonString = File(jsonPath).readText()
            // Parse directly into your entity
            val entities: List<DocChunkEntity> =
                Json.decodeFromString(ListSerializer(DocChunkEntity.serializer()), jsonString)
            // Delete all previous entries
            documentBox.removeAll()
            Log.i("BertEmbedder", "Importing ${entities.size} entities from $jsonPath")
            // Save into ObjectBox
            documentBox.put(entities)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}