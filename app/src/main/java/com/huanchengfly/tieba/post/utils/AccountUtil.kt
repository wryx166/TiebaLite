package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.content.Intent
import android.webkit.CookieManager
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.LoginBean
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.models.database.AccountDao
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import androidx.core.content.edit

@Stable
object AccountUtil {
    private lateinit var accountDao: AccountDao

    fun init(accountDao: AccountDao) {
        this.accountDao = accountDao
    }

    val dao: AccountDao
        get() = accountDao

    const val TAG = "AccountUtil"
    const val ACTION_SWITCH_ACCOUNT = "com.huanchengfly.tieba.post.action.SWITCH_ACCOUNT"

    val LocalAccount = staticCompositionLocalOf<Account?> { null }
    val AllAccounts = staticCompositionLocalOf<List<Account>> { emptyList() }

    @Composable
    fun LocalAccountProvider(content: @Composable () -> Unit) {
        val account by mutableCurrentAccountState
        val allAccounts by mutableAllAccountsState
        CompositionLocalProvider(
            LocalAccount provides account,
            AllAccounts provides allAccounts
        ) {
            content()
        }
    }

    @set:Synchronized
    private var mutableCurrentAccountState: MutableState<Account?> = mutableStateOf(null)
    private var mutableAllAccountsState: MutableState<List<Account>> = mutableStateOf(emptyList())

    val currentAccount get() = AccountConstants.currentAccount.value
    val allAccounts: List<Account> get() = mutableAllAccountsState.value

    @OptIn(DelicateCoroutinesApi::class)
    fun init(context: Context) {
        GlobalScope.launch(Dispatchers.IO) {
            val loginUser =
                context.getSharedPreferences("accountData", Context.MODE_PRIVATE).getInt("now", -1)
            val account = if (loginUser == -1) null else accountDao.getById(loginUser)
            val all = accountDao.getAll()
            withContext(Dispatchers.Main) {
                mutableCurrentAccountState.value = account
                mutableAllAccountsState.value = all
            }
        }
    }

    @JvmStatic
    fun getLoginInfo(): Account? {
        return currentAccount
    }

    @JvmStatic
    fun <T> getAccountInfo(getter: Account.() -> T): T? {
        return currentAccount?.getter()
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun newAccount(uid: String, account: Account, callback: (Boolean) -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            val existing = accountDao.getByUid(account.uid)
            if (existing == null) {
                accountDao.insert(account)
            } else {
                accountDao.update(account)
            }
            val all = accountDao.getAll()
            withContext(Dispatchers.Main) {
                mutableAllAccountsState.value = all
                callback(true)
            }
        }
    }

    private suspend fun getAccountInfo(accountId: Int): Account? {
        return accountDao.getById(accountId)
    }

    @JvmStatic
    suspend fun getAccountInfoByUid(uid: String): Account? {
        return accountDao.getByUid(uid)
    }

    @JvmStatic
    suspend fun getAccountInfoByBduss(bduss: String): Account? {
        return accountDao.getByBduss(bduss)
    }

    @JvmStatic
    fun isLoggedIn(): Boolean {
        return getLoginInfo() != null
    }

    @OptIn(DelicateCoroutinesApi::class)
    @JvmStatic
    fun switchAccount(context: Context, id: Int): Boolean {
        context.sendBroadcast(Intent().setAction(ACTION_SWITCH_ACCOUNT))
        GlobalScope.launch(Dispatchers.IO) {
            val account = getAccountInfo(id)
            withContext(Dispatchers.Main) {
                if (account != null) {
                    mutableCurrentAccountState.value = account
                    GlobalScope.launch { emitGlobalEvent(GlobalEvent.AccountSwitched) }
                    context.getSharedPreferences("accountData", Context.MODE_PRIVATE).edit(commit = true) {
                        putInt("now", id)
                    }
                }
            }
        }
        return true
    }

    private fun updateAccount(
        account: Account,
        loginBean: LoginBean,
    ) {
        account.apply {
            uid = loginBean.user.id
            name = loginBean.user.name
            portrait = loginBean.user.portrait
            tbs = loginBean.anti.tbs
            if (uuid.isNullOrBlank()) uuid = UUID.randomUUID().toString()
        }
    }

