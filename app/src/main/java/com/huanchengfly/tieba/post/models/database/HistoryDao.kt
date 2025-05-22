package com.huanchengfly.tieba.post.models.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("DELETE FROM history")
    suspend fun deleteAll()

    @Query("SELECT * FROM history ORDER BY timestamp DESC, count DESC LIMIT :limit")
    suspend fun getAll(limit: Int = 100): List<History>

    @Query("SELECT * FROM history WHERE type = :type ORDER BY timestamp DESC, count DESC LIMIT :limit")
    suspend fun getAllByType(type: Int, limit: Int = 100): List<History>

    @Query("SELECT * FROM history WHERE type = :type ORDER BY timestamp DESC, count DESC LIMIT :limit OFFSET :offset")
    fun getFlow(type: Int, limit: Int = 100, offset: Int = 0): Flow<List<History>>

    @Query("SELECT * FROM history WHERE data = :data LIMIT 1")
    suspend fun findByData(data: String): History?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: History): Long

    @Update
    suspend fun update(history: History): Int

    @Query("SELECT * FROM history WHERE type = :type ORDER BY timestamp DESC LIMIT :pageSize OFFSET :offset")
    suspend fun getByTypePaged(type: Int, pageSize: Int, offset: Int): List<History>

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM history WHERE type = :type")
    suspend fun deleteAllByType(type: Int)
}