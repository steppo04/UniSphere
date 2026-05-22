package com.example.unisphere.ui.screen.uniHome

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unisphere.db.SupabaseClient
import com.example.unisphere.db.local.entity.*
import com.example.unisphere.repository.CleaningRotationalState
import com.example.unisphere.repository.HouseRepository
import com.example.unisphere.repository.UserBalance
import com.example.unisphere.repository.HouseRepository.TransactionWithSplits
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UniHomeState(
    val isLoading: Boolean = true,
    val hasHouse: Boolean = false,
    val houseId: Int? = null,
    val houseName: String = "",
    val adminUid: String = "",
    val currentUserId: String = "",

    // STATO UTENTE GLOBALE REALE
    val currentUsername: String = "",
    val currentUserAvatar: String? = null,

    val members: List<HouseMemberEntity> = emptyList(),
    val pendingInvitations: List<HouseInvitationEntity> = emptyList(),
    val cleaningRotations: List<CleaningRotationalState> = emptyList(),
    val balances: List<UserBalance> = emptyList(),
    val transactionsWithSplits: List<TransactionWithSplits> = emptyList(),

    val searchedUsers: List<UserEntity> = emptyList(),
    val snackbarMessage: String? = null,
    val isSuccessSnackbar: Boolean = false,

    val newHouseName: String = "",
    val inviteUserQuery: String = "",
    val newServiceName: String = "",
    val newExpenseTitle: String = "",
    val newExpenseAmount: String = ""
)

sealed interface UniHomeAction {
    data class OnNewHouseNameChanged(val name: String) : UniHomeAction
    object OnCreateHouseClicked : UniHomeAction
    object OnDeleteHouseClicked : UniHomeAction

    data class OnInviteQueryChanged(val query: String) : UniHomeAction
    data class OnSelectUserToInvite(val user: UserEntity) : UniHomeAction
    object OnDismissSnackbar : UniHomeAction

    data class OnAcceptInvitation(val invitation: HouseInvitationEntity) : UniHomeAction
    data class OnDeclineInvitation(val invitationId: Int) : UniHomeAction
    data class OnRemoveMember(val member: HouseMemberEntity) : UniHomeAction
    object OnLeaveHouseClicked : UniHomeAction

    data class OnNewServiceNameChanged(val name: String) : UniHomeAction
    object OnAddCleaningServiceClicked : UniHomeAction
    data class OnDeleteServiceClicked(val serviceId: Int) : UniHomeAction

    data class OnToggleServiceCompleted(val serviceId: Int) : UniHomeAction

    data class OnNewExpenseTitleChanged(val title: String) : UniHomeAction
    data class OnNewExpenseAmountChanged(val amount: String) : UniHomeAction
    data class OnAddExpenseClicked(val selectedSplitMembers: List<HouseMemberEntity>) : UniHomeAction
    data class OnSettleDebtClicked(val targetUserUid: String, val targetUsername: String, val amount: Double) : UniHomeAction
    data class OnDeleteTransactionClicked(val txId: Int) : UniHomeAction
}

