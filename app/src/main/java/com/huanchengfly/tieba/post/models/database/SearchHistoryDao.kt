package com.huanchengfly.tieba.post.models.database

import androidx.room.*

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC")
    suspend fun getAllOrderByTimestampDesc(): List<SearchHistory>

    @Query("SELECT * FROM search_history WHERE content = :content LIMIT 1")
    suspend fun getByContent(content: String): SearchHistory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: SearchHistory): Long

    @Update
    suspend fun update(history: SearchHistory)

    @Query("DELETE FROM search_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM search_history")
    suspend fun deleteAll()
}