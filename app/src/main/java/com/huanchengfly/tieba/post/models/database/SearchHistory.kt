package com.huanchengfly.tieba.post.models.database

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "search_history")
data class SearchHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
)
