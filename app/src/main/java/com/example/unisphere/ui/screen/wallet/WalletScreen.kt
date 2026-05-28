package com.example.unisphere.ui.screen.wallet

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.unisphere.db.local.entity.TransactionEntity
import com.example.unisphere.ui.composables.AppBar
import com.example.unisphere.ui.composables.BottomNavigationBar
import com.example.unisphere.ui.composables.UniSphereAlertDialog
import com.example.unisphere.ui.composables.UniSphereEmptyState
import com.example.unisphere.ui.composables.UniSphereListItem
import com.example.unisphere.ui.composables.UniSphereTextField
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

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
            FloatingActionButton(
                onClick = { viewModel.onAction(WalletAction.OnAddClicked) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) { Icon(Icons.Default.Add, contentDescription = "Aggiungi") }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                WalletOverviewHero(saldoNetto = state.netBalance, entrate = state.totalIncomes, uscite = state.totalExpenses)
            }

            if (state.transactions.isEmpty()) {
                item {
                    EmptyDashboardState { viewModel.onAction(WalletAction.OnAddClicked) }
                }
            } else {
                item {
                    Text("Riepilogo Spese", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
                    PieChartSection(state.pieSlices)
                }
                item {
                    Text("Andamento Patrimoniale", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
                    LineChartSection(state = state, onAction = viewModel::onAction)
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Transazioni", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        IconButton(
                            onClick = { viewModel.onAction(WalletAction.ToggleFilterPanel) },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (state.isFilterPanelExpanded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filtra", tint = if (state.isFilterPanelExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                if (state.isFilterPanelExpanded) {
                    item { SmartFilterPanel(state = state, onAction = viewModel::onAction) }
                }

                val isFilteringActive = state.filterCategoryId != null || state.filterIsIncome != null || state.filterMinAmount.isNotBlank() || state.filterMaxAmount.isNotBlank()
                val transactionsToDisplay = if (state.showAllTransactions || isFilteringActive) {
                    state.filteredTransactions.sortedByDescending { it.date }
                } else {
                    state.filteredTransactions.sortedByDescending { it.date }.take(5)
                }

                if (transactionsToDisplay.isEmpty()) {
                    item {
                        Text("Nessun movimento trovato.", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
                    }
                } else {
                    items(transactionsToDisplay, key = { it.id }) { transaction ->
                        val matchedCat = state.categories.find { it.id == transaction.categoryId }
                        TransactionItem(
                            transaction = transaction,
                            categoryName = matchedCat?.name ?: "Altro",
                            colorHex = matchedCat?.colorHex ?: "#8E8E93",
                            onClick = { viewModel.onAction(WalletAction.OnTransactionSelected(transaction)) }
                        )
                    }

                    if (!isFilteringActive && state.filteredTransactions.size > 5) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                                TextButton(onClick = { viewModel.onAction(WalletAction.ToggleShowAllTransactions) }) {
                                    Text(
                                        text = if (state.showAllTransactions) "Mostra Meno" else "Vedi tutte le transazioni (${state.filteredTransactions.size})",
                                        fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (state.showAddDialog) AddTransactionDialog(state = state, viewModel = viewModel)
    if (state.selectedTransaction != null) TransactionDetailsDialog(state = state, viewModel = viewModel)
}

@Composable
fun WalletOverviewHero(saldoNetto: Double, entrate: Double, uscite: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text("Saldo Disponibile", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            Text(
                text = String.format(java.util.Locale.US, "%.2f €", saldoNetto),
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Color(0xFFE8F5E9)).padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ArrowUpward, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Entrate", fontSize = 10.sp, color = Color(0xFF2E7D32).copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
                        Text(String.format(java.util.Locale.US, "+%.0f €", entrate), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    }
                }
                Row(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Color(0xFFFFEAEA)).padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ArrowDownward, null, tint = Color(0xFFC62828), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Uscite", fontSize = 10.sp, color = Color(0xFFC62828).copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
                        Text(String.format(java.util.Locale.US, "-%.0f €", uscite), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartFilterPanel(state: WalletState, onAction: (WalletAction) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth().height(38.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.background).padding(2.dp)) {
                val selectedType = state.filterIsIncome
                Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(if (selectedType == null) MaterialTheme.colorScheme.surface else Color.Transparent).clickable { onAction(WalletAction.OnFilterTypeChanged(null)) }, contentAlignment = Alignment.Center) {
                    Text("Tutte", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selectedType == null) MaterialTheme.colorScheme.primary else Color.Gray)
                }
                Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(if (selectedType == true) Color(0xFFE8F5E9) else Color.Transparent).clickable { onAction(WalletAction.OnFilterTypeChanged(true)) }, contentAlignment = Alignment.Center) {
                    Text("Entrate", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selectedType == true) Color(0xFF2E7D32) else Color.Gray)
                }
                Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(if (selectedType == false) Color(0xFFFFEAEA) else Color.Transparent).clickable { onAction(WalletAction.OnFilterTypeChanged(false)) }, contentAlignment = Alignment.Center) {
                    Text("Uscite", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selectedType == false) Color(0xFFC62828) else Color.Gray)
                }
            }

            val currentFilterCat = state.categories.find { it.id == state.filterCategoryId }
            ExposedDropdownMenuBox(
                expanded = state.isFilterCategoryDropdownExpanded,
                onExpandedChange = { onAction(WalletAction.ToggleFilterCategoryDropdown(it)) }
            ) {
                UniSphereTextField(
                    value = currentFilterCat?.name ?: "Tutte le categorie",
                    onValueChange = {},
                    label = "Filtra per Categoria",
                    leadingIcon = Icons.Default.Category,
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = state.isFilterCategoryDropdownExpanded) }
                )
                ExposedDropdownMenu(
                    expanded = state.isFilterCategoryDropdownExpanded,
                    onDismissRequest = { onAction(WalletAction.ToggleFilterCategoryDropdown(false)) }
                ) {
                    DropdownMenuItem(text = { Text("Tutte le categorie", fontWeight = FontWeight.Bold) }, onClick = { onAction(WalletAction.OnFilterCategoryChanged(null)) })
                    state.categories.forEach { cat ->
                        DropdownMenuItem(text = { Text(cat.name) }, onClick = { onAction(WalletAction.OnFilterCategoryChanged(cat.id)) })
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                UniSphereTextField(
                    value = state.filterMinAmount,
                    onValueChange = { onAction(WalletAction.OnFilterMinAmountChanged(it)) },
                    label = "Importo Min (€)",
                    leadingIcon = Icons.Default.TrendingDown,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                UniSphereTextField(
                    value = state.filterMaxAmount,
                    onValueChange = { onAction(WalletAction.OnFilterMaxAmountChanged(it)) },
                    label = "Importo Max (€)",
                    leadingIcon = Icons.Default.TrendingUp,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            TextButton(onClick = { onAction(WalletAction.OnClearFilters) }, modifier = Modifier.align(Alignment.End)) {
                Icon(Icons.Default.ClearAll, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Azzera Filtri", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LineChartSection(state: WalletState, onAction: (WalletAction) -> Unit) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val axisColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurface.toArgb()

    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            AnimatedContent(targetState = state.selectedChartIndex, label = "HeaderAnim") { targetIndex ->
                if (targetIndex != null && targetIndex < state.lineChartPoints.size) {
                    val infoGiorno = state.lineChartPoints[targetIndex]
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = infoGiorno.date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                        Text(text = String.format(java.util.Locale.US, "Saldo: %.2f €", infoGiorno.balance), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = if (infoGiorno.balance >= 0) Color(0xFF2E7D32) else Color(0xFFC62828))
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TrendingUp, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tocca la curva per i dettagli giornalieri", fontSize = 13.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                Canvas(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp)
                        .pointerInput(state.lineChartPoints) {
                            detectTapGestures { offset ->
                                val totalPoints = state.lineChartPoints.size
                                if (totalPoints > 1) {
                                    val spacing = size.width / (totalPoints - 1)
                                    val closestIndex = (offset.x / spacing).roundToInt().coerceIn(0, totalPoints - 1)
                                    onAction(WalletAction.OnChartPointSelected(closestIndex))
                                }
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height
                    val totalPoints = state.lineChartPoints.size
                    val spacing = if (totalPoints > 1) width / (totalPoints - 1) else width

                    drawLine(color = axisColor, start = Offset(0f, height), end = Offset(width, height), strokeWidth = 1f)

                    val points = state.lineChartPoints.mapIndexed { index, data ->
                        Offset(index * spacing, height - (((data.balance - state.minTimelineBalance) / state.timelineRange).toFloat() * height))
                    }

                    if (points.isNotEmpty()) {
                        val strokePath = Path().apply { moveTo(points[0].x, points[0].y) }
                        val fillPath = Path().apply { moveTo(points[0].x, points[0].y) }

                        for (i in 0 until points.size - 1) {
                            val controlX = points[i].x + (points[i + 1].x - points[i].x) / 2f
                            strokePath.cubicTo(controlX, points[i].y, controlX, points[i + 1].y, points[i + 1].x, points[i + 1].y)
                            fillPath.cubicTo(controlX, points[i].y, controlX, points[i + 1].y, points[i + 1].x, points[i + 1].y)
                        }
                        fillPath.lineTo(width, height)
                        fillPath.lineTo(0f, height)
                        fillPath.close()

                        drawPath(path = fillPath, brush = Brush.verticalGradient(listOf(primaryColor.copy(alpha = 0.25f), Color.Transparent), 0f, height))
                        drawPath(path = strokePath, color = primaryColor, style = Stroke(width = 2.5.dp.toPx()))

                        state.selectedChartIndex?.let { index ->
                            if (index < points.size) {
                                drawLine(primaryColor.copy(alpha = 0.4f), Offset(points[index].x, 0f), Offset(points[index].x, height), 1.5.dp.toPx())
                                drawCircle(primaryColor, 6.dp.toPx(), points[index])
                                drawCircle(Color.White, 2.5.dp.toPx(), points[index])
                            }
                        }
                        state.lineChartPoints.forEachIndexed { index, data ->
                            if (data.hasTransaction && index != state.selectedChartIndex) {
                                drawCircle(primaryColor.copy(alpha = 0.6f), 2.dp.toPx(), points[index])
                            }
                        }
                    }

                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply { color = labelColor; textSize = 22f; textAlign = android.graphics.Paint.Align.LEFT }
                        drawText("${state.maxTimelineBalance.toInt()} €", 0f, -10f, paint)
                        paint.textAlign = android.graphics.Paint.Align.CENTER
                        val formatter = DateTimeFormatter.ofPattern("dd MMM")
                        drawText(state.lineChartPoints.first().date.format(formatter), 25f, height + 32f, paint)
                        drawText(state.lineChartPoints.last().date.format(formatter), width - 25f, height + 32f, paint)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(state: WalletState, viewModel: WalletViewModel) {
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
        title = { Text("Nuova Transazione", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp)).background(Color.LightGray.copy(alpha = 0.2f)).padding(3.dp)) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(9.dp)).background(if (!state.newTransactionIsIncome) Color(0xFFFFEAEA) else Color.Transparent).clickable { viewModel.onAction(WalletAction.OnTypeChanged(false)) }, contentAlignment = Alignment.Center) {
                        Text("Uscita", color = if (!state.newTransactionIsIncome) Color(0xFFC62828) else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(9.dp)).background(if (state.newTransactionIsIncome) Color(0xFFE8F5E9) else Color.Transparent).clickable { viewModel.onAction(WalletAction.OnTypeChanged(true)) }, contentAlignment = Alignment.Center) {
                        Text("Entrata", color = if (state.newTransactionIsIncome) Color(0xFF2E7D32) else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                UniSphereTextField(value = state.newTransactionTitle, onValueChange = { viewModel.onAction(WalletAction.OnTitleChanged(it)) }, label = "Titolo", leadingIcon = Icons.Default.Title, modifier = Modifier.fillMaxWidth())
                UniSphereTextField(value = state.newTransactionAmount, onValueChange = { viewModel.onAction(WalletAction.OnAmountChanged(it)) }, label = "Importo", leadingIcon = Icons.Default.AttachMoney, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

                ExposedDropdownMenuBox(expanded = state.isCategoryDropdownExpanded, onExpandedChange = { viewModel.onAction(WalletAction.ToggleCategoryDropdown(it)) }) {
                    UniSphereTextField(
                        value = selectedCategory?.name ?: "Seleziona Categoria",
                        onValueChange = {},
                        label = "Categoria",
                        leadingIcon = Icons.Default.Category,
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = state.isCategoryDropdownExpanded) }
                    )
                    ExposedDropdownMenu(expanded = state.isCategoryDropdownExpanded, onDismissRequest = { viewModel.onAction(WalletAction.ToggleCategoryDropdown(false)) }) {
                        DropdownMenuItem(text = { Text("+ Nuova Categoria", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }, onClick = { viewModel.onAction(WalletAction.ToggleCategoryCreationDialog(true)) })
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
                                        IconButton(onClick = { viewModel.onAction(WalletAction.OnRequestDeleteCategory(cat)) }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                onClick = { viewModel.onAction(WalletAction.OnCategoryChanged(cat.id)) }
                            )
                        }
                    }
                }

                OutlinedCard(onClick = { viewModel.onAction(WalletAction.ToggleDatePicker) }, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, null, tint = Color.Gray)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Data: ${state.newTransactionDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}", fontWeight = FontWeight.Medium)
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { viewModel.onAction(WalletAction.OnSaveTransactionClicked) }) { Text("Aggiungi") } }
    )

    if (state.showCategoryCreationDialog) {
        val palette = listOf("#FF3B30", "#FF9500", "#FFCC00", "#34C759", "#007AFF", "#5856D6", "#AF52DE", "#8E8E93")
        AlertDialog(
            onDismissRequest = { viewModel.onAction(WalletAction.ToggleCategoryCreationDialog(false)) },
            title = { Text("Nuova Categoria", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    UniSphereTextField(value = state.newCategoryName, onValueChange = { viewModel.onAction(WalletAction.OnNewCategoryNameChanged(it)) }, label = "Nome Categoria", modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        palette.forEach { hex ->
                            Box(Modifier.size(28.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(hex))).clickable { viewModel.onAction(WalletAction.OnNewCategoryColorChanged(hex)) }.border(if (state.newCategoryColorHex == hex) 2.dp else 0.dp, MaterialTheme.colorScheme.primary, CircleShape))
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.onAction(WalletAction.OnCreateCategoryType(state.newCategoryName, state.newCategoryColorHex)) }) { Text("Crea") }
            }
        )
    }

    state.categoryToDelete?.let { cat ->
        UniSphereAlertDialog(
            title = "Elimina Categoria",
            text = "Eliminando la categoria \"${cat.name}\" eliminerai anche tutte le transazioni collegate ad essa. Continuare?",
            confirmText = "Elimina",
            onConfirm = { viewModel.onAction(WalletAction.OnConfirmDeleteCategory) },
            onDismiss = { viewModel.onAction(WalletAction.OnRequestDeleteCategory(null)) },
            dismissText = "Annulla"
        )
    }
}

@Composable
fun EmptyDashboardState(onAddClick: () -> Unit) {
    UniSphereEmptyState(
        icon = Icons.Default.AccountBalanceWallet,
        title = "Nessun movimento registrato",
        description = "Il tuo riepilogo finanziario personale è vuoto. Inizia a tracciare le tue spese quotidiane o le tue entrate per sbloccare i grafici.",
        actionButton = {
            Button(onClick = onAddClick, shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Aggiungi Prima Transazione", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun PieChartSection(slices: List<PieSlice>) {
    Card(
        modifier = Modifier.fillMaxWidth().height(220.dp).padding(top = 10.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Canvas(modifier = Modifier.size(130.dp)) {
                var startAngle = 0f
                slices.forEach { slice ->
                    drawArc(color = Color(android.graphics.Color.parseColor(slice.colorHex)), startAngle = startAngle, sweepAngle = slice.sweepAngle, useCenter = true)
                    startAngle += slice.sweepAngle
                }
            }
            Column(modifier = Modifier.padding(start = 24.dp).weight(1f)) {
                slices.forEach { slice ->
                    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(Color(android.graphics.Color.parseColor(slice.colorHex)), CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(slice.categoryName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: TransactionEntity, categoryName: String, colorHex: String, onClick: () -> Unit) {
    val barColor = remember(colorHex) { try { Color(android.graphics.Color.parseColor(colorHex)) } catch (_: Exception) { Color.Gray } }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        UniSphereListItem(
            headlineText = transaction.title,
            supportingText = "$categoryName • ${transaction.date.format(DateTimeFormatter.ofPattern("dd MMM"))}",
            leadingBarColor = barColor,
            onClick = onClick,
            trailingContent = {
                Text(
                    text = if (transaction.isIncome) "+€${transaction.amount}" else "-€${transaction.amount}",
                    color = if (transaction.isIncome) Color(0xFF2E7D32) else Color(0xFFC62828),
                    fontWeight = FontWeight.Bold, fontSize = 15.sp
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailsDialog(state: WalletState, viewModel: WalletViewModel) {
    val transaction = state.selectedTransaction ?: return

    if (state.showDetailDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = state.detailDateValue.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { viewModel.onAction(WalletAction.ToggleDetailDatePicker) },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.onAction(WalletAction.OnDetailDateChanged(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate())) }
                }) { Text("OK") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    AlertDialog(
        onDismissRequest = { viewModel.onAction(WalletAction.OnTransactionSelected(null)) },
        title = { Text(if (state.isDetailEditingActive) "Modifica Transazione" else "Dettagli Transazione", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.isDetailEditingActive) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !state.detailIsIncomeValue, onClick = { viewModel.onAction(WalletAction.OnDetailTypeChanged(false)) }, label = { Text("Uscita") }, modifier = Modifier.weight(1f))
                        FilterChip(selected = state.detailIsIncomeValue, onClick = { viewModel.onAction(WalletAction.OnDetailTypeChanged(true)) }, label = { Text("Entrata") }, modifier = Modifier.weight(1f))
                    }
                    UniSphereTextField(value = state.detailTitleText, onValueChange = { viewModel.onAction(WalletAction.OnDetailTitleChanged(it)) }, label = "Titolo", modifier = Modifier.fillMaxWidth())
                    UniSphereTextField(value = state.detailAmountText, onValueChange = { viewModel.onAction(WalletAction.OnDetailAmountChanged(it)) }, label = "Importo", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

                    ExposedDropdownMenuBox(expanded = state.isDetailCategoryDropdownExpanded, onExpandedChange = { viewModel.onAction(WalletAction.ToggleDetailCategoryDropdown(it)) }) {
                        UniSphereTextField(
                            value = state.categories.find { it.id == state.detailCategoryId }?.name ?: "Seleziona",
                            onValueChange = {},
                            label = "Categoria",
                            leadingIcon = Icons.Default.Category,
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = state.isDetailCategoryDropdownExpanded) }
                        )
                        ExposedDropdownMenu(expanded = state.isDetailCategoryDropdownExpanded, onDismissRequest = { viewModel.onAction(WalletAction.ToggleDetailCategoryDropdown(false)) }) {
                            state.categories.forEach { cat -> DropdownMenuItem(text = { Text(cat.name) }, onClick = { viewModel.onAction(WalletAction.OnDetailCategoryChanged(cat.id)) }) }
                        }
                    }
                    OutlinedCard(onClick = { viewModel.onAction(WalletAction.ToggleDetailDatePicker) }, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, null, tint = Color.Gray)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Data: ${state.detailDateValue.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}", fontWeight = FontWeight.Medium)
                        }
                    }
                } else {
                    val matchedCat = state.categories.find { it.id == transaction.categoryId }
                    Text("Titolo: ${transaction.title}", fontWeight = FontWeight.Bold)
                    Text("Importo: ${if (transaction.isIncome) "+" else "-"}€${transaction.amount}")
                    Text("Categoria: ${matchedCat?.name ?: "Altro"}")
                    Text("Data: ${transaction.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}")
                }
            }
        },
        confirmButton = {
            if (state.isDetailEditingActive) {
                Button(onClick = { viewModel.onAction(WalletAction.OnSaveUpdateTransactionClicked) }) { Text("Salva") }
            } else {
                TextButton(onClick = { viewModel.onAction(WalletAction.OnToggleDetailEditing) }) { Text("Modifica") }
            }
        },
        dismissButton = { TextButton(onClick = { viewModel.onAction(WalletAction.OnDeleteTransactionClicked) }) { Text("Elimina", color = Color.Red) } }
    )
}