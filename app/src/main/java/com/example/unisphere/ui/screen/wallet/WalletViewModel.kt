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

data class ChartPoint(val date: LocalDate, val balance: Double, val hasTransaction: Boolean)
data class PieSlice(val categoryName: String, val colorHex: String, val totalAmount: Double, val sweepAngle: Float)

data class WalletState(
    val transactions: List<TransactionEntity> = emptyList(),
    val filteredTransactions: List<TransactionEntity> = emptyList(),
    val categories: List<TransactionCategoryEntity> = emptyList(),
    val selectedTransaction: TransactionEntity? = null,
    val showAddDialog: Boolean = false,
    val currentUserId: String = "default_user",

    val totalIncomes: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netBalance: Double = 0.0,
    val pieSlices: List<PieSlice> = emptyList(),
    val lineChartPoints: List<ChartPoint> = emptyList(),
    val maxTimelineBalance: Double = 100.0,
    val minTimelineBalance: Double = 0.0,
    val timelineRange: Double = 100.0,
    val selectedChartIndex: Int? = null,

    // Filtri avanzati
    val filterCategoryId: Int? = null,
    val filterIsIncome: Boolean? = null,
    val filterMinAmount: String = "",
    val filterMaxAmount: String = "",
    val isFilterPanelExpanded: Boolean = false,
    val isFilterCategoryDropdownExpanded: Boolean = false,
    val showAllTransactions: Boolean = false,

    val newTransactionTitle: String = "",
    val newTransactionAmount: String = "",
    val newTransactionCategoryId: Int = 0,
    val newTransactionDate: LocalDate = LocalDate.now(),
    val newTransactionIsIncome: Boolean = false,
    val showDatePicker: Boolean = false,
    val showCategoryCreationDialog: Boolean = false,
    val categoryToDelete: TransactionCategoryEntity? = null,
    val isCategoryDropdownExpanded: Boolean = false,
    val newCategoryName: String = "",
    val newCategoryColorHex: String = "#34C759",

    val isDetailEditingActive: Boolean = false,
    val detailTitleText: String = "",
    val detailAmountText: String = "",
    val detailCategoryId: Int = 0,
    val detailDateValue: LocalDate = LocalDate.now(),
    val detailIsIncomeValue: Boolean = false,
    val showDetailDatePicker: Boolean = false,
    val isDetailCategoryDropdownExpanded: Boolean = false
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
    data object OnToggleDetailEditing : WalletAction
    data class OnDetailTitleChanged(val value: String) : WalletAction
    data class OnDetailAmountChanged(val value: String) : WalletAction
    data class OnDetailCategoryChanged(val categoryId: Int) : WalletAction
    data class OnDetailTypeChanged(val isIncome: Boolean) : WalletAction
    data class OnDetailDateChanged(val value: LocalDate) : WalletAction
    data object OnSaveUpdateTransactionClicked : WalletAction
    data object OnDeleteTransactionClicked : WalletAction

    data class OnCreateCategoryType(val name: String, val colorHex: String) : WalletAction
    data class OnRequestDeleteCategory(val category: TransactionCategoryEntity?) : WalletAction
    data object OnConfirmDeleteCategory : WalletAction
    data class OnNewCategoryNameChanged(val value: String) : WalletAction
    data class OnNewCategoryColorChanged(val hex: String) : WalletAction

    data object ToggleDatePicker : WalletAction
    data object ToggleDetailDatePicker : WalletAction
    data class ToggleCategoryDropdown(val expanded: Boolean) : WalletAction
    data class ToggleDetailCategoryDropdown(val expanded: Boolean) : WalletAction
    data class ToggleCategoryCreationDialog(val show: Boolean) : WalletAction
    data class OnChartPointSelected(val index: Int?) : WalletAction

    // Filtri
    data class OnFilterCategoryChanged(val categoryId: Int?) : WalletAction
    data class OnFilterTypeChanged(val isIncome: Boolean?) : WalletAction
    data class OnFilterMinAmountChanged(val value: String) : WalletAction
    data class OnFilterMaxAmountChanged(val value: String) : WalletAction
    data object ToggleFilterPanel : WalletAction
    data class ToggleFilterCategoryDropdown(val expanded: Boolean) : WalletAction // NUOVA AZIONE
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

    private fun observeSession() {
        viewModelScope.launch {
            SupabaseClient.client.auth.sessionStatus.collectLatest { status ->
                val uid = if (status is SessionStatus.Authenticated) {
                    status.session.user?.id ?: "default_user"
                } else {
                    SupabaseClient.client.auth.currentUserOrNull()?.id ?: "default_user"
                }
                currentUid = uid
                state = state.copy(currentUserId = uid)
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
                pendingCategorySelectionName?.let { name ->
                    cats.find { it.name == name }?.let { matchedCat ->
                        state = state.copy(newTransactionCategoryId = matchedCat.id)
                    }
                    pendingCategorySelectionName = null
                }
                generateCalculatedData()
            }
        }

        transactionsJob?.cancel()
        transactionsJob = viewModelScope.launch {
            walletRepository.getTransactions(uid).collectLatest { trans ->
                rawTransactions = trans
                generateCalculatedData()
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
                    newTransactionIsIncome = false,
                    isCategoryDropdownExpanded = false
                )
            }
            WalletAction.OnDismissAddDialog -> state = state.copy(showAddDialog = false)
            WalletAction.OnSaveTransactionClicked -> saveTransaction()

            is WalletAction.OnTransactionSelected -> {
                val tx = action.transaction
                if (tx != null) {
                    state = state.copy(
                        selectedTransaction = tx,
                        isDetailEditingActive = false,
                        detailTitleText = tx.title,
                        detailAmountText = tx.amount.toString(),
                        detailCategoryId = tx.categoryId,
                        detailDateValue = tx.date,
                        detailIsIncomeValue = tx.isIncome
                    )
                } else {
                    state = state.copy(selectedTransaction = null)
                }
            }
            WalletAction.OnToggleDetailEditing -> state = state.copy(isDetailEditingActive = !state.isDetailEditingActive)
            is WalletAction.OnDetailTitleChanged -> state = state.copy(detailTitleText = action.value)
            is WalletAction.OnDetailAmountChanged -> state = state.copy(detailAmountText = action.value)
            is WalletAction.OnDetailCategoryChanged -> state = state.copy(detailCategoryId = action.categoryId)
            is WalletAction.OnDetailTypeChanged -> state = state.copy(detailIsIncomeValue = action.isIncome)
            is WalletAction.OnDetailDateChanged -> state = state.copy(detailDateValue = action.value, showDetailDatePicker = false)

            WalletAction.OnSaveUpdateTransactionClicked -> {
                val currentTx = state.selectedTransaction ?: return
                val parsedAmount = state.detailAmountText.toDoubleOrNull() ?: 0.0
                if (state.detailTitleText.isNotBlank() && parsedAmount > 0) {
                    viewModelScope.launch {
                        walletRepository.saveTransaction(
                            currentTx.copy(
                                title = state.detailTitleText,
                                amount = parsedAmount,
                                categoryId = state.detailCategoryId,
                                date = state.detailDateValue,
                                isIncome = state.detailIsIncomeValue
                            )
                        )
                        state = state.copy(selectedTransaction = null)
                    }
                }
            }
            WalletAction.OnDeleteTransactionClicked -> {
                state.selectedTransaction?.let { trans ->
                    viewModelScope.launch {
                        walletRepository.deleteTransaction(trans)
                        state = state.copy(selectedTransaction = null)
                    }
                }
            }
            is WalletAction.OnCreateCategoryType -> {
                pendingCategorySelectionName = action.name
                state = state.copy(showCategoryCreationDialog = false)
                viewModelScope.launch {
                    walletRepository.saveCategory(
                        TransactionCategoryEntity(name = action.name, colorHex = action.colorHex, userId = currentUid)
                    )
                }
            }
            is WalletAction.OnRequestDeleteCategory -> state = state.copy(categoryToDelete = action.category)
            WalletAction.OnConfirmDeleteCategory -> {
                val catToDelete = state.categoryToDelete ?: return
                viewModelScope.launch {
                    walletRepository.deleteCategory(catToDelete)
                    state = state.copy(categoryToDelete = null)
                }
            }
            is WalletAction.OnNewCategoryNameChanged -> state = state.copy(newCategoryName = action.value)
            is WalletAction.OnNewCategoryColorChanged -> state = state.copy(newCategoryColorHex = action.hex)
            WalletAction.ToggleDatePicker -> state = state.copy(showDatePicker = !state.showDatePicker)
            WalletAction.ToggleDetailDatePicker -> state = state.copy(showDetailDatePicker = !state.showDetailDatePicker)
            is WalletAction.ToggleCategoryDropdown -> state = state.copy(isCategoryDropdownExpanded = action.expanded)
            is WalletAction.ToggleDetailCategoryDropdown -> state = state.copy(isDetailCategoryDropdownExpanded = action.expanded)
            is WalletAction.ToggleCategoryCreationDialog -> {
                state = state.copy(
                    showCategoryCreationDialog = action.show,
                    newCategoryName = "",
                    newCategoryColorHex = "#34C759"
                )
            }
            is WalletAction.OnChartPointSelected -> state = state.copy(selectedChartIndex = action.index)

            // FILTRI
            is WalletAction.OnFilterCategoryChanged -> {
                state = state.copy(
                    filterCategoryId = action.categoryId,
                    isFilterCategoryDropdownExpanded = false // Chiude la tendina dopo la selezione
                )
                generateCalculatedData()
            }
            is WalletAction.OnFilterTypeChanged -> {
                state = state.copy(filterIsIncome = action.isIncome)
                generateCalculatedData()
            }
            is WalletAction.OnFilterMinAmountChanged -> {
                state = state.copy(filterMinAmount = action.value)
                generateCalculatedData()
            }
            is WalletAction.OnFilterMaxAmountChanged -> {
                state = state.copy(filterMaxAmount = action.value)
                generateCalculatedData()
            }
            WalletAction.ToggleFilterPanel -> state = state.copy(
                isFilterPanelExpanded = !state.isFilterPanelExpanded,
                isFilterCategoryDropdownExpanded = false // Assicura che la tendina parta chiusa
            )
            is WalletAction.ToggleFilterCategoryDropdown -> state = state.copy(isFilterCategoryDropdownExpanded = action.expanded)
            WalletAction.ToggleShowAllTransactions -> state = state.copy(showAllTransactions = !state.showAllTransactions)
            WalletAction.OnClearFilters -> {
                state = state.copy(
                    filterCategoryId = null,
                    filterIsIncome = null,
                    filterMinAmount = "",
                    filterMaxAmount = "",
                    isFilterCategoryDropdownExpanded = false
                )
                generateCalculatedData()
            }
        }
    }

    private fun generateCalculatedData() {
        val totalInc = rawTransactions.filter { it.isIncome }.sumOf { it.amount }
        val totalExp = rawTransactions.filter { !it.isIncome }.sumOf { it.amount }

        val filteredList = rawTransactions.filter { trans ->
            val matchesCategory = state.filterCategoryId == null || trans.categoryId == state.filterCategoryId
            val matchesType = state.filterIsIncome == null || trans.isIncome == state.filterIsIncome
            val minAmt = state.filterMinAmount.toDoubleOrNull()
            val matchesMin = minAmt == null || trans.amount >= minAmt
            val maxAmt = state.filterMaxAmount.toDoubleOrNull()
            val matchesMax = maxAmt == null || trans.amount <= maxAmt
            matchesCategory && matchesType && matchesMin && matchesMax
        }

        val expensesOnly = rawTransactions.filter { !it.isIncome }
        val totalExpensesAmount = expensesOnly.sumOf { it.amount }
        val pieGroup = expensesOnly.groupBy { it.categoryId }.map { (catId, list) ->
            val category = state.categories.find { it.id == catId }
            val amount = list.sumOf { it.amount }
            val sweep = if (totalExpensesAmount > 0) (amount.toFloat() / totalExpensesAmount.toFloat()) * 360f else 0f
            PieSlice(category?.name ?: "Altro", category?.colorHex ?: "#8E8E93", amount, sweep)
        }

        val sortedTx = rawTransactions.sortedBy { it.date }
        val timelinePoints = mutableListOf<ChartPoint>()
        var rollingBalance = 0.0
        var cursorDate = LocalDate.now().minusDays(30)
        val today = LocalDate.now()

        while (!cursorDate.isAfter(today)) {
            val daysTransactions = sortedTx.filter { it.date == cursorDate }
            val hasChange = daysTransactions.isNotEmpty()
            if (hasChange) {
                rollingBalance += daysTransactions.sumOf { if (it.isIncome) it.amount else -it.amount }
            }
            timelinePoints.add(ChartPoint(cursorDate, rollingBalance, hasChange))
            cursorDate = cursorDate.plusDays(1)
        }

        val maxBal = timelinePoints.maxOfOrNull { it.balance } ?: 100.0
        val minBal = timelinePoints.minOfOrNull { it.balance } ?: 0.0
        val rangeBal = (maxBal - minBal).coerceAtLeast(100.0)

        state = state.copy(
            transactions = rawTransactions,
            filteredTransactions = filteredList,
            totalIncomes = totalInc,
            totalExpenses = totalExp,
            netBalance = totalInc - totalExp,
            pieSlices = pieGroup,
            lineChartPoints = timelinePoints,
            maxTimelineBalance = maxBal,
            minTimelineBalance = minBal,
            timelineRange = rangeBal
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