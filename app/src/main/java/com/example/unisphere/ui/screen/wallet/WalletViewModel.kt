package com.example.unisphere.ui.screen.wallet

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unisphere.db.SupabaseClient
import com.example.unisphere.db.local.entity.TransactionCategoryEntity
import com.example.unisphere.db.local.entity.TransactionEntity
import com.example.unisphere.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class WalletState(
    val transactions: List<TransactionEntity> = emptyList(),
    val filteredTransactions: List<TransactionEntity> = emptyList(),
    val categories: List<TransactionCategoryEntity> = emptyList(),
    val selectedTransaction: TransactionEntity? = null,
    val showAddDialog: Boolean = false,

    // Filtri Combinabili iOS
    val filterCategoryId: Int? = null,
    val filterIsIncome: Boolean? = null, // null = Tutti, true = Entrate, false = Uscite
    val filterMinAmount: String = "",
    val filterMaxAmount: String = "",
    val isFilterPanelExpanded: Boolean = false,
    val showAllTransactions: Boolean = false,

    // Form fields nuova transazione
    val newTransactionTitle: String = "",
    val newTransactionAmount: String = "",
    val newTransactionCategoryId: Int = 0,
    val newTransactionDate: LocalDate = LocalDate.now(),
    val newTransactionIsIncome: Boolean = false,
    val showDatePicker: Boolean = false
)

sealed interface WalletAction {
    data class OnTitleChanged(val value: String) : WalletAction
    data class OnAmountChanged(val value: String) : WalletAction
    data class OnCategoryChanged(val categoryId: Int) : WalletAction
    data class OnDateChanged(val value: LocalDate) : WalletAction
    data class OnTypeChanged(val isIncome: Boolean) : WalletAction
    data object OnAddClicked : WalletAction
    data object OnDismissAddDialog : WalletAction
    data object OnSaveTransactionClicked : WalletAction
    data class OnTransactionSelected(val transaction: TransactionEntity?) : WalletAction
    data object OnDeleteTransactionClicked : WalletAction
    data class OnUpdateTransactionClicked(val transaction: TransactionEntity) : WalletAction
    data class OnCreateCategoryType(val name: String, val colorHex: String) : WalletAction
    data class OnDeleteCategoryType(val category: TransactionCategoryEntity) : WalletAction
    data object ToggleDatePicker : WalletAction

    // Azioni dei Filtri
    data class OnFilterCategoryChanged(val categoryId: Int?) : WalletAction
    data class OnFilterTypeChanged(val isIncome: Boolean?) : WalletAction
    data class OnFilterMinAmountChanged(val value: String) : WalletAction
    data class OnFilterMaxAmountChanged(val value: String) : WalletAction
    data object ToggleFilterPanel : WalletAction
    data object ToggleShowAllTransactions : WalletAction
    data object OnClearFilters : WalletAction
}

