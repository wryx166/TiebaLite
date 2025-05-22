package com.huanchengfly.tieba.post.models.database


import androidx.room.*

@Dao
interface BlockDao {
    @Query("SELECT * FROM block")
    suspend fun getAll(): List<Block>

    @Insert
    suspend fun insert(block: Block): Long

    @Delete
    suspend fun delete(block: Block)

    @Query("DELETE FROM block WHERE id = :id")
    suspend fun deleteById(id: Long)
}