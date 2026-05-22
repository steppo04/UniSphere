package com.example.unisphere.db.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.LocalDate

// 1. LA CASA
@Entity(tableName = "houses")
data class HouseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val adminUid: String // <--- NUOVO: Traccia l'ID dell'amministratore/creatore della casa
)

// 2. I MEMBRI DELLA CASA (Tabella di giunzione Relazionale)
@Entity(
    tableName = "house_members",
    primaryKeys = ["houseId", "userUid"],
    foreignKeys = [
        ForeignKey(entity = HouseEntity::class, parentColumns = ["id"], childColumns = ["houseId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class HouseMemberEntity(
    val houseId: Int,
    val userUid: String,
    val username: String,
    val profilePictureUri: String? = null // <--- NUOVO: Memorizza l'avatar per la visualizzazione sincrona
)

// 3. GLI INVITI COINQUILINI
@Entity(
    tableName = "house_invitations",
    foreignKeys = [
        ForeignKey(entity = HouseEntity::class, parentColumns = ["id"], childColumns = ["houseId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class HouseInvitationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val houseId: Int,
    val houseName: String,
    val senderUsername: String,
    val receiverUid: String,
    val status: String // "PENDING", "ACCEPTED", "DECLINED"
)

// 4. I SERVIZI DI PULIZIA (es. "Cucina", "Bagno")
@Entity(
    tableName = "cleaning_services",
    foreignKeys = [
        ForeignKey(entity = HouseEntity::class, parentColumns = ["id"], childColumns = ["houseId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class CleaningServiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val houseId: Int,
    val name: String,
    val isCompleted: Boolean = false
)

// 5. I TURNI ASSEGNATI PER SETTIMANA
@Entity(
    tableName = "cleaning_assignments",
    foreignKeys = [
        ForeignKey(entity = CleaningServiceEntity::class, parentColumns = ["id"], childColumns = ["serviceId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class CleaningAssignmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val serviceId: Int,
    val userUid: String,
    val username: String,
    val weekOfYear: Int, // Usiamo la settimana e l'anno per calcolare la rotazione automatica
    val year: Int,
    val isCompleted: Boolean
)

// 6. TRANSAZIONI CONDIVISE (Splitwise Base)
@Entity(
    tableName = "group_transactions",
    foreignKeys = [
        ForeignKey(entity = HouseEntity::class, parentColumns = ["id"], childColumns = ["houseId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class GroupTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val houseId: Int,
    val title: String,
    val amount: Double,
    val payerUid: String,
    val payerUsername: String,
    val date: LocalDate
)

// 7. QUOTE DI DIVISIONE DELLA TRANSAZIONE
@Entity(
    tableName = "transaction_splits",
    foreignKeys = [
        ForeignKey(entity = GroupTransactionEntity::class, parentColumns = ["id"], childColumns = ["transactionId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class TransactionSplitEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val transactionId: Int,
    val userUid: String, // Coinquilino che deve i soldi
    val username: String,
    val amountOwed: Double // Quota spettante
)