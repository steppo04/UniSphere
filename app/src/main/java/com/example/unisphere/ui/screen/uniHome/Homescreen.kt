package com.example.unisphere.ui.screen.uniHome

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.unisphere.db.local.entity.HouseMemberEntity
import com.example.unisphere.db.local.entity.HouseInvitationEntity
import com.example.unisphere.db.local.entity.UserEntity
import com.example.unisphere.repository.CleaningRotationalState
import com.example.unisphere.repository.UserBalance
import com.example.unisphere.repository.HouseRepository.TransactionWithSplits
import com.example.unisphere.ui.composables.AppBar
import com.example.unisphere.ui.composables.BottomNavigationBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: UniHomeViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val snackbarHostState = remember { SnackbarHostState() }

    var showInviteDialog by remember { mutableStateOf(false) }
    var showServiceDialog by remember { mutableStateOf(false) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var showSettleDialog by remember { mutableStateOf(false) }
    var showAllTxDetailsPage by remember { mutableStateOf(false) }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onAction(UniHomeAction.OnDismissSnackbar)
        }
    }

    Scaffold(
        topBar = {
            AppBar(
                title = if (state.hasHouse) state.houseName else "UniHome",
                navController = navController
            )
        },
        bottomBar = { BottomNavigationBar(navController) },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Card(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.isSuccessSnackbar) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (state.isSuccessSnackbar) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Text(
                            text = data.visuals.message,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (!state.hasHouse) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Brush.verticalGradient(colors = listOf(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f), MaterialTheme.colorScheme.surface)))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Icon(Icons.Default.HomeWork, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("Crea o unisciti ad una UniHome", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                    Text("Unisci le forze con i tuoi coinquilini per organizzare la casa in modo intelligente.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)

                    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AddHome, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Crea una nuova casa", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            OutlinedTextField(value = state.newHouseName, onValueChange = { viewModel.onAction(UniHomeAction.OnNewHouseNameChanged(it)) }, label = { Text("Nome della casa") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            Button(onClick = { viewModel.onAction(UniHomeAction.OnCreateHouseClicked) }, modifier = Modifier.fillMaxWidth().height(48.dp), enabled = state.newHouseName.isNotBlank()) {
                                Text("Crea", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (state.pendingInvitations.isNotEmpty()) {
                        Text("Inviti Ricevuti", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                        state.pendingInvitations.forEach { invitation ->
                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Invito da: ${invitation.senderUsername}", fontWeight = FontWeight.Bold)
                                        Text("Entra in: ${invitation.houseName}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        FilledIconButton(onClick = { viewModel.onAction(UniHomeAction.OnAcceptInvitation(invitation)) }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFE8F5E9))) { Icon(Icons.Default.Check, null, tint = Color(0xFF2E7D32)) }
                                        FilledIconButton(onClick = { viewModel.onAction(UniHomeAction.OnDeclineInvitation(invitation.id)) }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFFFEBEE))) { Icon(Icons.Default.Close, null, tint = Color(0xFFC62828)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (showAllTxDetailsPage) {
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = { showAllTxDetailsPage = false }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                    }
                    Text("Registro Spese", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (state.transactionsWithSplits.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Nessuna spesa inserita finora.", color = Color.Gray) }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        state.transactionsWithSplits.forEach { item ->
                            val tx = item.transaction
                            val splitNames = item.splits.map { if(it.userUid == state.currentUserId) "Te" else it.username }.joinToString(", ")

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.4f))
                            ) {
                                Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                                    Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
                                    Row(
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(tx.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Text("Pagato da: ${tx.payerUsername} • ${tx.date}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Diviso con: $splitNames", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("${String.format(java.util.Locale.US, "%.2f", tx.amount)}€", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                                            IconButton(onClick = { viewModel.onAction(UniHomeAction.OnDeleteTransactionClicked(tx.id)) }) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "Elimina", tint = Color.Red.copy(alpha = 0.6f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // --- UI MAIN HUB ATTIVO ---
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                RoommatesSection(
                    members = state.members,
                    adminUid = state.adminUid,
                    currentUserId = state.currentUserId,
                    onInviteClick = { showInviteDialog = true },
                    onRemoveMember = { viewModel.onAction(UniHomeAction.OnRemoveMember(it)) }
                )

                CleaningSection(
                    rotations = state.cleaningRotations,
                    onAddServiceClick = { showServiceDialog = true },
                    onDeleteService = { viewModel.onAction(UniHomeAction.OnDeleteServiceClicked(it)) }
                )

                BalanceSection(
                    balances = state.balances,
                    onAddExpenseClick = { showExpenseDialog = true },
                    onSettleDebtClick = { showSettleDialog = true },
                    onOpenDetailsClick = { showAllTxDetailsPage = true }
                )

                if (state.currentUserId == state.adminUid) {
                    OutlinedButton(
                        onClick = { viewModel.onAction(UniHomeAction.OnDeleteHouseClicked) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.DeleteForever, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Elimina e Chiudi Casa", fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(
                        onClick = { viewModel.onAction(UniHomeAction.OnLeaveHouseClicked) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.ExitToApp, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Abbandona Gruppo Casa", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // --- POPUP DIALOGS ---

    if (showInviteDialog) {
        var dropdownExpanded by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showInviteDialog = false; viewModel.onAction(UniHomeAction.OnInviteQueryChanged("")) },
            title = { Text("Cerca Coinquilino", fontWeight = FontWeight.Bold) },
            text = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = state.inviteUserQuery, onValueChange = { viewModel.onAction(UniHomeAction.OnInviteQueryChanged(it)); dropdownExpanded = it.length >= 2 }, label = { Text("Digita lo username...") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    DropdownMenu(expanded = dropdownExpanded && state.searchedUsers.isNotEmpty(), onDismissRequest = { dropdownExpanded = false }, modifier = Modifier.fillMaxWidth(), properties = PopupProperties(focusable = false)) {
                        state.searchedUsers.forEach { user ->
                            DropdownMenuItem(text = { Text(user.username) }, onClick = { viewModel.onAction(UniHomeAction.OnSelectUserToInvite(user)); dropdownExpanded = false; showInviteDialog = false })
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showInviteDialog = false; viewModel.onAction(UniHomeAction.OnInviteQueryChanged("")) }) { Text("Annulla") } }
        )
    }

    if (showServiceDialog) {
        AlertDialog(
            onDismissRequest = { showServiceDialog = false },
            title = { Text("Nuovo Servizio Pulizie", fontWeight = FontWeight.Bold) },
            text = { OutlinedTextField(value = state.newServiceName, onValueChange = { viewModel.onAction(UniHomeAction.OnNewServiceNameChanged(it)) }, label = { Text("Nome Servizio (es. Cucina)") }, modifier = Modifier.fillMaxWidth(), singleLine = true) },
            confirmButton = { Button(onClick = { viewModel.onAction(UniHomeAction.OnAddCleaningServiceClicked); showServiceDialog = false }, enabled = state.newServiceName.isNotBlank()) { Text("Aggiungi") } },
            dismissButton = { TextButton(onClick = { showServiceDialog = false }) { Text("Annulla") } }
        )
    }

    if (showExpenseDialog) {
        val selectedMembersForSplit = remember { mutableStateListOf<HouseMemberEntity>().apply { addAll(state.members) } }
        AlertDialog(
            onDismissRequest = { showExpenseDialog = false },
            title = { Text("Inserisci Spesa", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = state.newExpenseTitle, onValueChange = { viewModel.onAction(UniHomeAction.OnNewExpenseTitleChanged(it)) }, label = { Text("Descrizione spesa") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = state.newExpenseAmount, onValueChange = { viewModel.onAction(UniHomeAction.OnNewExpenseAmountChanged(it)) }, label = { Text("Importo Totale (€)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Text("Dividi la quota con:", fontWeight = FontWeight.Bold)
                    state.members.forEach { member ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { if (selectedMembersForSplit.contains(member)) selectedMembersForSplit.remove(member) else selectedMembersForSplit.add(member) }) {
                            Checkbox(checked = selectedMembersForSplit.contains(member), onCheckedChange = { if (it == true) selectedMembersForSplit.add(member) else selectedMembersForSplit.remove(member) })
                            Text(member.username, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = { viewModel.onAction(UniHomeAction.OnAddExpenseClicked(selectedMembersForSplit.toList())); showExpenseDialog = false }, enabled = state.newExpenseTitle.isNotBlank() && state.newExpenseAmount.isNotBlank() && selectedMembersForSplit.isNotEmpty()) { Text("Salva") } },
            dismissButton = { TextButton(onClick = { showExpenseDialog = false }) { Text("Annulla") } }
        )
    }

    if (showSettleDialog) {
        var selectedDebtor by remember { mutableStateOf<UserBalance?>(null) }
        var settleAmount by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showSettleDialog = false },
            title = { Text("Salda Conto Diretto", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Seleziona chi stai pagando:")
                    state.balances.filter { it.userUid != state.currentUserId && it.netAmount > 0 }.forEach { balance ->
                        Row(
                            modifier = Modifier.fillMaxWidth().background(if (selectedDebtor?.userUid == balance.userUid) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, RoundedCornerShape(8.dp)).clickable { selectedDebtor = balance }.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedDebtor?.userUid == balance.userUid, onClick = null)
                            Text(balance.username, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    OutlinedTextField(value = settleAmount, onValueChange = { settleAmount = it }, label = { Text("Somma da trasferire (€)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amount = settleAmount.toDoubleOrNull() ?: 0.0
                    selectedDebtor?.let { viewModel.onAction(UniHomeAction.OnSettleDebtClicked(it.userUid, it.username, amount)) }
                    showSettleDialog = false
                }, enabled = selectedDebtor != null && settleAmount.isNotBlank()) { Text("Conferma Trasferimento") }
            },
            dismissButton = { TextButton(onClick = { showSettleDialog = false }) { Text("Annulla") } }
        )
    }
}

@Composable
fun RoommatesSection(
    members: List<HouseMemberEntity>,
    adminUid: String,
    currentUserId: String,
    onInviteClick: () -> Unit,
    onRemoveMember: (HouseMemberEntity) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Coinquilini Attivi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onInviteClick) { Icon(Icons.Default.PersonAdd, null, tint = MaterialTheme.colorScheme.primary) }
            }
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.fillMaxWidth()) {
                items(members) { member ->
                    val isAdmin = member.userUid == adminUid
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(68.dp)) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            if (!member.profilePictureUri.isNullOrEmpty()) {
                                AsyncImage(
                                    model = member.profilePictureUri,
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer).border(1.5.dp, if (isAdmin) Color(0xFFFFB300) else Color.Transparent, CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                // CORRETTO: rimosso Locale.getDefault() per azzerare gli errori di compilazione
                                val initial = member.username.take(1).uppercase()
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(colors = listOf(Color(0xFFECEFF1), Color(0xFFCFD8DC))))
                                        .border(1.5.dp, if (isAdmin) Color(0xFFFFB300) else Color.Transparent, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initial,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            if (isAdmin) {
                                Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(Color(0xFFFFB300)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.WorkspacePremium, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                            } else if (currentUserId == adminUid) {
                                Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(Color.Red).border(1.dp, MaterialTheme.colorScheme.surface, CircleShape).clickable { onRemoveMember(member) }, contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Remove, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = member.username, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
fun CleaningSection(
    rotations: List<CleaningRotationalState>,
    onAddServiceClick: () -> Unit,
    onDeleteService: (Int) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CleaningServices, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Turni Pulizie", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                IconButton(onClick = onAddServiceClick) { Icon(Icons.Default.Add, null) }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (rotations.isEmpty()) {
                Text("Nessun servizio attivo.", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rotations.forEach { rotation ->
                        Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(rotation.serviceName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Questa settimana: ${rotation.assigneeName}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { onDeleteService(rotation.serviceId) }) { Icon(Icons.Default.DeleteOutline, null, tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(20.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BalanceSection(
    balances: List<UserBalance>,
    onAddExpenseClick: () -> Unit,
    onSettleDebtClick: () -> Unit,
    onOpenDetailsClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onOpenDetailsClick() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Bilancio Casa", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                    Text("Tocca per vedere il registro completo", fontSize = 12.sp, color = Color.Gray)
                }
                Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (balances.isEmpty()) {
                Text("Nessun conto registrato.", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    balances.forEach { balance ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(balance.username, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            if (balance.netAmount >= 0) {
                                Text("+${String.format(java.util.Locale.US, "%.2f", balance.netAmount)}€", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            } else {
                                val positiveAmount = kotlin.math.abs(balance.netAmount)
                                Text("-${String.format(java.util.Locale.US, "%.2f", positiveAmount)}€", color = Color(0xFFC62828), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = onSettleDebtClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(40.dp)
                ) {
                    Text("Salda Conto", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onAddExpenseClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(40.dp)
                ) {
                    Text("Nuova Spesa", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}