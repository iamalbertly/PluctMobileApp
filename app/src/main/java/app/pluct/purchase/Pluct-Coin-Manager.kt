package app.pluct.purchase

import android.util.Log
import app.pluct.data.dao.UserCoinsDao
import app.pluct.data.entity.UserCoins
import app.pluct.data.entity.CoinTransaction
import app.pluct.data.entity.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages user coins and transactions
 */
@Singleton
class PluctCoinManager @Inject constructor(
    private val userCoinsDao: UserCoinsDao
) {
    companion object {
        private const val TAG = "CoinManager"
        private const val DEFAULT_USER_ID = "default_user"
        private const val AI_ANALYSIS_COST = 1
        private const val WELCOME_BONUS = 3
    }
    
    /**
     * Get current coin balance for user
     */
    suspend fun getCoinBalance(userId: String = DEFAULT_USER_ID): Int {
        val userCoins = userCoinsDao.getUserCoins(userId)
        return userCoins?.coins ?: 0
    }
    
    /**
     * Get coin balance as Flow for reactive updates
     */
    fun getCoinBalanceFlow(userId: String = DEFAULT_USER_ID): Flow<Int> {
        return userCoinsDao.getUserCoinsFlow(userId).map { userCoins ->
            userCoins?.coins ?: 0
        }
    }
    
    /**
     * Check if user has enough coins for AI Analysis
     */
    suspend fun canAffordAIAnalysis(userId: String = DEFAULT_USER_ID): Boolean {
        val balance = getCoinBalance(userId)
        return balance >= AI_ANALYSIS_COST
    }
    
    /**
     * Deduct coins for AI Analysis
     */
    suspend fun spendCoinsForAIAnalysis(userId: String = DEFAULT_USER_ID): Boolean {
        return try {
            val currentBalance = getCoinBalance(userId)
            if (currentBalance < AI_ANALYSIS_COST) {
                Log.w(TAG, "Insufficient coins for AI Analysis. Current: $currentBalance, Required: $AI_ANALYSIS_COST")
                false
            } else {
                val newBalance = currentBalance - AI_ANALYSIS_COST
                userCoinsDao.updateCoins(userId, newBalance, System.currentTimeMillis())
                
                // Record transaction
                val transaction = CoinTransaction(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    amount = -AI_ANALYSIS_COST,
                    type = TransactionType.USAGE,
                    description = "AI Analysis"
                )
                userCoinsDao.insertTransaction(transaction)
                
                Log.i(TAG, "Spent $AI_ANALYSIS_COST coins for AI Analysis. New balance: $newBalance")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error spending coins for AI Analysis", e)
            false
        }
    }
    
    /**
     * Add coins to user account
     */
    suspend fun addCoins(userId: String = DEFAULT_USER_ID, amount: Int, description: String = "Coin purchase"): Boolean {
        return try {
            val currentBalance = getCoinBalance(userId)
            val newBalance = currentBalance + amount
            
            // Update or create user coins record
            val userCoins = UserCoins(
                userId = userId,
                coins = newBalance,
                totalPurchased = if (currentBalance == 0) amount else amount, // Simplified logic
                lastUpdated = System.currentTimeMillis()
            )
            userCoinsDao.upsertUserCoins(userCoins)
            
            // Record transaction
            val transaction = CoinTransaction(
                id = UUID.randomUUID().toString(),
                userId = userId,
                amount = amount,
                type = TransactionType.PURCHASE,
                description = description
            )
            userCoinsDao.insertTransaction(transaction)
            
            Log.i(TAG, "Added $amount coins. New balance: $newBalance")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding coins", e)
            false
        }
    }
    
    /**
     * Give welcome bonus to new users
     */
    suspend fun giveWelcomeBonus(userId: String = DEFAULT_USER_ID): Boolean {
        return try {
            val currentBalance = getCoinBalance(userId)
            if (currentBalance == 0) {
                addCoins(userId, WELCOME_BONUS, "Welcome bonus")
                Log.i(TAG, "Gave welcome bonus of $WELCOME_BONUS coins to new user")
                true
            } else {
                Log.d(TAG, "User already has coins, no welcome bonus needed")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error giving welcome bonus", e)
            false
        }
    }
    
    /**
     * Get transaction history
     */
    fun getTransactionHistory(userId: String = DEFAULT_USER_ID): Flow<List<CoinTransaction>> {
        return userCoinsDao.getTransactionsFlow(userId)
    }
    
    /**
     * Get available coin packages for purchase
     */
    fun getCoinPackages(): List<CoinPackage> {
        return listOf(
            CoinPackage(id = "small", name = "Starter Pack", coins = 5, price = 0.99, description = "Perfect for trying out AI Analysis"),
            CoinPackage(id = "medium", name = "Value Pack", coins = 15, price = 2.99, description = "Great value for regular users"),
            CoinPackage(id = "large", name = "Pro Pack", coins = 50, price = 8.99, description = "Best value for power users"),
            CoinPackage(id = "mega", name = "Mega Pack", coins = 100, price = 15.99, description = "Maximum value for heavy users")
        )
    }
}

/**
 * Coin package for purchase
 */
data class CoinPackage(
    val id: String,
    val name: String,
    val coins: Int,
    val price: Double,
    val description: String
)
