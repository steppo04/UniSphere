package com.example.unisphere.db.local.dao


import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.unisphere.db.local.entity.TransactionCategoryEntity
import com.example.unisphere.db.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {

    // --- TRANSAZIONI ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE userUid = :userId")
    fun getTransactionsForUser(userId: String): Flow<List<TransactionEntity>>

    // --- CATEGORIE ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCategory(category: TransactionCategoryEntity)

    @Delete
    suspend fun deleteCategory(category: TransactionCategoryEntity)

    @Query("SELECT * FROM transaction_categories WHERE userId = :userId")
    fun getCategoriesForUser(userId: String): Flow<List<TransactionCategoryEntity>>
}