package com.huanchengfly.tieba.post.ui.page.history.list

import androidx.compose.runtime.Stable
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.BaseViewModel
import com.huanchengfly.tieba.post.arch.PartialChange
import com.huanchengfly.tieba.post.arch.PartialChangeProducer
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiIntent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.models.database.HistoryDao
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import com.huanchengfly.tieba.post.utils.HistoryUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

abstract class HistoryListViewModel :
    BaseViewModel<HistoryListUiIntent, HistoryListPartialChange, HistoryListUiState, HistoryListUiEvent>() {
    override fun createInitialState(): HistoryListUiState = HistoryListUiState()

    override fun dispatchEvent(partialChange: HistoryListPartialChange): UiEvent? {
        return when (partialChange) {
            is HistoryListPartialChange.Delete.Success -> HistoryListUiEvent.Delete.Success
            is HistoryListPartialChange.Delete.Failure -> HistoryListUiEvent.Delete.Failure(
                partialChange.error.getErrorMessage()
            )

            else -> null
        }
    }
}

@Stable
@HiltViewModel
class ThreadHistoryListViewModel @Inject constructor(
    private val historyDao: HistoryDao
) : HistoryListViewModel() {
    override fun createPartialChangeProducer(): PartialChangeProducer<HistoryListUiIntent, HistoryListPartialChange, HistoryListUiState> =
        HistoryListPartialChangeProducer(HistoryUtil.TYPE_THREAD, historyDao)
}

@Stable
@HiltViewModel
class ForumHistoryListViewModel @Inject constructor(
    private val historyDao: HistoryDao
) : HistoryListViewModel() {
    override fun createPartialChangeProducer(): PartialChangeProducer<HistoryListUiIntent, HistoryListPartialChange, HistoryListUiState> =
        HistoryListPartialChangeProducer(HistoryUtil.TYPE_FORUM, historyDao)
}

private class HistoryListPartialChangeProducer(
    val type: Int,
    private val historyDao: HistoryDao
) : PartialChangeProducer<HistoryListUiIntent, HistoryListPartialChange, HistoryListUiState> {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun toPartialChangeFlow(intentFlow: Flow<HistoryListUiIntent>): Flow<HistoryListPartialChange> =
        merge(
            intentFlow.filterIsInstance<HistoryListUiIntent.Refresh>()
                .flatMapLatest { produceRefreshPartialChange() },
            intentFlow.filterIsInstance<HistoryListUiIntent.LoadMore>()
                .flatMapLatest { it.producePartialChange() },
            intentFlow.filterIsInstance<HistoryListUiIntent.Delete>()
                .flatMapLatest { it.producePartialChange() },
            intentFlow.filterIsInstance<HistoryListUiIntent.DeleteAll>()
                .flatMapLatest { produceDeleteAllPartialChange() },
        )

    private fun produceDeleteAllPartialChange() =
        flow {
            // 用 Room 删除全部替换 LitePal
            historyDao.deleteAllByType(type)
            emit(HistoryListPartialChange.DeleteAll)
        }.flowOn(Dispatchers.IO)

    private fun produceRefreshPartialChange(): Flow<HistoryListPartialChange> =
        flow {
            val histories = historyDao.getByTypePaged(type, 0, HistoryUtil.PAGE_SIZE)
            val (today, before) = histories.partition { DateTimeUtils.isToday(it.timestamp) }
            emit(
                HistoryListPartialChange.Refresh.Success(
                    today,
                    before,
                    histories.size == HistoryUtil.PAGE_SIZE,
                )
            )
        }.catch { HistoryListPartialChange.Refresh.Failure(it) }
            .flowOn(Dispatchers.IO)

    private fun HistoryListUiIntent.LoadMore.producePartialChange(): Flow<HistoryListPartialChange> =
        flow {
            val histories = historyDao.getByTypePaged(type, page, HistoryUtil.PAGE_SIZE)
            emit(
                HistoryListPartialChange.LoadMore.Success(
                    histories.filter { DateTimeUtils.isToday(it.timestamp) },
                    histories.filterNot { DateTimeUtils.isToday(it.timestamp) },
                    histories.size == HistoryUtil.PAGE_SIZE,
                    page
                )
            )
        }.onStart { HistoryListPartialChange.LoadMore.Start }
            .catch { HistoryListPartialChange.LoadMore.Failure(it) }
            .flowOn(Dispatchers.IO)

    private fun HistoryListUiIntent.Delete.producePartialChange() =
        flow {
            // 用 Room 按 id 删除替换 LitePal
            val count = historyDao.deleteById(id)
            if (count > 0) emit(HistoryListPartialChange.Delete.Success(id))
            else emit(HistoryListPartialChange.Delete.Failure(IllegalStateException("未知错误")))
        }.catch { emit(HistoryListPartialChange.Delete.Failure(it)) }
            .flowOn(Dispatchers.IO)
}

