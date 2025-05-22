package com.huanchengfly.tieba.post.models.database

import androidx.room.*

@Dao
interface TopForumDao {
    @Query("SELECT * FROM top_forum ORDER BY id ASC")
    suspend fun getAll(): List<TopForum>

    @Query("SELECT * FROM top_forum WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TopForum?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(topForum: TopForum): Long

    @Update
    suspend fun update(topForum: TopForum)

    @Delete
    suspend fun delete(topForum: TopForum)

    @Query("DELETE FROM top_forum WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM top_forum WHERE forumId = :forumId")
    suspend fun deleteByForumId(forumId: String): Int
}