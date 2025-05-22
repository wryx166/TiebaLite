package com.huanchengfly.tieba.post.models.database

import androidx.room.*

@Dao
interface SearchPostHistoryDao {
    @Query("SELECT * FROM search_post_history ORDER BY timestamp DESC")
    suspend fun getAll(): List<SearchPostHistory>

    @Query("SELECT * FROM search_post_history WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SearchPostHistory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(searchPostHistory: SearchPostHistory): Long

    @Update
    suspend fun update(searchPostHistory: SearchPostHistory)

    @Delete
    suspend fun delete(searchPostHistory: SearchPostHistory)

    @Query("DELETE FROM search_post_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM search_post_history ORDER BY timestamp DESC")
    suspend fun getAllOrderByTimestampDesc(): List<SearchPostHistory>

    @Query("SELECT * FROM search_post_history WHERE content = :content LIMIT 1")
    suspend fun getByContent(content: String): SearchPostHistory?

    @Query("DELETE FROM search_post_history")
    suspend fun deleteAll()
}