package com.example.unisphere.ui.screen.wallet

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    navController: NavHostController,
    viewModel: WalletViewModel = hiltViewModel()
) {
    val state = viewModel.state

    val totaleEntrate = state.transactions.filter { it.isIncome }.sumOf { it.amount }
    val totaleUscite = state.transactions.filter { !it.isIncome }.sumOf { it.amount }
    val saldoNetto = totaleEntrate - totaleUscite

    Scaffold(
        topBar = { AppBar(title = "UniWallet", navController = navController) },
        bottomBar = { BottomNavigationBar(navController = navController) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onAction(WalletAction.OnAddClicked) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi")
            }
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
                AppleWalletHeroCard(saldoNetto = saldoNetto, entrate = totaleEntrate, uscite = totaleUscite)
            }

            if (state.transactions.isEmpty()) {
                item {
                    AppleWalletEmptyState { viewModel.onAction(WalletAction.OnAddClicked) }
                }
            } else {
                item {
                    Text("Riepilogo Spese", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
                    PieChartSection(state.transactions.filter { !it.isIncome }, state.categories)
                }

                item {
                    Text("Andamento Patrimoniale", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
                    LineChartSection(state.transactions)
                }

                item {
                    Text("Transazioni Recenti", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
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

            item { Spacer(modifier = Modifier.height(24.dp)) }
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
fun AppleWalletHeroCard(saldoNetto: Double, entrate: Double, uscite: Double) {
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFE8F5E9))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
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
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFFFEAEA))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
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

@Composable
fun LineChartSection(transactions: List<TransactionEntity>) {
    val sortedTransactions = transactions.sortedBy { it.date }
    val balanceTimeline = mutableListOf<Triple<LocalDate, Double, Boolean>>()
    var currentBalance = 0.0

    // SANA CORREZIONE: Usiamo una variabile tempDate mutabile (var) per evitare il loop infinito
    var tempDate = LocalDate.now().minusDays(30)
    val endDate = LocalDate.now()

    while (!tempDate.isAfter(endDate)) {
        val daysTransactions = sortedTransactions.filter { it.date == tempDate }
        val hasChange = daysTransactions.isNotEmpty()
        if (hasChange) {
            currentBalance += daysTransactions.sumOf { if (it.isIncome) it.amount else -it.amount }
        }
        balanceTimeline.add(Triple(tempDate, currentBalance, hasChange))
        tempDate = tempDate.plusDays(1) // Ora tempDate incrementa correttamente e il ciclo finisce!
    }

    val maxBalance = balanceTimeline.maxOfOrNull { it.second } ?: 100.0
    val minBalance = balanceTimeline.minOfOrNull { it.second } ?: 0.0
    val range = (maxBalance - minBalance).coerceAtLeast(100.0)

    var selectedIndex by remember { mutableStateOf<Int?>(null) }

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

            AnimatedContent(
                targetState = selectedIndex,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "HeaderAnim"
            ) { targetIndex ->
                if (targetIndex != null && targetIndex < balanceTimeline.size) {
                    val infoGiorno = balanceTimeline[targetIndex]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = infoGiorno.first.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                        Text(text = String.format(java.util.Locale.US, "Saldo: %.2f €", infoGiorno.second), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = if (infoGiorno.second >= 0) Color(0xFF2E7D32) else Color(0xFFC62828))
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.TrendingUp, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Tocca la curva per i dettagli giornalieri", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .pointerInput(balanceTimeline) {
                            detectTapGestures { offset ->
                                val totalPoints = balanceTimeline.size
                                if (totalPoints > 1) {
                                    val drawingWidth = size.width
                                    val spacing = drawingWidth / (totalPoints - 1)
                                    val closestIndex = (offset.x / spacing).roundToInt().coerceIn(0, totalPoints - 1)
                                    selectedIndex = closestIndex
                                }
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height
                    val totalPoints = balanceTimeline.size
                    val spacing = if (totalPoints > 1) width / (totalPoints - 1) else width

                    drawLine(color = axisColor, start = Offset(0f, height), end = Offset(width, height), strokeWidth = 1f)

                    val points = balanceTimeline.mapIndexed { index, data ->
                        val x = index * spacing
                        val normalizedY = ((data.second - minBalance) / range).toFloat()
                        val y = height - (normalizedY * height)
                        Offset(x, y)
                    }

                    val strokePath = Path()
                    val fillPath = Path()

                    if (points.isNotEmpty()) {
                        strokePath.moveTo(points[0].x, points[0].y)
                        fillPath.moveTo(points[0].x, points[0].y)

                        for (i in 0 until points.size - 1) {
                            val p1 = points[i]
                            val p2 = points[i + 1]
                            val controlX = p1.x + (p2.x - p1.x) / 2f
                            strokePath.cubicTo(controlX, p1.y, controlX, p2.y, p2.x, p2.y)
                            fillPath.cubicTo(controlX, p1.y, controlX, p2.y, p2.x, p2.y)
                        }

                        fillPath.lineTo(width, height)
                        fillPath.lineTo(0f, height)
                        fillPath.close()

                        drawPath(path = fillPath, brush = Brush.verticalGradient(colors = listOf(primaryColor.copy(alpha = 0.25f), Color.Transparent), startY = 0f, endY = height))
                        drawPath(path = strokePath, color = primaryColor, style = Stroke(width = 2.5.dp.toPx()))

                        selectedIndex?.let { index ->
                            if (index < points.size) {
                                val selectedPoint = points[index]
                                drawLine(color = primaryColor.copy(alpha = 0.4f), start = Offset(selectedPoint.x, 0f), end = Offset(selectedPoint.x, height), strokeWidth = 1.5.dp.toPx())
                                drawCircle(color = primaryColor, radius = 6.dp.toPx(), center = selectedPoint)
                                drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = selectedPoint)
                            }
                        }

                        balanceTimeline.forEachIndexed { index, data ->
                            if (data.third && index != selectedIndex) {
                                drawCircle(color = primaryColor.copy(alpha = 0.6f), radius = 2.dp.toPx(), center = points[index])
                            }
                        }
                    }

                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = labelColor
                            textSize = 22f
                            textAlign = android.graphics.Paint.Align.LEFT
                        }
                        drawText("${maxBalance.toInt()} €", 0f, -10f, paint)

                        paint.textAlign = android.graphics.Paint.Align.CENTER
                        val formatter = DateTimeFormatter.ofPattern("dd MMM")
                        drawText(balanceTimeline.first().first.format(formatter), 25f, height + 32f, paint)
                        drawText(balanceTimeline.last().first.format(formatter), width - 25f, height + 32f, paint)
                    }
                }
            }
        }
    }
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
        title = { Text("Nuova Transazione", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.LightGray.copy(alpha = 0.2f))
                        .padding(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (!state.newTransactionIsIncome) Color(0xFFFFEAEA) else Color.Transparent)
                            .clickable { viewModel.onAction(WalletAction.OnTypeChanged(false)) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Uscita", color = if (!state.newTransactionIsIncome) Color(0xFFC62828) else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (state.newTransactionIsIncome) Color(0xFFE8F5E9) else Color.Transparent)
                            .clickable { viewModel.onAction(WalletAction.OnTypeChanged(true)) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Entrata", color = if (state.newTransactionIsIncome) Color(0xFF2E7D32) else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                OutlinedTextField(value = state.newTransactionTitle, onValueChange = { viewModel.onAction(WalletAction.OnTitleChanged(it)) }, label = { Text("Titolo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = state.newTransactionAmount, onValueChange = { viewModel.onAction(WalletAction.OnAmountChanged(it)) }, label = { Text("Importo") }, singleLine = true, modifier = Modifier.fillMaxWidth())

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
                        Icon(Icons.Default.CalendarToday, null, tint = Color.Gray)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Data: ${state.newTransactionDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}", fontWeight = FontWeight.Medium)
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
            title = { Text("Nuova Categoria", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = newCatName, onValueChange = { newCatName = it }, label = { Text("Nome Categoria") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        palette.forEach { hex ->
                            Box(Modifier.size(28.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(hex))).clickable { selectedColorHex = hex }.border(if (selectedColorHex == hex) 2.dp else 0.dp, MaterialTheme.colorScheme.primary, CircleShape))
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.onAction(WalletAction.OnCreateCategoryType(newCatName, selectedColorHex))
                    showCatDialog = false
                }) { Text("Crea") }
            }
        )
    }

    catToDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { catToDelete = null },
            title = { Text("Elimina Categoria", fontWeight = FontWeight.Bold) },
            text = { Text("Eliminando la categoria \"${cat.name}\" eliminerai anche tutte le transazioni collegate ad essa. Continuare?") },
            confirmButton = { Button(onClick = { viewModel.onAction(WalletAction.OnDeleteCategoryType(cat)); catToDelete = null }) { Text("Elimina") } }
        )
    }
}

