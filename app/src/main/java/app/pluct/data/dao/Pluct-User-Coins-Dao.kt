package app.pluct.data.dao

import androidx.room.*
import app.pluct.data.entity.UserCoins
import app.pluct.data.entity.CoinTransaction
import kotlinx.coroutines.flow.Flow

/**
 * DAO for user coins and transactions
 */
@Dao
interface UserCoinsDao {
    
    @Query("SELECT * FROM user_coins WHERE userId = :userId")
    suspend fun getUserCoins(userId: String): UserCoins?
    
    @Query("SELECT * FROM user_coins WHERE userId = :userId")
    fun getUserCoinsFlow(userId: String): Flow<UserCoins?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserCoins(userCoins: UserCoins)
    
    @Query("UPDATE user_coins SET coins = :newAmount, lastUpdated = :timestamp WHERE userId = :userId")
    suspend fun updateCoins(userId: String, newAmount: Int, timestamp: Long)
    
    @Query("SELECT * FROM coin_transactions WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getTransactions(userId: String): List<CoinTransaction>
    
    @Query("SELECT * FROM coin_transactions WHERE userId = :userId ORDER BY timestamp DESC")
    fun getTransactionsFlow(userId: String): Flow<List<CoinTransaction>>
    
    @Insert
    suspend fun insertTransaction(transaction: CoinTransaction)
    
    @Query("DELETE FROM coin_transactions WHERE userId = :userId")
    suspend fun clearTransactions(userId: String)
}
