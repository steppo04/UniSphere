package com.example.unisphere.ui.screen.wallet

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import java.time.LocalDate
import kotlin.random.Random

data class WalletState(
    val transactions: List<Transaction> = listOf(
        Transaction(title = "Spesa Conad", amount = 45.50, category = "Cibo", color = Color(0xFFFF9800), date = LocalDate.now().minusDays(5), isIncome = false),
        Transaction(title = "Affitto Aprile", amount = 450.0, category = "Casa", color = Color(0xFF2196F3), date = LocalDate.now().minusDays(10), isIncome = false),
        Transaction(title = "Bolletta Luce", amount = 32.20, category = "Utenze", color = Color(0xFF4CAF50), date = LocalDate.now().minusDays(2), isIncome = false),
        Transaction(title = "Stipendio", amount = 1200.0, category = "Lavoro", color = Color(0xFF4CAF50), date = LocalDate.now().minusDays(15), isIncome = true),
        Transaction(title = "Netflix", amount = 12.99, category = "Svago", color = Color(0xFFE91E63), date = LocalDate.now().minusDays(1), isIncome = false)
    ),
    val showAddDialog: Boolean = false,
    val selectedTransaction: Transaction? = null,
    
    // State for New/Edit Transaction
    val newTransactionTitle: String = "",
    val newTransactionAmount: String = "",
    val newTransactionCategory: String = "Cibo",
    val newTransactionDate: LocalDate = LocalDate.now(),
    val newTransactionIsIncome: Boolean = false,
    val showDatePicker: Boolean = false
)

sealed interface WalletAction {
    data class OnTitleChanged(val value: String) : WalletAction
    data class OnAmountChanged(val value: String) : WalletAction
    data class OnCategoryChanged(val value: String) : WalletAction
    data class OnDateChanged(val value: LocalDate) : WalletAction
    data class OnTypeChanged(val isIncome: Boolean) : WalletAction
    data object OnAddClicked : WalletAction
    data object OnDismissAddDialog : WalletAction
    data object OnSaveTransactionClicked : WalletAction
    data class OnTransactionSelected(val transaction: Transaction?) : WalletAction
    data object OnDeleteTransactionClicked : WalletAction
    data class OnUpdateTransactionClicked(val transaction: Transaction) : WalletAction
    data object ToggleDatePicker : WalletAction
}

class WalletViewModel(application: Application) : AndroidViewModel(application) {

    var state by mutableStateOf(WalletState())
        private set

    fun onAction(action: WalletAction) {
        when (action) {
            is WalletAction.OnTitleChanged -> {
                state = state.copy(newTransactionTitle = action.value)
            }
            is WalletAction.OnAmountChanged -> {
                state = state.copy(newTransactionAmount = action.value)
            }
            is WalletAction.OnCategoryChanged -> {
                state = state.copy(newTransactionCategory = action.value)
            }
            is WalletAction.OnDateChanged -> {
                state = state.copy(newTransactionDate = action.value, showDatePicker = false)
            }
            is WalletAction.OnTypeChanged -> {
                state = state.copy(newTransactionIsIncome = action.isIncome)
            }
            WalletAction.OnAddClicked -> {
                state = state.copy(
                    showAddDialog = true,
                    newTransactionTitle = "",
                    newTransactionAmount = "",
                    newTransactionCategory = "Cibo",
                    newTransactionDate = LocalDate.now(),
                    newTransactionIsIncome = false
                )
            }
            WalletAction.OnDismissAddDialog -> {
                state = state.copy(showAddDialog = false)
            }
            WalletAction.OnSaveTransactionClicked -> {
                saveTransaction()
            }
            is WalletAction.OnTransactionSelected -> {
                state = state.copy(selectedTransaction = action.transaction)
            }
            WalletAction.OnDeleteTransactionClicked -> {
                state.selectedTransaction?.let { transaction ->
                    state = state.copy(
                        transactions = state.transactions.filter { it.id != transaction.id },
                        selectedTransaction = null
                    )
                }
            }
            is WalletAction.OnUpdateTransactionClicked -> {
                state = state.copy(
                    transactions = state.transactions.map {
                        if (it.id == action.transaction.id) action.transaction else it
                    },
                    selectedTransaction = null
                )
            }
            WalletAction.ToggleDatePicker -> {
                state = state.copy(showDatePicker = !state.showDatePicker)
            }
        }
    }

    // --- FUNCTIONS ---

    private fun saveTransaction() {
        val amountDouble = state.newTransactionAmount.toDoubleOrNull() ?: 0.0
        if (state.newTransactionTitle.isNotBlank() && amountDouble > 0) {
            val newColor = when (state.newTransactionCategory) {
                "Cibo" -> Color(0xFFFF9800)
                "Casa" -> Color(0xFF2196F3)
                "Utenze" -> Color(0xFF4CAF50)
                "Lavoro" -> Color(0xFF4CAF50)
                else -> generateUniqueRandomColor(state.transactions.map { it.color })
            }
            
            val newTransaction = Transaction(
                title = state.newTransactionTitle,
                amount = amountDouble,
                category = state.newTransactionCategory.ifBlank { "Altro" },
                color = newColor,
                date = state.newTransactionDate,
                isIncome = state.newTransactionIsIncome
            )
            
            state = state.copy(
                transactions = state.transactions + newTransaction,
                showAddDialog = false
            )
        }
    }

    private fun generateUniqueRandomColor(existingColors: List<Color>): Color {
        var color: Color
        val existingArgb = existingColors.map { it.toArgb() }
        do {
            val h = Random.nextFloat() * 360f
            val s = 0.6f + Random.nextFloat() * 0.4f
            val v = 0.6f + Random.nextFloat() * 0.3f
            val argb = android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))
            color = Color(argb)
        } while (existingArgb.contains(argb))
        return color
    }
}
