package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY CASE WHEN accountMode = 'AUTHENTICATED' THEN 0 ELSE 1 END, updatedAt DESC LIMIT 1")
    fun observeActiveAccount(): Flow<AccountEntity?>

    @Query("SELECT * FROM accounts ORDER BY CASE WHEN accountMode = 'AUTHENTICATED' THEN 0 ELSE 1 END, updatedAt DESC LIMIT 1")
    suspend fun getActiveAccountSync(): AccountEntity?

    @Query("SELECT * FROM accounts WHERE uid = :uid LIMIT 1")
    suspend fun getAccountByUid(uid: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccount(account: AccountEntity)

    @Query("UPDATE accounts SET sessionExpired = :expired, updatedAt = :updatedAt WHERE uid = :uid")
    suspend fun updateSessionExpired(uid: String, expired: Boolean, updatedAt: Long)

    @Query("UPDATE accounts SET displayName = :displayName, updatedAt = :updatedAt WHERE uid = :uid")
    suspend fun updateDisplayName(uid: String, displayName: String, updatedAt: Long)

    @Query("UPDATE accounts SET parentalConsentGranted = :granted, parentEmail = :parentEmail, parentalConsentTimestamp = :consentAt, birthYear = :birthYear, isMinorUnder16 = :isMinor, updatedAt = :updatedAt WHERE uid = :uid")
    suspend fun updateParentalConsent(
        uid: String,
        granted: Boolean,
        parentEmail: String,
        consentAt: Long,
        birthYear: Int?,
        isMinor: Boolean,
        updatedAt: Long
    )

    @Query("DELETE FROM accounts WHERE uid = :uid")
    suspend fun deleteAccountByUid(uid: String)

    @Query("DELETE FROM accounts")
    suspend fun clearAllAccounts()
}
