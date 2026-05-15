package com.example.unisphere.ui.screen.wallet

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.unisphere.db.local.entity.TransactionCategoryEntity
import com.example.unisphere.db.local.entity.TransactionEntity
import com.example.unisphere.ui.composables.AppBar
import com.example.unisphere.ui.composables.BottomNavigationBar
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    navController: NavHostController,
    viewModel: WalletViewModel = hiltViewModel()
) {
    val state = viewModel.state

    Scaffold(
        topBar = { AppBar(title = "UniWallet", navController = navController) },
        bottomBar = { BottomNavigationBar(navController = navController) },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onAction(WalletAction.OnAddClicked) }) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text("Riepilogo Spese", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                PieChartSection(state.transactions.filter { !it.isIncome }, state.categories)
            }

            item {
                Text("Andamento Mensile (Netto)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                LineChartSection(state.transactions)
            }

            item {
                Text("Transazioni Recenti", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            items(state.transactions.sortedByDescending { it.date }, key = { it.id }) { transaction ->
                val matchedCat = state.categories.find { it.id == transaction.categoryId }
                TransactionItem(
                    transaction = transaction,
                    categoryName = matchedCat?.name ?: "Altro",
                    colorHex = matchedCat?.colorHex ?: "#8E8E93",
                    onClick = { viewModel.onAction(WalletAction.OnTransactionSelected(transaction)) }
                )
            }
        }
    }

    if (state.showAddDialog) {
        AddTransactionDialog(state = state, viewModel = viewModel)
    }

    if (state.selectedTransaction != null) {
        TransactionDetailsDialog(state = state, viewModel = viewModel)
    }
}

