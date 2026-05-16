package com.example.unisphere.repository

import com.example.unisphere.db.local.dao.HouseDao
import com.example.unisphere.db.local.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

// Modello di comodo per mostrare chi fa cosa questa settimana
data class CleaningRotationalState(
    val serviceId: Int,
    val serviceName: String,
    val assigneeUid: String,
    val assigneeName: String
)

// Modello di comodo per il bilancio Splitwise
data class UserBalance(
    val userUid: String,
    val username: String,
    val netAmount: Double // Positivo = deve ricevere, Negativo = deve dare
)

@Singleton
class HouseRepository @Inject constructor(
    private val houseDao: HouseDao
) {

    // --- GESTIONE CASA E MEMBRI ---

    suspend fun createHouse(houseName: String, creatorUid: String, creatorUsername: String): Int {
        // 1. Passiamo anche l'adminUid richiesto dalla nuova HouseEntity (il creatore diventa admin)
        val houseId = houseDao.insertHouse(
            HouseEntity(name = houseName,adminUid = creatorUid)
        ).toInt()

        // 2. Passiamo null per il profilePictureUri richiesto da HouseMemberEntity
        houseDao.insertMember(
            HouseMemberEntity(houseId = houseId, userUid = creatorUid,
                username = creatorUsername, profilePictureUri = null // Puoi mettere un URL reale se l'utente ha già un avatar
            )
        )
        return houseId
    }
    fun getUserMembership(userId: String): Flow<HouseMemberEntity?> = houseDao.getUserMembership(userId)

    fun getHouseMembers(houseId: Int): Flow<List<HouseMemberEntity>> = houseDao.getHouseMembers(houseId)

    suspend fun removeMemberFromHouse(houseId: Int, userUid: String, username: String) {
        houseDao.removeMember(HouseMemberEntity(houseId, userUid, username))
    }

    // --- GESTIONE INVITI ---

    suspend fun sendInvitation(houseId: Int, houseName: String, senderUsername: String, receiverUid: String) {
        houseDao.insertInvitation(
            HouseInvitationEntity(
                houseId = houseId,
                houseName = houseName,
                senderUsername = senderUsername,
                receiverUid = receiverUid,
                status = "PENDING"
            )
        )
    }

    fun getPendingInvitations(userId: String): Flow<List<HouseInvitationEntity>> = houseDao.getPendingInvitations(userId)

    suspend fun acceptInvitation(invitation: HouseInvitationEntity, username: String) {
        houseDao.updateInvitationStatus(invitation.id, "ACCEPTED")
        houseDao.insertMember(HouseMemberEntity(invitation.houseId, invitation.receiverUid, username))
    }

    suspend fun declineInvitation(invitationId: Int) {
        houseDao.updateInvitationStatus(invitationId, "DECLINED")
    }

    // --- ALGORITMO ROTAZIONE AUTOMATICA PULIZIE ---

    suspend fun addCleaningService(houseId: Int, name: String) {
        houseDao.insertCleaningService(CleaningServiceEntity(houseId = houseId, name = name))
    }

    /**
     * Calcola dinamicamente i turni della settimana corrente senza appesantire il DB.
     * Sfrutta l'indice della settimana dell'anno per ruotare i membri sui servizi.
     */
    fun getWeeklyCleaningRotation(houseId: Int): Flow<List<CleaningRotationalState>> {
        val currentLocalDate = LocalDate.now()
        val weekFields = WeekFields.of(Locale.getDefault())
        // CORREZIONE: Usiamo weekOfYear() per estrarre il campo temporale corretto
        val currentWeek = currentLocalDate.get(weekFields.weekOfYear())

        return combine(
            houseDao.getCleaningServices(houseId),
            houseDao.getHouseMembers(houseId)
        ) { services, members ->
            if (members.isEmpty() || services.isEmpty()) return@combine emptyList()

            // Ordiniamo i membri in modo che la rotazione sia deterministica per tutti
            val sortedMembers = members.sortedBy { it.userUid }

            services.mapIndexed { index, service ->
                // Algoritmo di sfasamento: uniamo l'id del servizio alla settimana dell'anno
                val memberIndex = (index + currentWeek) % sortedMembers.size
                val assignedMember = sortedMembers[memberIndex]

                CleaningRotationalState(
                    serviceId = service.id,
                    serviceName = service.name,
                    assigneeUid = assignedMember.userUid,
                    assigneeName = assignedMember.username
                )
            }
        }
    }

    // --- ALGORITMO BILANCIO SPESE (SPLITWISE CLONE) ---

    suspend fun addGroupExpense(
        houseId: Int,
        title: String,
        totalAmount: Double,
        payerUid: String,
        payerUsername: String,
        splits: List<TransactionSplitEntity>
    ) {
        val transactionId = houseDao.insertGroupTransaction(
            GroupTransactionEntity(
                houseId = houseId,
                title = title,
                amount = totalAmount,
                payerUid = payerUid,
                payerUsername = payerUsername,
                date = LocalDate.now()
            )
        ).toInt()

        // Colleghiamo i relativi splits alla transazione appena inserita
        val splitsWithId = splits.map { it.copy(transactionId = transactionId) }
        houseDao.insertTransactionSplits(splitsWithId)
    }

    fun getGroupTransactions(houseId: Int): Flow<List<GroupTransactionEntity>> = houseDao.getGroupTransactions(houseId)

    /**
     * Calcola lo stato dei debiti e crediti incrociati di tutti i membri della casa
     */
    fun getHouseBalances(houseId: Int): Flow<List<UserBalance>> {
        return combine(
            houseDao.getGroupTransactions(houseId),
            houseDao.getHouseMembers(houseId)
        ) { transactions, members ->
            if (members.isEmpty()) return@combine emptyList()

            // Inizializziamo la mappa dei bilanci a zero per ogni membro
            val balanceMap = members.associate { it.userUid to 0.0 }.toMutableMap()
            val usernameMap = members.associate { it.userUid to it.username }

            transactions.forEach { tx ->
                // Al pagatore spetta un credito pari all'importo totale speso
                val currentPayerBalance = balanceMap[tx.payerUid] ?: 0.0
                balanceMap[tx.payerUid] = currentPayerBalance + tx.amount

                // Recuperiamo i dettagli delle divisioni di questa transazione
                val splits = houseDao.getSplitsForTransaction(tx.id)
                splits.forEach { split ->
                    val currentDebtorBalance = balanceMap[split.userUid] ?: 0.0
                    // Chi fa parte dello split accumula un debito (importo negativo)
                    balanceMap[split.userUid] = currentDebtorBalance - split.amountOwed
                }
            }

            balanceMap.map { (uid, netAmount) ->
                UserBalance(
                    userUid = uid,
                    username = usernameMap[uid] ?: "Utente rimosso",
                    netAmount = netAmount
                )
            }
        }
    }
    suspend fun searchUsers(query: String): List<UserEntity> {
        return houseDao.searchUsersByUsername("%$query%")
    }

    suspend fun deleteCleaningService(serviceId: Int) {
        houseDao.deleteCleaningService(serviceId)
    }

    suspend fun createHouse(houseName: String, creatorUid: String, creatorUsername: String, creatorAvatar: String?): Int {
        // Il creatore diventa automaticamente l'adminUid della casa
        val houseId = houseDao.insertHouse(HouseEntity(name = houseName, adminUid = creatorUid)).toInt()
        houseDao.insertMember(HouseMemberEntity(houseId, creatorUid, creatorUsername, creatorAvatar))
        return houseId
    }

    suspend fun getHouseById(houseId: Int): HouseEntity? = houseDao.getHouseById(houseId)

    suspend fun deleteHouse(houseId: Int) = houseDao.deleteHouse(houseId)

    suspend fun acceptInvitation(invitation: HouseInvitationEntity, username: String, avatarUrl: String?) {
        houseDao.updateInvitationStatus(invitation.id, "ACCEPTED")
        houseDao.insertMember(HouseMemberEntity(invitation.houseId, invitation.receiverUid, username, avatarUrl))
    }

    suspend fun deleteTransaction(transactionId: Int) {
        houseDao.deleteTransactionSplits(transactionId)
        houseDao.deleteTransaction(transactionId)
    }

    suspend fun updateTransaction(transaction: GroupTransactionEntity, newSplits: List<TransactionSplitEntity>) {
        houseDao.updateGroupTransaction(transaction)
        houseDao.deleteTransactionSplits(transaction.id)
        houseDao.insertTransactionSplits(newSplits.map { it.copy(transactionId = transaction.id) })
    }

    // Aggiungi questo modello di comodo in cima al file o dove tieni gli altri modelli
    data class TransactionWithSplits(
        val transaction: GroupTransactionEntity,
        val splits: List<TransactionSplitEntity>
    )

    // Inserisci questo metodo dentro la classe HouseRepository
    fun getGroupTransactionsWithSplits(houseId: Int): Flow<List<TransactionWithSplits>> {
        return houseDao.getGroupTransactions(houseId).map { transactions ->
            val result = mutableListOf<TransactionWithSplits>()
            for (tx in transactions) {
                val splits = houseDao.getSplitsForTransaction(tx.id)
                result.add(TransactionWithSplits(tx, splits))
            }
            result
        }
    }
}