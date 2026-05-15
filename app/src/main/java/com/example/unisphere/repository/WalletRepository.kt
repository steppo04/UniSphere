package com.example.unisphere.repository

import com.example.unisphere.db.local.dao.WalletDao
import com.example.unisphere.db.local.entity.TransactionCategoryEntity
import com.example.unisphere.db.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class WalletRepository @Inject constructor(
    private val walletDao: WalletDao
) {
    fun getTransactions(userId: String): Flow<List<TransactionEntity>> = walletDao.getTransactionsForUser(userId)
    suspend fun saveTransaction(transaction: TransactionEntity) = walletDao.saveTransaction(transaction)
    suspend fun deleteTransaction(transaction: TransactionEntity) = walletDao.deleteTransaction(transaction)

    fun getCategories(userId: String): Flow<List<TransactionCategoryEntity>> = walletDao.getCategoriesForUser(userId)
    suspend fun saveCategory(category: TransactionCategoryEntity) = walletDao.saveCategory(category)
    suspend fun deleteCategory(category: TransactionCategoryEntity) = walletDao.deleteCategory(category)
}