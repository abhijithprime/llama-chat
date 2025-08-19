package com.prime.llamachat.database.entities

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity()
data class DocChunkEntity(
    @Id
    var id: Long = 0,
    var chunkText: String,
    var embedding: ByteArray // Use ByteArray for embeddings
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DocChunkEntity

        if (id != other.id) return false
        if (chunkText != other.chunkText) return false
        if (!embedding.contentEquals(other.embedding)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + chunkText.hashCode()
        result = 31 * result + embedding.contentHashCode()
        return result
    }
}