@Composable
fun PieChartSection(transactions: List<TransactionEntity>, categories: List<TransactionCategoryEntity>) {
    val total = transactions.sumOf { it.amount }
    val groupedByCat = transactions.groupBy { it.categoryId }

    Card(
        modifier = Modifier.fillMaxWidth().height(250.dp).padding(top = 16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(modifier = Modifier.size(150.dp)) {
                if (total > 0) {
                    var startAngle = 0f
                    groupedByCat.forEach { (catId, transList) ->
                        val catColorHex = categories.find { it.id == catId }?.colorHex ?: "#8E8E93"
                        val categoryTotal = transList.sumOf { it.amount }
                        val sweepAngle = (categoryTotal.toFloat() / total.toFloat()) * 360f
                        drawArc(
                            color = Color(android.graphics.Color.parseColor(catColorHex)),
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true
                        )
                        startAngle += sweepAngle
                    }
                }
            }

            Column(modifier = Modifier.padding(start = 16.dp).verticalScroll(rememberScrollState())) {
                groupedByCat.forEach { (catId, _) ->
                    val category = categories.find { it.id == catId }
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(12.dp).background(Color(android.graphics.Color.parseColor(category?.colorHex ?: "#8E8E93")), CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(category?.name ?: "Altro", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun LineChartSection(transactions: List<TransactionEntity>) {
    val sortedTransactions = transactions.sortedBy { it.date }
    val balanceTimeline = mutableListOf<Triple<LocalDate, Double, Boolean>>()
    var currentBalance = 0.0

    val startDate = LocalDate.now().minusDays(30)
    val endDate = LocalDate.now()

    var tempDate = startDate
    while (!tempDate.isAfter(endDate)) {
        val daysTransactions = sortedTransactions.filter { it.date == tempDate }
        val hasChange = daysTransactions.isNotEmpty()
        if (hasChange) {
            currentBalance += daysTransactions.sumOf { if (it.isIncome) it.amount else -it.amount }
        }
        balanceTimeline.add(Triple(tempDate, currentBalance, hasChange))
        tempDate = tempDate.plusDays(1)
    }

    val maxBalance = balanceTimeline.maxOfOrNull { it.second } ?: 100.0
    val minBalance = balanceTimeline.minOfOrNull { it.second } ?: 0.0
    val range = (maxBalance - minBalance).coerceAtLeast(100.0)

    Card(
        modifier = Modifier.fillMaxWidth().height(300.dp).padding(top = 16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        val labelColor = MaterialTheme.colorScheme.onSurface.toArgb()
        val axisColor = MaterialTheme.colorScheme.outline

        Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 30.dp)) {
            val width = size.width
            val height = size.height
            val spacing = width / (balanceTimeline.size - 1)

            drawLine(color = axisColor, start = Offset(0f, height), end = Offset(width, height), strokeWidth = 2f)
            drawLine(color = axisColor, start = Offset(0f, 0f), end = Offset(0f, height), strokeWidth = 2f)

            val path = Path()
            balanceTimeline.forEachIndexed { index, data ->
                val x = index * spacing
                val normalizedY = ((data.second - minBalance) / range).toFloat()
                val y = height - (normalizedY * height)

                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)

                if (data.third) {
                    drawCircle(color = Color(0xFF6200EE), radius = 4.dp.toPx(), center = Offset(x, y))
                }
            }

            drawPath(path = path, color = Color(0xFF6200EE), style = Stroke(width = 2.dp.toPx()))

            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = labelColor
                    textSize = 24f
                    textAlign = android.graphics.Paint.Align.RIGHT
                }

                drawText("${maxBalance.toInt()}€", -10f, 10f, paint)
                drawText("${minBalance.toInt()}€", -10f, height, paint)

                paint.textAlign = android.graphics.Paint.Align.CENTER
                val formatter = DateTimeFormatter.ofPattern("dd/MM")
                drawText(balanceTimeline.first().first.format(formatter), 0f, height + 40f, paint)
                drawText(balanceTimeline.last().first.format(formatter), width, height + 40f, paint)
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: TransactionEntity, categoryName: String, colorHex: String, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(10.dp).background(Color(android.graphics.Color.parseColor(colorHex)), CircleShape))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.title, fontWeight = FontWeight.Bold)
                Text("$categoryName • ${transaction.date.format(DateTimeFormatter.ofPattern("dd MMM"))}", style = MaterialTheme.typography.bodySmall)
            }
            val displayAmount = if (transaction.isIncome) "+€${transaction.amount}" else "-€${transaction.amount}"
            val amountColor = if (transaction.isIncome) Color(0xFF4CAF50) else Color.Red

            Text(displayAmount, color = amountColor, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailsDialog(state: WalletState, viewModel: WalletViewModel) {
    val transaction = state.selectedTransaction ?: return
    var isEditing by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf(transaction.title) }
    var amount by remember { mutableStateOf(transaction.amount.toString()) }
    var categoryId by remember { mutableStateOf(transaction.categoryId) }
    var date by remember { mutableStateOf(transaction.date) }
    var isIncome by remember { mutableStateOf(transaction.isIncome) }
    var showDatePicker by remember { mutableStateOf(false) }

    val matchedCat = state.categories.find { it.id == categoryId }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    AlertDialog(
        onDismissRequest = { viewModel.onAction(WalletAction.OnTransactionSelected(null)) },
        title = { Text(if (isEditing) "Modifica Transazione" else "Dettagli Transazione") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isEditing) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !isIncome, onClick = { isIncome = false }, label = { Text("Uscita") }, modifier = Modifier.weight(1f))
                        FilterChip(selected = isIncome, onClick = { isIncome = true }, label = { Text("Entrata") }, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Titolo") })
                    OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Importo") })

                    // Dropdown Categorie
                    var expandedCats by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expandedCats, onExpandedChange = { expandedCats = it }) {
                        OutlinedTextField(
                            value = state.categories.find { it.id == categoryId }?.name ?: "Seleziona",
                            onValueChange = {}, readOnly = true, label = { Text("Categoria") }, modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expandedCats, onDismissRequest = { expandedCats = false }) {
                            state.categories.forEach { cat ->
                                DropdownMenuItem(text = { Text(cat.name) }, onClick = { categoryId = cat.id; expandedCats = false })
                            }
                        }
                    }
                } else {
                    Text("Titolo: ${transaction.title}", fontWeight = FontWeight.Bold)
                    Text("Importo: ${if (transaction.isIncome) "+" else "-"}€${transaction.amount}")
                    Text("Categoria: ${matchedCat?.name ?: "Altro"}")
                    Text("Data: ${transaction.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}")
                }
            }
        },
        confirmButton = {
            if (isEditing) {
                Button(onClick = {
                    viewModel.onAction(WalletAction.OnUpdateTransactionClicked(
                        transaction.copy(title = title, amount = amount.toDoubleOrNull() ?: 0.0, categoryId = categoryId, date = date, isIncome = isIncome)
                    ))
                }) { Text("Salva") }
            } else {
                TextButton(onClick = { isEditing = true }) { Text("Modifica") }
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.onAction(WalletAction.OnDeleteTransactionClicked) }) { Text("Elimina", color = Color.Red) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(state: WalletState, viewModel: WalletViewModel) {
    var showCatDialog by remember { mutableStateOf(false) }
    var catToDelete by remember { mutableStateOf<TransactionCategoryEntity?>(null) }
    var expandedCats by remember { mutableStateOf(false) }

    val selectedCategory = state.categories.find { it.id == state.newTransactionCategoryId }

    if (state.showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = state.newTransactionDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { viewModel.onAction(WalletAction.ToggleDatePicker) },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.onAction(WalletAction.OnDateChanged(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate())) }
                }) { Text("OK") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    AlertDialog(
        onDismissRequest = { viewModel.onAction(WalletAction.OnDismissAddDialog) },
        title = { Text("Nuova Transazione") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !state.newTransactionIsIncome, onClick = { viewModel.onAction(WalletAction.OnTypeChanged(false)) }, label = { Text("Uscita") }, modifier = Modifier.weight(1f))
                    FilterChip(selected = state.newTransactionIsIncome, onClick = { viewModel.onAction(WalletAction.OnTypeChanged(true)) }, label = { Text("Entrata") }, modifier = Modifier.weight(1f))
                }

                OutlinedTextField(value = state.newTransactionTitle, onValueChange = { viewModel.onAction(WalletAction.OnTitleChanged(it)) }, label = { Text("Titolo") }, singleLine = true)
                OutlinedTextField(value = state.newTransactionAmount, onValueChange = { viewModel.onAction(WalletAction.OnAmountChanged(it)) }, label = { Text("Importo") }, singleLine = true)

                // Dropdown Selezione Categorie con Aggiunta/Eliminazione al volo
                ExposedDropdownMenuBox(expanded = expandedCats, onExpandedChange = { expandedCats = it }) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "Seleziona Categoria",
                        onValueChange = {}, readOnly = true, label = { Text("Categoria") },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        leadingIcon = {
                            selectedCategory?.let { Box(Modifier.size(12.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(it.colorHex)))) }
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCats) }
                    )
                    ExposedDropdownMenu(expanded = expandedCats, onDismissRequest = { expandedCats = false }) {
                        DropdownMenuItem(
                            text = { Text("+ Nuova Categoria", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                            onClick = { expandedCats = false; showCatDialog = true }
                        )
                        HorizontalDivider()
                        state.categories.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(Modifier.size(10.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(cat.colorHex))))
                                            Spacer(Modifier.width(8.dp))
                                            Text(cat.name)
                                        }
                                        IconButton(onClick = { catToDelete = cat }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                onClick = { viewModel.onAction(WalletAction.OnCategoryChanged(cat.id)); expandedCats = false }
                            )
                        }
                    }
                }

                OutlinedCard(onClick = { viewModel.onAction(WalletAction.ToggleDatePicker) }, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Data: ${state.newTransactionDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { viewModel.onAction(WalletAction.OnSaveTransactionClicked) }) { Text("Aggiungi") }
        }
    )

    if (showCatDialog) {
        var newCatName by remember { mutableStateOf("") }
        var selectedColorHex by remember { mutableStateOf("#34C759") }
        val palette = listOf("#FF3B30", "#FF9500", "#FFCC00", "#34C759", "#007AFF", "#5856D6", "#AF52DE", "#8E8E93")

        AlertDialog(
            onDismissRequest = { showCatDialog = false },
            title = { Text("Nuova Categoria") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = newCatName, onValueChange = { newCatName = it }, label = { Text("Nome") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        palette.forEach { hex ->
                            Box(Modifier.size(28.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(hex))).clickable { selectedColorHex = hex }.border(if (selectedColorHex == hex) 2.dp else 0.dp, MaterialTheme.colorScheme.primary, CircleShape))
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.onAction(WalletAction.OnCreateCategoryType(newCatName, selectedColorHex)); showCatDialog = false }) { Text("Crea") }
            }
        )
    }

    catToDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { catToDelete = null },
            title = { Text("Elimina Categoria") },
            text = { Text("Eliminando la categoria \"${cat.name}\" eliminerai anche tutte le transazioni collegate ad essa. Continuare?") },
            confirmButton = { Button(onClick = { viewModel.onAction(WalletAction.OnDeleteCategoryType(cat)); catToDelete = null }) { Text("Elimina") } }
        )
    }
}