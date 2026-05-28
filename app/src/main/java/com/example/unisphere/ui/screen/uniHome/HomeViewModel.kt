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
    val newExpenseAmount: String = "",
    val showInviteDialog: Boolean = false,
    val showServiceDialog: Boolean = false,
    val showExpenseDialog: Boolean = false,
    val showSettleDialog: Boolean = false,
    val showAllTxDetailsPage: Boolean = false,
    val txToDeleteId: Int? = null,
    val showDeleteHouseConfirm: Boolean = false,
    val showLeaveHouseConfirm: Boolean = false,
    val selectedMembersForSplit: List<HouseMemberEntity> = emptyList(),
    val selectedSettleDebtor: UserBalance? = null,
    val settleAmountText: String = ""
)

sealed interface UniHomeAction {
    data class OnNewHouseNameChanged(val name: String) : UniHomeAction
    data object OnCreateHouseClicked : UniHomeAction
    data object OnDeleteHouseClicked : UniHomeAction

    data class OnInviteQueryChanged(val query: String) : UniHomeAction
    data class OnSelectUserToInvite(val user: UserEntity) : UniHomeAction
    data object OnDismissSnackbar : UniHomeAction

    data class OnAcceptInvitation(val invitation: HouseInvitationEntity) : UniHomeAction
    data class OnDeclineInvitation(val invitationId: Int) : UniHomeAction
    data class OnRemoveMember(val member: HouseMemberEntity) : UniHomeAction
    data object OnLeaveHouseClicked : UniHomeAction

    data class OnNewServiceNameChanged(val name: String) : UniHomeAction
    data object OnAddCleaningServiceClicked : UniHomeAction
    data class OnDeleteServiceClicked(val serviceId: Int) : UniHomeAction
    data class OnToggleServiceCompleted(val serviceId: Int) : UniHomeAction

    data class OnNewExpenseTitleChanged(val title: String) : UniHomeAction
    data class OnNewExpenseAmountChanged(val amount: String) : UniHomeAction
    data class OnToggleMemberSplitSelection(val member: HouseMemberEntity) : UniHomeAction
    data object OnAddExpenseClicked : UniHomeAction

    data class OnSettleDebtorSelected(val balance: UserBalance) : UniHomeAction
    data class OnSettleAmountTextChanged(val amount: String) : UniHomeAction
    data object OnSettleDebtClicked : UniHomeAction

    data class OnRequestDeleteTransaction(val txId: Int?) : UniHomeAction
    data object OnConfirmDeleteTransaction : UniHomeAction

    data class OnToggleInviteDialog(val show: Boolean) : UniHomeAction
    data class OnToggleServiceDialog(val show: Boolean) : UniHomeAction
    data class OnToggleExpenseDialog(val show: Boolean) : UniHomeAction
    data class OnToggleSettleDialog(val show: Boolean) : UniHomeAction
    data class OnToggleAllTxDetailsPage(val show: Boolean) : UniHomeAction
    data class OnToggleDeleteHouseConfirm(val show: Boolean) : UniHomeAction
    data class OnToggleLeaveHouseConfirm(val show: Boolean) : UniHomeAction
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

        val membersJob = viewModelScope.launch {
            repository.getHouseMembers(houseId).collectLatest { listaMembri ->
                val realMembers = listaMembri.map { member ->
                    val realUser = repository.getRealUser(member.userUid)
                    if (realUser != null) {
                        member.copy(username = realUser.username, profilePictureUri = realUser.profilePictureUri)
                    } else member
                }

                val mioProfilo = realMembers.find { it.userUid == currentUserId }
                state = state.copy(
                    members = realMembers,
                    currentUsername = mioProfilo?.username ?: state.currentUsername,
                    currentUserAvatar = mioProfilo?.profilePictureUri ?: state.currentUserAvatar,
                    selectedMembersForSplit = realMembers
                )
            }
        }

        val cleaningJob = viewModelScope.launch {
            repository.getWeeklyCleaningRotation(houseId).collectLatest { rotations ->
                val realRotations = rotations.map { rot ->
                    val realUser = repository.getRealUser(rot.assigneeUid)
                    if (realUser != null) rot.copy(assigneeName = realUser.username) else rot
                }
                state = state.copy(cleaningRotations = realRotations)
            }
        }

