package com.huanchengfly.tieba.post.utils

import androidx.compose.runtime.compositionLocalOf
import com.huanchengfly.tieba.post.models.database.Account
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object AccountConstants {
    const val TAG = "AccountUtil"
    const val ACTION_SWITCH_ACCOUNT = "com.huanchengfly.tieba.post.action.SWITCH_ACCOUNT"

    val LocalAccount = compositionLocalOf<Account?> { null }
    val AllAccounts = compositionLocalOf<List<Account>> { emptyList() }

    // 用 StateFlow 管理当前账号
    private val _currentAccount = MutableStateFlow<Account?>(null)
    val currentAccount: StateFlow<Account?> = _currentAccount

    fun setCurrentAccount(account: Account?) {
        _currentAccount.value = account
    }
}