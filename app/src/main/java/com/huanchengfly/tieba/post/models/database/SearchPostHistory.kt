package com.huanchengfly.tieba.post.models.database

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "search_post_history")
data class SearchPostHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val content: String,
    val forumName: String,
    val timestamp: Long = System.currentTimeMillis(),
)
