package com.example.unisphere.repository

import com.example.unisphere.db.local.dao.HouseDao
import com.example.unisphere.db.local.dao.UserDao
import com.example.unisphere.db.local.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class CleaningRotationalState(
    val serviceId: Int,
    val serviceName: String,
    val assigneeUid: String,
    val assigneeName: String,
    val isCompleted: Boolean
)

data class UserBalance(
    val userUid: String,
    val username: String,
    val netAmount: Double
)

@Singleton
class HouseRepository @Inject constructor(
    private val houseDao: HouseDao,
    private val userDao: UserDao // INIETTATO: Consente l'accesso pulito e scalabile ai dati reali dell'utente
) {

    // --- NUOVO METODO: Recupera l'utente corrente prendendolo dal flusso atomico di Room ---
    suspend fun getRealUser(userId: String): UserEntity? {
        return userDao.getUserById(userId).first()
    }

    // --- GESTIONE CASA E MEMBRI ---

    suspend fun createHouse(houseName: String, creatorUid: String, creatorUsername: String, creatorAvatar: String?): Int {
        val houseId = houseDao.insertHouse(
            HouseEntity(name = houseName, adminUid = creatorUid)
        ).toInt()

        houseDao.insertMember(
            HouseMemberEntity(
                houseId = houseId,
                userUid = creatorUid,
                username = creatorUsername,
                profilePictureUri = creatorAvatar // SALVATO: Inserisce la foto profilo corretta dell'utente
            )
        )
        return houseId
    }

    fun getUserMembership(userId: String): Flow<HouseMemberEntity?> =
        houseDao.getUserMembership(userId)

    fun getHouseMembers(houseId: Int): Flow<List<HouseMemberEntity>> =
        houseDao.getHouseMembers(houseId)

    suspend fun removeMemberFromHouse(houseId: Int, userUid: String, username: String) {
        houseDao.removeMember(HouseMemberEntity(houseId, userUid, username))
    }

    // --- GESTIONE INVITI ---

    suspend fun sendInvitation(
        houseId: Int,
        houseName: String,
        senderUsername: String,
        receiverUid: String
    ) {
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

    fun getPendingInvitations(userId: String): Flow<List<HouseInvitationEntity>> =
        houseDao.getPendingInvitations(userId)

    suspend fun acceptInvitation(invitation: HouseInvitationEntity, username: String, avatarUrl: String?) {
        houseDao.updateInvitationStatus(invitation.id, "ACCEPTED")
        houseDao.insertMember(
            HouseMemberEntity(
                invitation.houseId,
                invitation.receiverUid,
                username,
                avatarUrl // SALVATO: Assicura l'inserimento dell'immagine anche accettando l'invito
            )
        )
    }

    suspend fun declineInvitation(invitationId: Int) {
        houseDao.updateInvitationStatus(invitationId, "DECLINED")
    }

    // --- ALGORITMO ROTAZIONE AUTOMATICA PULIZIE ---

    suspend fun addCleaningService(houseId: Int, name: String) {
        houseDao.insertCleaningService(CleaningServiceEntity(houseId = houseId, name = name))
    }

    fun getWeeklyCleaningRotation(houseId: Int): Flow<List<CleaningRotationalState>> {
        val currentLocalDate = LocalDate.now()
        val weekFields = WeekFields.of(Locale.getDefault())
        val currentWeek = currentLocalDate.get(weekFields.weekOfYear())

        return combine(
            houseDao.getCleaningServices(houseId),
            houseDao.getHouseMembers(houseId)
        ) { services, members ->
            if (members.isEmpty() || services.isEmpty()) return@combine emptyList()

            val sortedMembers = members.sortedBy { it.userUid }

            services.mapIndexed { index, service ->
                val memberIndex = (index + currentWeek) % sortedMembers.size
                val assignedMember = sortedMembers[memberIndex]

                CleaningRotationalState(
                    serviceId = service.id,
                    serviceName = service.name,
                    assigneeUid = assignedMember.userUid,
                    assigneeName = assignedMember.username,
                    isCompleted = service.isCompleted
                )
            }
        }
    }

    // --- ALGORITMO BILANCIO CONDIVISO ---

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

        val splitsWithId = splits.map { it.copy(transactionId = transactionId) }
        houseDao.insertTransactionSplits(splitsWithId)
    }

    fun getGroupTransactions(houseId: Int): Flow<List<GroupTransactionEntity>> =
        houseDao.getGroupTransactions(houseId)

    fun getHouseBalances(houseId: Int): Flow<List<UserBalance>> {
        return combine(
            houseDao.getGroupTransactions(houseId),
            houseDao.getHouseMembers(houseId)
        ) { transactions, members ->
            if (members.isEmpty()) return@combine emptyList()

            val balanceMap = members.associate { it.userUid to 0.0 }.toMutableMap()
            val usernameMap = members.associate { it.userUid to it.username }

            transactions.forEach { tx ->
                val currentPayerBalance = balanceMap[tx.payerUid] ?: 0.0
                balanceMap[tx.payerUid] = currentPayerBalance + tx.amount

                val splits = houseDao.getSplitsForTransaction(tx.id)
                splits.forEach { split ->
                    val currentDebtorBalance = balanceMap[split.userUid] ?: 0.0
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

    suspend fun getHouseById(houseId: Int): HouseEntity? = houseDao.getHouseById(houseId)

    suspend fun deleteHouse(houseId: Int) = houseDao.deleteHouse(houseId)

    suspend fun deleteTransaction(transactionId: Int) {
        houseDao.deleteTransactionSplits(transactionId)
        houseDao.deleteTransaction(transactionId)
    }

    suspend fun updateTransaction(
        transaction: GroupTransactionEntity,
        newSplits: List<TransactionSplitEntity>
    ) {
        houseDao.updateGroupTransaction(transaction)
        houseDao.deleteTransactionSplits(transaction.id)
        houseDao.insertTransactionSplits(newSplits.map { it.copy(transactionId = transaction.id) })
    }

    data class TransactionWithSplits(
        val transaction: GroupTransactionEntity,
        val splits: List<TransactionSplitEntity>
    )

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

    suspend fun toggleCleaningServiceCompletion(serviceId: Int) {
        houseDao.toggleServiceCompletion(serviceId)
    }
}