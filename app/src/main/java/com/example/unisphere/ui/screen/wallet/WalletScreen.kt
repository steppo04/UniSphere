package com.example.unisphere.ui.screen.wallet

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.unisphere.ui.composables.AppBar
import com.example.unisphere.ui.composables.BottomNavigationBar
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.random.Random

data class Transaction(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val category: String,
    val color: Color,
    val date: LocalDate = LocalDate.now(),
    val isIncome: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(navController: NavHostController) {
    var transactions by remember {
        mutableStateOf(
            mutableListOf(
                Transaction(title = "Spesa Conad", amount = 45.50, category = "Cibo", color = Color(0xFFFF9800), date = LocalDate.now().minusDays(5), isIncome = false),
                Transaction(title = "Affitto Aprile", amount = 450.0, category = "Casa", color = Color(0xFF2196F3), date = LocalDate.now().minusDays(10), isIncome = false),
                Transaction(title = "Bolletta Luce", amount = 32.20, category = "Utenze", color = Color(0xFF4CAF50), date = LocalDate.now().minusDays(2), isIncome = false),
                Transaction(title = "Stipendio", amount = 1200.0, category = "Lavoro", color = Color(0xFF4CAF50), date = LocalDate.now().minusDays(15), isIncome = true),
                Transaction(title = "Netflix", amount = 12.99, category = "Svago", color = Color(0xFFE91E63), date = LocalDate.now().minusDays(1), isIncome = false)
            )
        )
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }

    Scaffold(
        topBar = { AppBar(title = "UniWallet", navController = navController) },
        bottomBar = { BottomNavigationBar(navController = navController) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
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
                PieChartSection(transactions.filter { !it.isIncome })
            }

            item {
                Text("Andamento Mensile (Netto)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                LineChartSection(transactions)
            }

            item {
                Text("Transazioni Recenti", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            items(transactions.sortedByDescending { it.date }, key = { it.id }) { transaction ->
                TransactionItem(
                    transaction = transaction,
                    onClick = { selectedTransaction = transaction }
                )
            }
        }
    }

    if (showAddDialog) {
        AddTransactionDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, amount, category, date, isIncome ->
                val newColor = when(category) {
                    "Cibo" -> Color(0xFFFF9800)
                    "Casa" -> Color(0xFF2196F3)
                    "Utenze" -> Color(0xFF4CAF50)
                    "Lavoro" -> Color(0xFF4CAF50)
                    else -> generateUniqueRandomColor(transactions.map { it.color })
                }
                transactions = (transactions + Transaction(
                    title = title, 
                    amount = amount, 
                    category = category, 
                    color = newColor,
                    date = date,
                    isIncome = isIncome
                )).toMutableList()
                showAddDialog = false
            }
        )
    }

    if (selectedTransaction != null) {
        TransactionDetailsDialog(
            transaction = selectedTransaction!!,
            onDismiss = { selectedTransaction = null },
            onDelete = {
                transactions = transactions.filter { it.id != selectedTransaction!!.id }.toMutableList()
                selectedTransaction = null
            },
            onUpdate = { updatedTransaction ->
                transactions = transactions.map { 
                    if (it.id == updatedTransaction.id) updatedTransaction else it 
                }.toMutableList()
                selectedTransaction = null
            }
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

@Composable
fun PieChartSection(transactions: List<Transaction>) {
    val total = transactions.sumOf { it.amount }
    val categories = transactions.groupBy { it.category }
    
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
                    categories.forEach { (_, trans) ->
                        val categoryTotal = trans.sumOf { it.amount }
                        val sweepAngle = (categoryTotal.toFloat() / total.toFloat()) * 360f
                        drawArc(
                            color = trans.first().color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true
                        )
                        startAngle += sweepAngle
                    }
                }
            }
            
            Column(modifier = Modifier.padding(start = 16.dp).verticalScroll(rememberScrollState())) {
                categories.forEach { (name, trans) ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(12.dp).background(trans.first().color, CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(name, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun LineChartSection(transactions: List<Transaction>) {
    val initialBalance = 0.0
    val sortedTransactions = transactions.sortedBy { it.date }
    
    val balanceTimeline = mutableListOf<Triple<LocalDate, Double, Boolean>>()
    var currentBalance = initialBalance
    
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

            drawLine(
                color = axisColor,
                start = Offset(0f, height),
                end = Offset(width, height),
                strokeWidth = 2f
            )
            drawLine(
                color = axisColor,
                start = Offset(0f, 0f),
                end = Offset(0f, height),
                strokeWidth = 2f
            )

            val path = Path()
            balanceTimeline.forEachIndexed { index, data ->
                val x = index * spacing
                val normalizedY = ((data.second - minBalance) / range).toFloat()
                val y = height - (normalizedY * height)
                
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                
                if (data.third) {
                    drawCircle(
                        color = Color(0xFF6200EE),
                        radius = 4.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }
            
            drawPath(
                path = path,
                color = Color(0xFF6200EE),
                style = Stroke(width = 2.dp.toPx())
            )

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
fun TransactionItem(transaction: Transaction, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(10.dp).background(transaction.color, CircleShape))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.title, fontWeight = FontWeight.Bold)
                Text("${transaction.category} • ${transaction.date.format(DateTimeFormatter.ofPattern("dd MMM"))}", style = MaterialTheme.typography.bodySmall)
            }
            val displayAmount = if (transaction.isIncome) "+€${transaction.amount}" else "-€${transaction.amount}"
            val amountColor = if (transaction.isIncome) Color(0xFF4CAF50) else Color.Red
            
            Text(displayAmount, color = amountColor, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailsDialog(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onUpdate: (Transaction) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    
    var title by remember { mutableStateOf(transaction.title) }
    var amount by remember { mutableStateOf(transaction.amount.toString()) }
    var category by remember { mutableStateOf(transaction.category) }
    var date by remember { mutableStateOf(transaction.date) }
    var isIncome by remember { mutableStateOf(transaction.isIncome) }
    
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Annulla") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Modifica Transazione" else "Dettagli Transazione") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isEditing) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !isIncome,
                            onClick = { isIncome = false },
                            label = { Text("Uscita") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = isIncome,
                            onClick = { isIncome = true },
                            label = { Text("Entrata") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Titolo") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Importo") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Categoria") }, modifier = Modifier.fillMaxWidth())
                    OutlinedCard(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = "Data: ${date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}")
                        }
                    }
                } else {
                    Text("Titolo: ${transaction.title}", fontWeight = FontWeight.Bold)
                    Text("Importo: ${if (transaction.isIncome) "+" else "-"}€${transaction.amount}")
                    Text("Categoria: ${transaction.category}")
                    Text("Data: ${transaction.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}")
                    Text("Tipo: ${if (transaction.isIncome) "Entrata" else "Uscita"}")
                }
            }
        },
        confirmButton = {
            if (isEditing) {
                Button(onClick = {
                    onUpdate(transaction.copy(
                        title = title,
                        amount = amount.toDoubleOrNull() ?: 0.0,
                        category = category,
                        date = date,
                        isIncome = isIncome
                    ))
                }) { Text("Salva") }
            } else {
                TextButton(onClick = { isEditing = true }) { 
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Modifica") 
                }
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Elimina")
                }
                TextButton(onClick = onDismiss) { Text("Chiudi") }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(onDismiss: () -> Unit, onConfirm: (String, Double, String, LocalDate, Boolean) -> Unit) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Cibo") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var isIncome by remember { mutableStateOf(false) }
    
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Annulla") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuova Transazione") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isIncome,
                        onClick = { isIncome = false },
                        label = { Text("Uscita") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = isIncome,
                        onClick = { isIncome = true },
                        label = { Text("Entrata") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titolo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Importo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Categoria") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedCard(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "Data: ${date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountDouble = amount.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amountDouble > 0) {
                        onConfirm(title, amountDouble, category.ifBlank { "Altro" }, date, isIncome)
                    }
                }
            ) {
                Text("Aggiungi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}