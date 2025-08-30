package com.prime.llamachat.database.entities

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
data class QuestionsEntity(
    @Id var id: Long = 0,
    var text: String = "",
    var embedding: FloatArray = floatArrayOf()// store embedding vector
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as QuestionsEntity

        if (id != other.id) return false
        if (text != other.text) return false
        if (!embedding.contentEquals(other.embedding)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + text.hashCode()
        result = 31 * result + embedding.contentHashCode()
        return result
    }
}