sealed interface HistoryListUiIntent : UiIntent {
    object Refresh : HistoryListUiIntent

    data class LoadMore(val page: Int) : HistoryListUiIntent

    data class Delete(val id: Long) : HistoryListUiIntent

    object DeleteAll : HistoryListUiIntent
}

sealed interface HistoryListPartialChange : PartialChange<HistoryListUiState> {
    object DeleteAll : HistoryListPartialChange {
        override fun reduce(oldState: HistoryListUiState): HistoryListUiState = oldState.copy(
            todayHistoryData = emptyList(),
            beforeHistoryData = emptyList(),
            currentPage = 0,
            hasMore = false,
            isLoadingMore = false,
            isRefreshing = false
        )
    }

    sealed class Refresh : HistoryListPartialChange {
        override fun reduce(oldState: HistoryListUiState): HistoryListUiState = when (this) {
            is Failure -> oldState
            is Success -> oldState.copy(
                todayHistoryData = todayHistoryData,
                beforeHistoryData = beforeHistoryData,
                currentPage = 0,
                hasMore = hasMore
            )
        }

        data class Success(
            val todayHistoryData: List<History>,
            val beforeHistoryData: List<History>,
            val hasMore: Boolean
        ) : Refresh()

        data class Failure(
            val error: Throwable
        ) : Refresh()
    }

    sealed class LoadMore : HistoryListPartialChange {
        override fun reduce(oldState: HistoryListUiState): HistoryListUiState = when (this) {
            is Failure -> oldState.copy(isLoadingMore = false)
            Start -> oldState.copy(isLoadingMore = true)
            is Success -> oldState.copy(
                isLoadingMore = false,
                todayHistoryData = oldState.todayHistoryData + todayHistoryData,
                beforeHistoryData = oldState.beforeHistoryData + beforeHistoryData,
                currentPage = currentPage,
                hasMore = hasMore
            )
        }

        object Start : LoadMore()

        data class Success(
            val todayHistoryData: List<History>,
            val beforeHistoryData: List<History>,
            val hasMore: Boolean,
            val currentPage: Int
        ) : LoadMore()

        data class Failure(
            val error: Throwable
        ) : LoadMore()
    }

    sealed class Delete : HistoryListPartialChange {
        override fun reduce(oldState: HistoryListUiState): HistoryListUiState = when (this) {
            is Failure -> oldState
            is Success -> oldState.copy(
                todayHistoryData = oldState.todayHistoryData.filterNot { it.id == id },
                beforeHistoryData = oldState.beforeHistoryData.filterNot { it.id == id })
        }

        data class Success(
            val id: Long
        ) : Delete()

        data class Failure(
            val error: Throwable
        ) : Delete()
    }
}

data class HistoryListUiState(
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val currentPage: Int = 0,
    val todayHistoryData: List<History> = emptyList(),
    val beforeHistoryData: List<History> = emptyList(),
) : UiState

sealed interface HistoryListUiEvent : UiEvent {
    sealed interface Delete : HistoryListUiEvent {
        object Success : Delete

        data class Failure(
            val errorMsg: String
        ) : Delete
    }

    object DeleteAll : HistoryListUiEvent
}