package com.huanchengfly.tieba.post.models.database

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class HistoryRepository  @Inject constructor(
    private val dao: HistoryDao
) {

    companion object {
        const val PAGE_SIZE = 100
        const val TYPE_FORUM = 1
        const val TYPE_THREAD = 2
    }

    suspend fun saveOrUpdate(history: History) {
        val existing = dao.findByData(history.data)
        if (existing != null) {
            val updated = existing.copy(
                timestamp = System.currentTimeMillis(),
                title = history.title,
                extras = history.extras,
                avatar = history.avatar,
                username = history.username,
                count = existing.count + 1
            )
            dao.update(updated)
        } else {
            val newHistory = history.copy(
                count = 1,
                timestamp = System.currentTimeMillis()
            )
            dao.insert(newHistory)
        }
    }

    fun getFlow(type: Int, page: Int): Flow<List<History>> {
        val offset = page * PAGE_SIZE
        return dao.getFlow(type, PAGE_SIZE, offset)
    }

    suspend fun deleteAll() = dao.deleteAll()
    suspend fun getAll(type: Int) = dao.getAllByType(type)
    suspend fun getAll() = dao.getAll()
}