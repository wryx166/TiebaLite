package com.huanchengfly.tieba.post.utils

import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.models.database.HistoryDao
import kotlinx.coroutines.flow.Flow

object HistoryUtil {
    const val PAGE_SIZE = 100
    const val TYPE_FORUM = 1
    const val TYPE_THREAD = 2

    lateinit var historyDao: HistoryDao

    suspend fun deleteAll() {
        historyDao.deleteAll()
    }

    suspend fun saveHistory(history: History) {
        val existing = historyDao.findByData(history.data)
        if (existing != null) {
            val updated = existing.copy(
                timestamp = System.currentTimeMillis(),
                title = history.title,
                extras = history.extras,
                avatar = history.avatar,
                username = history.username,
                count = existing.count + 1
            )
            historyDao.update(updated)
        } else {
            val newHistory = history.copy(count = 1, timestamp = System.currentTimeMillis())
            historyDao.insert(newHistory)
        }
    }

    suspend fun getAll(): List<History> {
        return historyDao.getAll(PAGE_SIZE)
    }

    suspend fun getAll(type: Int): List<History> {
        return historyDao.getAllByType(type, PAGE_SIZE)
    }

    fun getFlow(type: Int, page: Int): Flow<List<History>> {
        return historyDao.getFlow(type, PAGE_SIZE, page * PAGE_SIZE)
    }

}