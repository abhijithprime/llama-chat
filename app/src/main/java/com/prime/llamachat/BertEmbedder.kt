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
import kotlin.math.sqrt

class BertEmbedder(context: Context) {

    private val embedder: TextEmbedder =
        TextEmbedder.createFromFile(context.applicationContext, "mobilebert.tflite")
    private val questionBox: Box<QuestionsEntity> = ObjectBox.store.boxFor()
    private val documentBox: Box<DocChunkEntity> = ObjectBox.store.boxFor()

    // Generate embedding for a text
    private fun getEmbedding(text: String): FloatArray {
        val result = embedder.embed(text)
        val embedding: FloatArray = result.embeddingResult().embeddings()[0].floatEmbedding()
        // L2 normalize on mobile
        val norm = sqrt(embedding.map { it * it }.sum().toDouble())
        return embedding.map { (it / norm).toFloat() }.toFloatArray()
    }

    // Save question with its embedding
    fun saveAndGetEmbedding(text: String) : FloatArray {
        val embedding = getEmbedding(text)
        val question = QuestionsEntity(text = text, embedding = embedding)
        questionBox.put(question)
        return embedding
    }

    // Compute cosine similarity between two vectors (optimized)
    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        var normA = 0f
        var normB = 0f
        val size = a.size
        for (i in 0 until size) {
            val ai = a[i]
            val bi = b[i]
            dot += ai * bi
            normA += ai * ai
            normB += bi * bi
        }
        return dot / (sqrt(normA.toDouble()) * sqrt(normB.toDouble())).toFloat()
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

//    fun findTopNSimilar(query: String, n: Int = 3): List<DocChunkEntity> {
//        val queryEmbedding = getEmbedding(query)
//
//        val minHeap = PriorityQueue<Pair<DocChunkEntity, Float>>(compareBy { it.second })
//
//        for (chunk in documentBox.all) {
//            Embedding.create()
//            val score = TextEmbedder.cosineSimilarity(queryEmbedding, chunk.embedding)
//
//            if (minHeap.size < n) {
//                minHeap.add(chunk to score)
//            } else if (score > (minHeap.peek()?.second ?: 0f)) {
//                minHeap.poll()
//                minHeap.add(chunk to score)
//            }
//        }
//
//        return minHeap.sortedByDescending { it.second }
//            .map { it.first }
//    }

    fun searchSimilarEmbeddings(
        queryEmbedding: FloatArray,
        topK: Int = 2
    ): List<DocChunkEntity> {
        val query =
            documentBox.query().nearestNeighbors(DocChunkEntity_.embedding, queryEmbedding, topK)
                .build()
        val results = query.find()
        val scores = query.findIdsWithScores()
        for (result in scores) {
            Log.i("BertEmbedder", "Found id: ${result.id} with score: ${result.score}")
        }
        return results
    }

    fun preparePrompt(text: String, embedding: FloatArray): String {
        // Search for similar embeddings in the DB
        val similar = searchSimilarEmbeddings(embedding, 2)
        for (s in similar) {
            Log.i("BertEmbedder", "similar chunk: ${s.chunkText}")
        }
        // Generate Prompt from the retrieved chunks
        Log.i("BertEmbedder", "Saved question: $text with embedding size: ${embedding.size}")
        val prompt = """
            INSTRUCTIONS: Use the following pieces of context to answer the question at the end. If you don't know the answer, just say that you don't know, don't try to make up an answer and tell the user to contact the ACKCIO team for further assistance.
            If the question is not related to the context, politely respond that you are tuned to only answer questions that are related to the context.
            
            CONTEXT:
            ${similar.joinToString("\n") { it.chunkText }}
            
            QUESTION:
            $text
        """.trimIndent()
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