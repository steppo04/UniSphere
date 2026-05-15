package com.example.unisphere.ui.screen.wallet

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unisphere.db.SupabaseClient
import com.example.unisphere.db.local.entity.TransactionCategoryEntity
import com.example.unisphere.db.local.entity.TransactionEntity
import com.example.unisphere.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import kotlin.random.Random

data class WalletState(
    val transactions: List<TransactionEntity> = emptyList(),
    val categories: List<TransactionCategoryEntity> = emptyList(),
    val selectedTransaction: TransactionEntity? = null,
    val showAddDialog: Boolean = false,

    // Form fields per nuova transazione
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
}

@HiltViewModel
class WalletViewModel @Inject constructor(
    application: Application,
    private val walletRepository: WalletRepository
) : AndroidViewModel(application) {

    var state by mutableStateOf(WalletState())
        private set

    init {
        loadWalletData()
    }

    private fun loadWalletData() {
        val uid = SupabaseClient.client.auth.currentUserOrNull()?.id ?: "default_user"

        viewModelScope.launch {
            walletRepository.getCategories(uid).collectLatest { cats ->
                state = state.copy(
                    categories = cats,
                    newTransactionCategoryId = if (state.newTransactionCategoryId == 0) cats.firstOrNull()?.id ?: 0 else state.newTransactionCategoryId
                )
            }
        }

        viewModelScope.launch {
            walletRepository.getTransactions(uid).collectLatest { trans ->
                state = state.copy(transactions = trans)
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
                val uid = SupabaseClient.client.auth.currentUserOrNull()?.id ?: "default_user"
                viewModelScope.launch {
                    walletRepository.saveCategory(
                        TransactionCategoryEntity(name = action.name, colorHex = action.colorHex, userId = uid)
                    )
                }
            }
            is WalletAction.OnDeleteCategoryType -> {
                viewModelScope.launch {
                    walletRepository.deleteCategory(action.category)
                }
            }
            WalletAction.ToggleDatePicker -> state = state.copy(showDatePicker = !state.showDatePicker)
        }
    }

    private fun saveTransaction() {
        val amountDouble = state.newTransactionAmount.toDoubleOrNull() ?: 0.0
        val uid = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return
        if (state.newTransactionTitle.isNotBlank() && amountDouble > 0 && state.newTransactionCategoryId != 0) {
            viewModelScope.launch {
                walletRepository.saveTransaction(
                    TransactionEntity(
                        userUid = uid,
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