    fun fetchAccountFlow(account: Account = getLoginInfo()!!): Flow<Account> {
        return fetchAccountFlow(account.bduss, account.sToken, account.cookie)
    }

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    fun fetchAccountFlow(
        bduss: String,
        sToken: String,
        cookie: String? = null
    ): Flow<Account> {
        return TiebaApi.getInstance()
            .loginFlow(bduss, sToken)
            .zip(TiebaApi.getInstance().initNickNameFlow(bduss, sToken)) { loginBean, _ ->
                getAccountInfoByUid(loginBean.user.id)?.apply {
                    this.bduss = bduss
                    this.sToken = sToken
                    this.cookie = cookie ?: getBdussCookie(bduss)
                    updateAccount(this, loginBean)
                } ?: Account(
                    uid = loginBean.user.id,
                    name = loginBean.user.name,
                    bduss = bduss,
                    tbs = loginBean.anti.tbs,
                    portrait =  loginBean.user.portrait,
                    sToken =  sToken,
                    cookie =  cookie ?: getBdussCookie(bduss),
                )
            }
            .zip(SofireUtils.fetchZid()) { account, zid ->
                account.apply { this.zid = zid }
            }
            .flatMapConcat { account ->
                TiebaApi.getInstance()
                    .getUserInfoFlow(account.uid.toLong(), account.bduss, account.sToken)
                    .map { checkNotNull(it.data_?.user) }
                    .map { user ->
                        account.apply {
                            nameShow = user.nameShow
                            portrait = user.portrait
                        }
                    }
                    .catch {
                        emit(account)
                    }
            }
            .onEach { account ->
                GlobalScope.launch(Dispatchers.IO) {
                    val existing = accountDao.getByUid(account.uid)
                    if (existing == null) {
                        accountDao.insert(account)
                    } else {
                        accountDao.update(account)
                    }
                    val all = accountDao.getAll()
                    withContext(Dispatchers.Main) {
                        mutableAllAccountsState.value = all
                    }
                }
            }
            .flowOn(Dispatchers.IO)
    }

    fun parseCookie(cookie: String): Map<String, String> {
        return cookie
            .split(";")
            .map { it.trim().split("=") }
            .filter { it.size > 1 }
            .associate { it.first() to it.drop(1).joinToString("=") }
    }

    @JvmStatic
    suspend fun updateLoginInfo(cookie: String): Boolean {
        val cookies = parseCookie(cookie).mapKeys { it.key.uppercase() }
        val bduss = cookies["BDUSS"]
        val sToken = cookies["STOKEN"]
        if (bduss != null && sToken != null) {
            val account = getAccountInfoByBduss(bduss)
            account?.apply {
                this.sToken = sToken
                this.cookie = cookie
                accountDao.insertOrUpdate(this)
            }
            return true
        }
        return false
    }

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun exit(context: Context) {
        GlobalScope.launch(Dispatchers.IO) {
            var accounts = allAccounts
            var account = getLoginInfo() ?: return@launch
            accountDao.delete(account)
            CookieManager.getInstance().removeAllCookies(null)
            withContext(Dispatchers.Main) {
                if (accounts.isNotEmpty()) {
                    val next = accounts[0]
                    switchAccount(context, next.id)
                    Toast.makeText(context, "退出登录成功，已切换至账号 " + next.nameShow, Toast.LENGTH_SHORT).show()
                } else {
                    mutableCurrentAccountState.value = null
                    context.getSharedPreferences("accountData", Context.MODE_PRIVATE).edit(commit = true) { clear() }
                    Toast.makeText(context, R.string.toast_exit_account_success, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun getSToken(): String? {
        val account = getLoginInfo()
        return account?.sToken
    }

    fun getCookie(): String? {
        val account = getLoginInfo()
        return account?.cookie
    }

    fun getUid(): String? {
        val account = getLoginInfo()
        return account?.uid
    }

    fun getBduss(): String? {
        val account = getLoginInfo()
        return account?.bduss
    }

    @JvmStatic
    fun getBdussCookie(): String? {
        val bduss = getBduss()
        return if (bduss != null) {
            getBdussCookie(bduss)
        } else null
    }

    fun getBdussCookie(bduss: String): String {
        return "BDUSS=$bduss; Path=/; Max-Age=315360000; Domain=.baidu.com; Httponly"
    }
}