@Composable
fun AppleWalletEmptyState(onAddClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(72.dp).clip(CircleShape).background(Brush.linearGradient(colors = listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.secondaryContainer))),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(text = "Nessun movimento registrato", fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Il tuo riepilogo finanziario personale è vuoto. Inizia a tracciare le tue spese quotidiane o le tue entrate per sbloccare i grafici.", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center, lineHeight = 18.sp)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onAddClick, shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Aggiungi Prima Transazione", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun PieChartSection(transactions: List<TransactionEntity>, categories: List<TransactionCategoryEntity>) {
    val total = transactions.sumOf { it.amount }
    val groupedByCat = transactions.groupBy { it.categoryId }

    Card(
        modifier = Modifier.fillMaxWidth().height(220.dp).padding(top = 10.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(modifier = Modifier.size(130.dp)) {
                if (total > 0) {
                    var startAngle = 0f
                    groupedByCat.forEach { (catId, transList) ->
                        val catColorHex = categories.find { it.id == catId }?.colorHex ?: "#8E8E93"
                        val categoryTotal = transList.sumOf { it.amount }
                        val sweepAngle = (categoryTotal.toFloat() / total.toFloat()) * 360f
                        drawArc(color = Color(android.graphics.Color.parseColor(catColorHex)), startAngle = startAngle, sweepAngle = sweepAngle, useCenter = true)
                        startAngle += sweepAngle
                    }
                }
            }
            // SANA MODIFICA: rimosso l'ulteriore scroll interno per evitare loop di misurazione con la LazyColumn esterna
            Column(modifier = Modifier.padding(start = 24.dp).weight(1f)) {
                groupedByCat.forEach { (catId, _) ->
                    val category = categories.find { it.id == catId }
                    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(Color(android.graphics.Color.parseColor(category?.colorHex ?: "#8E8E93")), CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(category?.name ?: "Altro", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: TransactionEntity, categoryName: String, colorHex: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(10.dp).background(Color(android.graphics.Color.parseColor(colorHex)), CircleShape))
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("$categoryName • ${transaction.date.format(DateTimeFormatter.ofPattern("dd MMM"))}", fontSize = 12.sp, color = Color.Gray)
            }
            val displayAmount = if (transaction.isIncome) "+€${transaction.amount}" else "-€${transaction.amount}"
            val amountColor = if (transaction.isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)
            Text(displayAmount, color = amountColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
        title = { Text(if (isEditing) "Modifica Transazione" else "Dettagli Transazione", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isEditing) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !isIncome, onClick = { isIncome = false }, label = { Text("Uscita") }, modifier = Modifier.weight(1f))
                        FilterChip(selected = isIncome, onClick = { isIncome = true }, label = { Text("Entrata") }, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Titolo") })
                    OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Importo") })
                    var expandedCats by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expandedCats, onExpandedChange = { expandedCats = it }) {
                        OutlinedTextField(value = state.categories.find { it.id == categoryId }?.name ?: "Seleziona", onValueChange = {}, readOnly = true, label = { Text("Categoria") }, modifier = Modifier.menuAnchor().fillMaxWidth())
                        ExposedDropdownMenu(expanded = expandedCats, onDismissRequest = { expandedCats = false }) {
                            state.categories.forEach { cat -> DropdownMenuItem(text = { Text(cat.name) }, onClick = { categoryId = cat.id; expandedCats = false }) }
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
                Button(onClick = { viewModel.onAction(WalletAction.OnUpdateTransactionClicked(transaction.copy(title = title, amount = amount.toDoubleOrNull() ?: 0.0, categoryId = categoryId, date = date, isIncome = isIncome))) }) { Text("Salva") }
            } else {
                TextButton(onClick = { isEditing = true }) { Text("Modifica") }
            }
        },
        dismissButton = { TextButton(onClick = { viewModel.onAction(WalletAction.OnDeleteTransactionClicked) }) { Text("Elimina", color = Color.Red) } }
    )
}