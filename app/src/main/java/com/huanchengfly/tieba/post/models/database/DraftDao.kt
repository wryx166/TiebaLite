package com.huanchengfly.tieba.post.models.database

import androidx.room.*

@Dao
interface DraftDao {
    @Query("SELECT * FROM draft")
    suspend fun getAll(): List<Draft>

    @Query("SELECT * FROM draft WHERE hash = :hash LIMIT 1")
    suspend fun getByHash(hash: String): Draft?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(draft: Draft): Long

    @Update
    suspend fun update(draft: Draft)

    @Delete
    suspend fun delete(draft: Draft)

    @Query("DELETE FROM draft WHERE hash = :hash")
    suspend fun deleteByHash(hash: String)

    @Query("SELECT * FROM draft WHERE hash = :hash LIMIT 1")
    suspend fun getDraft(hash: String): Draft?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(draft: Draft)

    @Query("DELETE FROM draft WHERE hash = :hash")
    suspend fun deleteDraft(hash: String)
}