@HiltViewModel
class UniHomeViewModel @Inject constructor(
    application: Application,
    private val repository: HouseRepository
) : AndroidViewModel(application) {

    var state by mutableStateOf(UniHomeState(currentUserId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: "default_user"))
        private set

    private val currentUserId = state.currentUserId

    private var houseDataJobs: List<Job> = emptyList()
    private var searchJob: Job? = null

    init {
        loadCurrentUserData()
        observeUserMembership()
        observePendingInvitations()
    }

    private fun loadCurrentUserData() {
        viewModelScope.launch {
            val user = repository.getRealUser(currentUserId)
            if (user != null) {
                state = state.copy(
                    currentUsername = user.username,
                    currentUserAvatar = user.profilePictureUri
                )
            }
        }
    }

    private fun observeUserMembership() {
        viewModelScope.launch {
            repository.getUserMembership(currentUserId).collectLatest { membership ->
                if (membership != null) {
                    val houseInfo = repository.getHouseById(membership.houseId)
                    state = state.copy(
                        hasHouse = true,
                        houseId = membership.houseId,
                        houseName = houseInfo?.name ?: "La mia Casa",
                        adminUid = houseInfo?.adminUid ?: "",
                        isLoading = false
                    )
                    observeHouseData(membership.houseId)
                } else {
                    cancelHouseObservations()
                    state = state.copy(hasHouse = false, houseId = null, adminUid = "", isLoading = false)
                }
            }
        }
    }

    private fun observePendingInvitations() {
        viewModelScope.launch {
            repository.getPendingInvitations(currentUserId).collectLatest { invites ->
                state = state.copy(pendingInvitations = invites)
            }
        }
    }

    private fun observeHouseData(houseId: Int) {
        cancelHouseObservations()

        // 1. MEMBRI - Scavalcamento DB Vecchio
        val membersJob = viewModelScope.launch {
            repository.getHouseMembers(houseId).collectLatest { listaMembri ->
                val realMembers = listaMembri.map { member ->
                    val realUser = repository.getRealUser(member.userUid)
                    if (realUser != null) {
                        member.copy(
                            username = realUser.username,
                            profilePictureUri = realUser.profilePictureUri
                        )
                    } else member
                }

                val mioProfilo = realMembers.find { it.userUid == currentUserId }
                state = state.copy(
                    members = realMembers,
                    currentUsername = mioProfilo?.username ?: state.currentUsername,
                    currentUserAvatar = mioProfilo?.profilePictureUri ?: state.currentUserAvatar
                )
            }
        }

        // 2. TURNI PULIZIA - Scavalcamento DB Vecchio
        val cleaningJob = viewModelScope.launch {
            repository.getWeeklyCleaningRotation(houseId).collectLatest { rotations ->
                val realRotations = rotations.map { rot ->
                    val realUser = repository.getRealUser(rot.assigneeUid)
                    if (realUser != null) rot.copy(assigneeName = realUser.username) else rot
                }
                state = state.copy(cleaningRotations = realRotations)
            }
        }

        // 3. BILANCI - Scavalcamento DB Vecchio
        val balanceJob = viewModelScope.launch {
            repository.getHouseBalances(houseId).collectLatest { balances ->
                val realBalances = balances.map { bal ->
                    val realUser = repository.getRealUser(bal.userUid)
                    if (realUser != null) bal.copy(username = realUser.username) else bal
                }
                state = state.copy(balances = realBalances)
            }
        }

        // 4. TRANSAZIONI E QUOTE - Scavalcamento DB Vecchio
        val transactionsJob = viewModelScope.launch {
            repository.getGroupTransactionsWithSplits(houseId).collectLatest { txList ->
                val realTxList = txList.map { item ->
                    val realPayer = repository.getRealUser(item.transaction.payerUid)
                    val updatedTx = if (realPayer != null) item.transaction.copy(payerUsername = realPayer.username) else item.transaction

                    val updatedSplits = item.splits.map { split ->
                        val realSplitter = repository.getRealUser(split.userUid)
                        if (realSplitter != null) split.copy(username = realSplitter.username) else split
                    }
                    item.copy(transaction = updatedTx, splits = updatedSplits)
                }
                state = state.copy(transactionsWithSplits = realTxList)
            }
        }

        houseDataJobs = listOf(membersJob, cleaningJob, balanceJob, transactionsJob)
    }

    private fun cancelHouseObservations() {
        houseDataJobs.forEach { it.cancel() }
        houseDataJobs = emptyList()
    }

    private fun areAllBalancesEven(): Boolean {
        return state.balances.all { Math.abs(it.netAmount) < 0.01 }
    }

    fun onAction(action: UniHomeAction) {
        when (action) {
            is UniHomeAction.OnNewHouseNameChanged -> state = state.copy(newHouseName = action.name)
            UniHomeAction.OnCreateHouseClicked -> {
                if (state.newHouseName.isNotBlank()) {
                    viewModelScope.launch {
                        val creatorName = state.currentUsername.ifBlank { "Tu" }
                        repository.createHouse(state.newHouseName, currentUserId, creatorName, state.currentUserAvatar)
                        state = state.copy(newHouseName = "")
                    }
                }
            }
            UniHomeAction.OnDeleteHouseClicked -> {
                val houseId = state.houseId ?: return
                if (currentUserId != state.adminUid) return
                viewModelScope.launch {
                    if (areAllBalancesEven()) {
                        repository.deleteHouse(houseId)
                        state = state.copy(snackbarMessage = "Casa eliminata definitivamente.", isSuccessSnackbar = true)
                    } else {
                        state = state.copy(snackbarMessage = "Impossibile eliminare la casa: ci sono conti in sospeso!", isSuccessSnackbar = false)
                    }
                }
            }
            is UniHomeAction.OnInviteQueryChanged -> {
                state = state.copy(inviteUserQuery = action.query)
                searchJob?.cancel()
                searchJob = viewModelScope.launch {
                    if (action.query.length >= 2) {
                        val results = repository.searchUsers(action.query).filter { it.uid != currentUserId }
                        state = state.copy(searchedUsers = results)
                    } else {
                        state = state.copy(searchedUsers = emptyList())
                    }
                }
            }
            is UniHomeAction.OnSelectUserToInvite -> {
                val houseId = state.houseId ?: return
                viewModelScope.launch {
                    repository.sendInvitation(houseId, state.houseName, state.currentUsername, action.user.uid)
                    state = state.copy(inviteUserQuery = "", searchedUsers = emptyList(), snackbarMessage = "Invito inviato a ${action.user.username}!", isSuccessSnackbar = true)
                }
            }
            UniHomeAction.OnDismissSnackbar -> state = state.copy(snackbarMessage = null)
            is UniHomeAction.OnAcceptInvitation -> viewModelScope.launch { repository.acceptInvitation(action.invitation, state.currentUsername, state.currentUserAvatar) }
            is UniHomeAction.OnDeclineInvitation -> viewModelScope.launch { repository.declineInvitation(action.invitationId) }

            is UniHomeAction.OnRemoveMember -> {
                val houseId = state.houseId ?: return
                val memberBalance = state.balances.find { it.userUid == action.member.userUid }?.netAmount ?: 0.0
                viewModelScope.launch {
                    if (Math.abs(memberBalance) < 0.01) {
                        repository.removeMemberFromHouse(houseId, action.member.userUid, action.member.username)
                        state = state.copy(snackbarMessage = "${action.member.username} è stato rimosso dalla casa.", isSuccessSnackbar = true)
                    } else {
                        state = state.copy(snackbarMessage = "Impossibile rimuovere ${action.member.username}: i suoi conti non sono in pari!", isSuccessSnackbar = false)
                    }
                }
            }
            UniHomeAction.OnLeaveHouseClicked -> {
                val houseId = state.houseId ?: return
                val myBalance = state.balances.find { it.userUid == currentUserId }?.netAmount ?: 0.0
                viewModelScope.launch {
                    if (Math.abs(myBalance) < 0.01) {
                        repository.removeMemberFromHouse(houseId, currentUserId, state.currentUsername)
                        state = state.copy(snackbarMessage = "Sei uscito dalla casa.", isSuccessSnackbar = true)
                    } else {
                        state = state.copy(snackbarMessage = "Non puoi uscire dalla casa se il tuo bilancio non è in pari (0.00€)!", isSuccessSnackbar = false)
                    }
                }
            }
            is UniHomeAction.OnNewServiceNameChanged -> state = state.copy(newServiceName = action.name)
            UniHomeAction.OnAddCleaningServiceClicked -> {
                val houseId = state.houseId ?: return
                if (state.newServiceName.isNotBlank()) {
                    viewModelScope.launch {
                        repository.addCleaningService(houseId, state.newServiceName)
                        state = state.copy(newServiceName = "")
                    }
                }
            }
            is UniHomeAction.OnDeleteServiceClicked -> viewModelScope.launch { repository.deleteCleaningService(action.serviceId) }

            is UniHomeAction.OnToggleServiceCompleted -> {
                viewModelScope.launch {
                    repository.toggleCleaningServiceCompletion(action.serviceId)
                    state = state.copy(snackbarMessage = "Stato della pulizia aggiornato.", isSuccessSnackbar = true)
                }
            }

            is UniHomeAction.OnNewExpenseTitleChanged -> state = state.copy(newExpenseTitle = action.title)
            is UniHomeAction.OnNewExpenseAmountChanged -> state = state.copy(newExpenseAmount = action.amount)
            is UniHomeAction.OnAddExpenseClicked -> {
                val houseId = state.houseId ?: return
                val totalAmount = state.newExpenseAmount.toDoubleOrNull() ?: 0.0
                if (state.newExpenseTitle.isNotBlank() && totalAmount > 0.0 && action.selectedSplitMembers.isNotEmpty()) {
                    viewModelScope.launch {
                        val dividedAmount = totalAmount / action.selectedSplitMembers.size
                        val splits = action.selectedSplitMembers.map { member ->
                            TransactionSplitEntity(transactionId = 0, userUid = member.userUid, username = member.username, amountOwed = dividedAmount)
                        }
                        repository.addGroupExpense(houseId, state.newExpenseTitle, totalAmount, currentUserId, state.currentUsername, splits)
                        state = state.copy(newExpenseTitle = "", newExpenseAmount = "", isSuccessSnackbar = true)
                    }
                }
            }
            is UniHomeAction.OnSettleDebtClicked -> {
                val houseId = state.houseId ?: return
                viewModelScope.launch {
                    val singleSplit = listOf(
                        TransactionSplitEntity(transactionId = 0, userUid = action.targetUserUid, username = action.targetUsername, amountOwed = action.amount)
                    )
                    repository.addGroupExpense(
                        houseId = houseId,
                        title = "Saldatura: ${state.currentUsername} ➔ ${action.targetUsername}",
                        totalAmount = action.amount,
                        payerUid = currentUserId,
                        payerUsername = state.currentUsername,
                        splits = singleSplit
                    )
                    state = state.copy(snackbarMessage = "Debito saldato correttamente con ${action.targetUsername}!", isSuccessSnackbar = true)
                }
            }
            is UniHomeAction.OnDeleteTransactionClicked -> {
                viewModelScope.launch {
                    repository.deleteTransaction(action.txId)
                    state = state.copy(snackbarMessage = "Spesa eliminata, conti ricalcolati.", isSuccessSnackbar = true)
                }
            }
        }
    }
}