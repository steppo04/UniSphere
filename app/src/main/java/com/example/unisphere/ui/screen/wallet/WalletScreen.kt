package com.example.unisphere.ui.screen.wallet

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.unisphere.ui.composables.AppBar
import com.example.unisphere.ui.composables.BottomNavigationBar
import kotlin.math.cos
import kotlin.math.sin

data class Transaction(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val category: String,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(navController: NavHostController) {
    var transactions by remember {
        mutableStateOf(
            mutableListOf(
                Transaction(title = "Spesa Conad", amount = 45.50, category = "Cibo", color = Color(0xFFFF9800)),
                Transaction(title = "Affitto Aprile", amount = 450.0, category = "Casa", color = Color(0xFF2196F3)),
                Transaction(title = "Bolletta Luce", amount = 32.20, category = "Utenze", color = Color(0xFF4CAF50)),
                Transaction(title = "Netflix", amount = 12.99, category = "Svago", color = Color(0xFFE91E63))
            )
        )
    }

    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { AppBar(title = "Wallet", navController = navController) },
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
                Text("Riepilogo Categorie", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                PieChartSection(transactions)
            }

            item {
                Text("Andamento Mensile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                LineChartSection()
            }

            item {
                Text("Transazioni Recenti", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            items(transactions, key = { it.id }) { transaction ->
                TransactionItem(
                    transaction = transaction,
                    onDelete = {
                        transactions = transactions.filter { it.id != transaction.id }.toMutableList()
                    }
                )
            }
        }
    }

    if (showAddDialog) {
        AddTransactionDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, amount, category ->
                val newColor = when(category) {
                    "Cibo" -> Color(0xFFFF9800)
                    "Casa" -> Color(0xFF2196F3)
                    "Utenze" -> Color(0xFF4CAF50)
                    else -> Color(0xFFE91E63)
                }
                transactions = (transactions + Transaction(title = title, amount = amount, category = category, color = newColor)).toMutableList()
                showAddDialog = false
            }
        )
    }
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
                var startAngle = 0f
                categories.forEach { (_, trans) ->
                    val sweepAngle = (trans.sumOf { it.amount }.toFloat() / total.toFloat()) * 360f
                    drawArc(
                        color = trans.first().color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true
                    )
                    startAngle += sweepAngle
                }
            }
            
            Column(modifier = Modifier.padding(start = 16.dp)) {
                categories.forEach { (name, trans) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
fun LineChartSection() {
    Card(
        modifier = Modifier.fillMaxWidth().height(200.dp).padding(top = 16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val points = listOf(Offset(0f, 150f), Offset(100f, 120f), Offset(200f, 180f), Offset(300f, 50f), Offset(400f, 100f))
            for (i in 0 until points.size - 1) {
                drawLine(
                    color = Color(0xFF6200EE),
                    start = points[i],
                    end = points[i+1],
                    strokeWidth = 4f
                )
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction, onDelete: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
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
                Text(transaction.category, style = MaterialTheme.typography.bodySmall)
            }
            Text("-€${transaction.amount}", color = Color.Red, fontWeight = FontWeight.Bold)
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = Color.Gray)
            }
        }
    }
}

@Composable
fun AddTransactionDialog(onDismiss: () -> Unit, onConfirm: (String, Double, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Cibo") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuova Spesa") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Titolo") })
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Importo") })
                // Semplificato per l'esempio
                Text("Categoria: Cibo, Casa, Utenze, Svago")
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Categoria") })
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(title, amount.toDoubleOrNull() ?: 0.0, category) }) {
                Text("Aggiungi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}