package com.example.unisphere.db.local.dao

import androidx.room.*
import com.example.unisphere.db.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HouseDao {

    // --- LOGICA CASA E MEMBRI ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHouse(house: HouseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: HouseMemberEntity)

    @Delete
    suspend fun removeMember(member: HouseMemberEntity)

    @Query("SELECT * FROM house_members WHERE userUid = :userId LIMIT 1")
    fun getUserMembership(userId: String): Flow<HouseMemberEntity?>

    @Query("SELECT * FROM house_members WHERE houseId = :houseId")
    fun getHouseMembers(houseId: Int): Flow<List<HouseMemberEntity>>


    // --- LOGICA INVITI ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvitation(invitation: HouseInvitationEntity)

    @Query("SELECT * FROM house_invitations WHERE receiverUid = :userId AND status = 'PENDING'")
    fun getPendingInvitations(userId: String): Flow<List<HouseInvitationEntity>>

    @Query("UPDATE house_invitations SET status = :status WHERE id = :invitationId")
    suspend fun updateInvitationStatus(invitationId: Int, status: String)


    // --- LOGICA PULIZIE ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCleaningService(service: CleaningServiceEntity)

    @Query("SELECT * FROM cleaning_services WHERE houseId = :houseId")
    fun getCleaningServices(houseId: Int): Flow<List<CleaningServiceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCleaningAssignments(assignments: List<CleaningAssignmentEntity>)

    @Query("SELECT * FROM cleaning_assignments WHERE serviceId = :serviceId AND weekOfYear = :week AND year = :year LIMIT 1")
    fun getAssignmentForService(serviceId: Int, week: Int, year: Int): Flow<CleaningAssignmentEntity?>


    // --- LOGICA SPESE CONDIVISE (SPLITWISE) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupTransaction(transaction: GroupTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionSplits(splits: List<TransactionSplitEntity>)

    @Query("SELECT * FROM group_transactions WHERE houseId = :houseId ORDER BY date DESC")
    fun getGroupTransactions(houseId: Int): Flow<List<GroupTransactionEntity>>

    @Query("SELECT * FROM transaction_splits WHERE transactionId = :transactionId")
    suspend fun getSplitsForTransaction(transactionId: Int): List<TransactionSplitEntity>

    // Cerca gli utenti registrati nel DB per proporli nella ricerca coinquilini
    @Query("SELECT * FROM users WHERE username LIKE :query")
    suspend fun searchUsersByUsername(query: String): List<UserEntity>

    // Elimina un servizio di pulizia specifico (cancella a cascata anche i turni di quella settimana)
    @Query("DELETE FROM cleaning_services WHERE id = :serviceId")
    suspend fun deleteCleaningService(serviceId: Int)

    @Query("SELECT * FROM houses WHERE id = :houseId LIMIT 1")
    suspend fun getHouseById(houseId: Int): HouseEntity?

    @Query("DELETE FROM houses WHERE id = :houseId")
    suspend fun deleteHouse(houseId: Int)

    @Query("DELETE FROM group_transactions WHERE id = :transactionId")
    suspend fun deleteTransaction(transactionId: Int)

    @Query("DELETE FROM transaction_splits WHERE transactionId = :transactionId")
    suspend fun deleteTransactionSplits(transactionId: Int)

    @Update
    suspend fun updateGroupTransaction(transaction: GroupTransactionEntity)
}