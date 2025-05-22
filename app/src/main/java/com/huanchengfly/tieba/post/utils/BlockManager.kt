package com.huanchengfly.tieba.post.utils

import com.huanchengfly.tieba.post.api.models.MessageListBean
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.SubPostList
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.abstractText
import com.huanchengfly.tieba.post.api.models.protos.plainText
import com.huanchengfly.tieba.post.models.database.Block
import com.huanchengfly.tieba.post.models.database.Block.Companion.getKeywords
import com.huanchengfly.tieba.post.models.database.BlockDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

object BlockManager {
    private val blockList: MutableList<Block> = mutableListOf()
    lateinit var blockDao: BlockDao

    fun init(blockDao: BlockDao) {
        this.blockDao = blockDao
    }

    val blackList: List<Block>
        get() = blockList.filter { it.category == Block.CATEGORY_BLACK_LIST }

    val whiteList: List<Block>
        get() = blockList.filter { it.category == Block.CATEGORY_WHITE_LIST }

    suspend fun addBlock(block: Block) {
        withContext(Dispatchers.IO) {
            val id = blockDao.insert(block)
            blockList.add(block.copy(id = id))
        }
    }

    suspend fun removeBlock(id: Long) {
        withContext(Dispatchers.IO) {
            blockDao.deleteById(id)
            blockList.removeAll { it.id == id }
        }
    }

    suspend fun init() {
        withContext(Dispatchers.IO) {
            blockList.clear()
            blockList.addAll(blockDao.getAll())
        }
    }

    fun shouldBlock(content: String): Boolean {
        // 支持正则表达式的屏蔽判断
        val isWhite = whiteList.any { block ->
            block.type == Block.TYPE_KEYWORD && block.getKeywords().any { keyword ->
                if (block.isRegex) {
                    try {
                        Pattern.compile(keyword).matcher(content).find()
                    } catch (_: Exception) {
                        false
                    }
                } else {
                    content.contains(keyword)
                }
            }
        }
        if (isWhite)
            return false
        val isBlack = blackList.any { block ->
            block.type == Block.TYPE_KEYWORD && block.getKeywords().any { keyword ->
                if (block.isRegex) {
                    try {
                        Pattern.compile(keyword).matcher(content).find()
                    } catch (_: Exception) {
                        false // 如果正则表达式非法则忽略
                    }
                } else {
                    content.contains(keyword)
                }
            }
        }
        return isBlack
    }

    fun shouldBlock(userId: Long = 0L, userName: String? = null): Boolean {
        val isWhite = whiteList.any { block ->
            !block.isRegex &&
                    block.type == Block.TYPE_USER &&
                    (block.uid == userId.toString() || block.username == userName)
        }
        if (isWhite) return false

        val isBlack = blackList.any { block ->
            !block.isRegex &&
                    block.type == Block.TYPE_USER &&
                    (block.uid == userId.toString() || block.username == userName)
        }
        return isBlack
    }

    fun ThreadInfo.shouldBlock(): Boolean =
        shouldBlock(title) || shouldBlock(abstractText) || shouldBlock(authorId, author?.name)

    fun Post.shouldBlock(): Boolean =
        shouldBlock(content.plainText) || shouldBlock(author_id, author?.name)

    fun SubPostList.shouldBlock(): Boolean =
        shouldBlock(content.plainText) || shouldBlock(author_id, author?.name)

    fun MessageListBean.MessageInfoBean.shouldBlock(): Boolean =
        shouldBlock(content.orEmpty()) || shouldBlock(
            this.replyer?.id?.toLongOrNull() ?: -1,
            this.replyer?.name.orEmpty()
        )
}