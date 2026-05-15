package com.example.unisphere.db.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "transaction_categories")
data class TransactionCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val colorHex: String,
    val userId: String // Associa la categoria all'utente loggato
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = TransactionCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE // Se elimini la categoria, elimina le transazioni collegate
        )
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userUid: String, // Associa la transazione all'utente loggato
    val title: String,
    val amount: Double,
    val categoryId: Int, // Chiave esterna relazionale
    val date: LocalDate,
    val isIncome: Boolean
)