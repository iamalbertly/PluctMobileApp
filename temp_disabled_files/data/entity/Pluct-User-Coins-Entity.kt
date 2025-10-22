package app.pluct.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "user_coins")
data class UserCoins(
    @PrimaryKey
    val userId: String = "default_user",
    val coins: Int = 0,
    val totalPurchased: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "coin_transactions")
data class CoinTransaction(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val amount: Int,
    val type: TransactionType,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class TransactionType {
    PURCHASE, USAGE, BONUS, REFUND
}