@HiltViewModel
class WalletViewModel @Inject constructor(
    application: Application,
    private val walletRepository: WalletRepository
) : AndroidViewModel(application) {

    var state by mutableStateOf(WalletState())
        private set

    private var rawTransactions: List<TransactionEntity> = emptyList()
    private var pendingCategorySelectionName: String? = null
    private var currentUid: String = "default_user"

    private var categoriesJob: Job? = null
    private var transactionsJob: Job? = null

    init {
        observeSession()
    }

    // RISOLUZIONE COLD START: Ascolta reattivamente la sessione di Supabase senza perdere l'ID
    private fun observeSession() {
        viewModelScope.launch {
            SupabaseClient.client.auth.sessionStatus.collectLatest { status ->
                val uid = if (status is SessionStatus.Authenticated) {
                    status.session.user?.id ?: "default_user"
                } else {
                    SupabaseClient.client.auth.currentUserOrNull()?.id ?: "default_user"
                }
                currentUid = uid
                loadWalletData(uid)
            }
        }
    }

    private fun loadWalletData(uid: String) {
        categoriesJob?.cancel()
        categoriesJob = viewModelScope.launch {
            walletRepository.getCategories(uid).collectLatest { cats ->
                state = state.copy(
                    categories = cats,
                    newTransactionCategoryId = if (state.newTransactionCategoryId == 0) cats.firstOrNull()?.id ?: 0 else state.newTransactionCategoryId
                )

                // AUTOSELEZIONE AUTOMATICA: Se abbiamo appena creato una categoria, la imposta come attiva
                pendingCategorySelectionName?.let { name ->
                    cats.find { it.name == name }?.let { matchedCat ->
                        state = state.copy(newTransactionCategoryId = matchedCat.id)
                    }
                    pendingCategorySelectionName = null
                }
                applyFilters()
            }
        }

        transactionsJob?.cancel()
        transactionsJob = viewModelScope.launch {
            walletRepository.getTransactions(uid).collectLatest { trans ->
                rawTransactions = trans
                // CORREZIONE CRITICA: Ora aggiorna la sorgente primaria per alimentare i grafici della UI
                state = state.copy(transactions = trans)
                applyFilters()
            }
        }
    }

    fun onAction(action: WalletAction) {
        when (action) {
            is WalletAction.OnTitleChanged -> state = state.copy(newTransactionTitle = action.value)
            is WalletAction.OnAmountChanged -> state = state.copy(newTransactionAmount = action.value)
            is WalletAction.OnCategoryChanged -> state = state.copy(newTransactionCategoryId = action.categoryId)
            is WalletAction.OnDateChanged -> state = state.copy(newTransactionDate = action.value, showDatePicker = false)
            is WalletAction.OnTypeChanged -> state = state.copy(newTransactionIsIncome = action.isIncome)
            WalletAction.OnAddClicked -> {
                state = state.copy(
                    showAddDialog = true,
                    newTransactionTitle = "",
                    newTransactionAmount = "",
                    newTransactionCategoryId = state.categories.firstOrNull()?.id ?: 0,
                    newTransactionDate = LocalDate.now(),
                    newTransactionIsIncome = false
                )
            }
            WalletAction.OnDismissAddDialog -> state = state.copy(showAddDialog = false)
            WalletAction.OnSaveTransactionClicked -> saveTransaction()
            is WalletAction.OnTransactionSelected -> state = state.copy(selectedTransaction = action.transaction)
            WalletAction.OnDeleteTransactionClicked -> {
                state.selectedTransaction?.let { trans ->
                    viewModelScope.launch {
                        walletRepository.deleteTransaction(trans)
                        state = state.copy(selectedTransaction = null)
                    }
                }
            }
            is WalletAction.OnUpdateTransactionClicked -> {
                viewModelScope.launch {
                    walletRepository.saveTransaction(action.transaction)
                    state = state.copy(selectedTransaction = null)
                }
            }
            is WalletAction.OnCreateCategoryType -> {
                pendingCategorySelectionName = action.name
                viewModelScope.launch {
                    walletRepository.saveCategory(
                        TransactionCategoryEntity(name = action.name, colorHex = action.colorHex, userId = currentUid)
                    )
                }
            }
            is WalletAction.OnDeleteCategoryType -> {
                viewModelScope.launch {
                    walletRepository.deleteCategory(action.category)
                }
            }
            WalletAction.ToggleDatePicker -> state = state.copy(showDatePicker = !state.showDatePicker)
            is WalletAction.OnFilterCategoryChanged -> {
                state = state.copy(filterCategoryId = action.categoryId)
                applyFilters()
            }
            is WalletAction.OnFilterTypeChanged -> {
                state = state.copy(filterIsIncome = action.isIncome)
                applyFilters()
            }
            is WalletAction.OnFilterMinAmountChanged -> {
                state = state.copy(filterMinAmount = action.value)
                applyFilters()
            }
            is WalletAction.OnFilterMaxAmountChanged -> {
                state = state.copy(filterMaxAmount = action.value)
                applyFilters()
            }
            WalletAction.ToggleFilterPanel -> state = state.copy(isFilterPanelExpanded = !state.isFilterPanelExpanded)
            WalletAction.ToggleShowAllTransactions -> state = state.copy(showAllTransactions = !state.showAllTransactions)
            WalletAction.OnClearFilters -> {
                state = state.copy(filterCategoryId = null, filterIsIncome = null, filterMinAmount = "", filterMaxAmount = "")
                applyFilters()
            }
        }
    }

    private fun applyFilters() {
        state = state.copy(
            filteredTransactions = rawTransactions.filter { trans ->
                val matchesCategory = state.filterCategoryId == null || trans.categoryId == state.filterCategoryId
                val matchesType = state.filterIsIncome == null || trans.isIncome == state.filterIsIncome
                val minAmt = state.filterMinAmount.toDoubleOrNull()
                val matchesMin = minAmt == null || trans.amount >= minAmt
                val maxAmt = state.filterMaxAmount.toDoubleOrNull()
                val matchesMax = maxAmt == null || trans.amount <= maxAmt

                matchesCategory && matchesType && matchesMin && matchesMax
            }
        )
    }

    private fun saveTransaction() {
        val amountDouble = state.newTransactionAmount.toDoubleOrNull() ?: 0.0
        if (state.newTransactionTitle.isNotBlank() && amountDouble > 0 && state.newTransactionCategoryId != 0) {
            viewModelScope.launch {
                walletRepository.saveTransaction(
                    TransactionEntity(
                        userUid = currentUid,
                        title = state.newTransactionTitle,
                        amount = amountDouble,
                        categoryId = state.newTransactionCategoryId,
                        date = state.newTransactionDate,
                        isIncome = state.newTransactionIsIncome
                    )
                )
                state = state.copy(showAddDialog = false)
            }
        }
    }
}