        val balanceJob = viewModelScope.launch {
            repository.getHouseBalances(houseId).collectLatest { balances ->
                val realBalances = balances.map { bal ->
                    val realUser = repository.getRealUser(bal.userUid)
                    if (realUser != null) bal.copy(username = realUser.username) else bal
                }
                state = state.copy(balances = realBalances)
            }
        }

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
        return state.balances.all { kotlin.math.abs(it.netAmount) < 0.01 }
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
                        state = state.copy(snackbarMessage = "Casa eliminata definitivamente.", isSuccessSnackbar = true, showDeleteHouseConfirm = false)
                    } else {
                        state = state.copy(snackbarMessage = "Impossibile eliminare la casa: ci sono conti in sospeso!", isSuccessSnackbar = false, showDeleteHouseConfirm = false)
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
                    state = state.copy(inviteUserQuery = "", searchedUsers = emptyList(), snackbarMessage = "Invito inviato a ${action.user.username}!", isSuccessSnackbar = true, showInviteDialog = false)
                }
            }
            UniHomeAction.OnDismissSnackbar -> state = state.copy(snackbarMessage = null)
            is UniHomeAction.OnAcceptInvitation -> viewModelScope.launch { repository.acceptInvitation(action.invitation, state.currentUsername, state.currentUserAvatar) }
            is UniHomeAction.OnDeclineInvitation -> viewModelScope.launch { repository.declineInvitation(action.invitationId) }

            is UniHomeAction.OnRemoveMember -> {
                val houseId = state.houseId ?: return
                val memberBalance = state.balances.find { it.userUid == action.member.userUid }?.netAmount ?: 0.0
                viewModelScope.launch {
                    if (kotlin.math.abs(memberBalance) < 0.01) {
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
                    if (kotlin.math.abs(myBalance) < 0.01) {
                        repository.removeMemberFromHouse(houseId, currentUserId, state.currentUsername)
                        state = state.copy(snackbarMessage = "Sei uscito dalla casa.", isSuccessSnackbar = true, showLeaveHouseConfirm = false)
                    } else {
                        state = state.copy(snackbarMessage = "Non puoi uscire dalla casa se il tuo bilancio non è in pari (0.00€)!", isSuccessSnackbar = false, showLeaveHouseConfirm = false)
                    }
                }
            }
            is UniHomeAction.OnNewServiceNameChanged -> state = state.copy(newServiceName = action.name)
            UniHomeAction.OnAddCleaningServiceClicked -> {
                val houseId = state.houseId ?: return
                if (state.newServiceName.isNotBlank()) {
                    viewModelScope.launch {
                        repository.addCleaningService(houseId, state.newServiceName)
                        state = state.copy(newServiceName = "", showServiceDialog = false)
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

            is UniHomeAction.OnToggleMemberSplitSelection -> {
                val currentList = state.selectedMembersForSplit.toMutableList()
                if (currentList.contains(action.member)) currentList.remove(action.member) else currentList.add(action.member)
                state = state.copy(selectedMembersForSplit = currentList)
            }

            UniHomeAction.OnAddExpenseClicked -> {
                val houseId = state.houseId ?: return
                val totalAmount = state.newExpenseAmount.toDoubleOrNull() ?: 0.0
                if (state.newExpenseTitle.isNotBlank() && totalAmount > 0.0 && state.selectedMembersForSplit.isNotEmpty()) {
                    viewModelScope.launch {
                        val dividedAmount = totalAmount / state.selectedMembersForSplit.size
                        val splits = state.selectedMembersForSplit.map { member ->
                            TransactionSplitEntity(transactionId = 0, userUid = member.userUid, username = member.username, amountOwed = dividedAmount)
                        }
                        repository.addGroupExpense(houseId, state.newExpenseTitle, totalAmount, currentUserId, state.currentUsername, splits)
                        state = state.copy(newExpenseTitle = "", newExpenseAmount = "", isSuccessSnackbar = true, showExpenseDialog = false, selectedMembersForSplit = state.members)
                    }
                }
            }

            is UniHomeAction.OnSettleDebtorSelected -> state = state.copy(selectedSettleDebtor = action.balance)
            is UniHomeAction.OnSettleAmountTextChanged -> state = state.copy(settleAmountText = action.amount)

            UniHomeAction.OnSettleDebtClicked -> {
                val houseId = state.houseId ?: return
                val targetDebtor = state.selectedSettleDebtor ?: return
                val amount = state.settleAmountText.toDoubleOrNull() ?: 0.0
                if (amount > 0.0) {
                    viewModelScope.launch {
                        val singleSplit = listOf(
                            TransactionSplitEntity(transactionId = 0, userUid = targetDebtor.userUid, username = targetDebtor.username, amountOwed = amount)
                        )
                        repository.addGroupExpense(
                            houseId = houseId,
                            title = "Saldatura: ${state.currentUsername} ➔ ${targetDebtor.username}",
                            totalAmount = amount,
                            payerUid = currentUserId,
                            payerUsername = state.currentUsername,
                            splits = singleSplit
                        )
                        state = state.copy(snackbarMessage = "Debito saldato correttamente con ${targetDebtor.username}!", isSuccessSnackbar = true, showSettleDialog = false, selectedSettleDebtor = null, settleAmountText = "")
                    }
                }
            }

            is UniHomeAction.OnRequestDeleteTransaction -> state = state.copy(txToDeleteId = action.txId)
            UniHomeAction.OnConfirmDeleteTransaction -> {
                val idToDelete = state.txToDeleteId ?: return
                viewModelScope.launch {
                    repository.deleteTransaction(idToDelete)
                    state = state.copy(snackbarMessage = "Spesa eliminata, conti ricalcolati.", isSuccessSnackbar = true, txToDeleteId = null)
                }
            }

            is UniHomeAction.OnToggleInviteDialog -> state = state.copy(showInviteDialog = action.show, inviteUserQuery = "", searchedUsers = emptyList())
            is UniHomeAction.OnToggleServiceDialog -> state = state.copy(showServiceDialog = action.show, newServiceName = "")
            is UniHomeAction.OnToggleExpenseDialog -> state = state.copy(showExpenseDialog = action.show, newExpenseTitle = "", newExpenseAmount = "", selectedMembersForSplit = state.members)
            is UniHomeAction.OnToggleSettleDialog -> state = state.copy(showSettleDialog = action.show, selectedSettleDebtor = null, settleAmountText = "")
            is UniHomeAction.OnToggleAllTxDetailsPage -> state = state.copy(showAllTxDetailsPage = action.show)
            is UniHomeAction.OnToggleDeleteHouseConfirm -> state = state.copy(showDeleteHouseConfirm = action.show)
            is UniHomeAction.OnToggleLeaveHouseConfirm -> state = state.copy(showLeaveHouseConfirm = action.show)
        }
    }
}