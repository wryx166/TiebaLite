package com.huanchengfly.tieba.post.models.database

import androidx.room.*

@Dao
interface AccountDao {
    @Query("SELECT * FROM account")
    suspend fun getAll(): List<Account>

    @Query("SELECT * FROM account WHERE id = :id")
    suspend fun getById(id: Int): Account?

    @Query("SELECT * FROM account WHERE uid = :uid")
    suspend fun getByUid(uid: String): Account?

    @Query("SELECT * FROM account WHERE bduss = :bduss")
    suspend fun getByBduss(bduss: String): Account?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: Account): Long

    @Update
    suspend fun update(account: Account)

    @Delete
    suspend fun delete(account: Account)

    @Update
    suspend fun updateAccount(account: Account)
    suspend fun insertOrUpdate(account: Account) {
        val existingAccount = getById(account.id)
        if (existingAccount == null) {
            insert(account)
        } else {
            update(account)
        }
    }
}