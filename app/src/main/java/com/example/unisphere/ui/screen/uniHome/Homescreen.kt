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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.input.KeyboardType
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
import com.example.unisphere.ui.composables.UniSphereAlertDialog
import com.example.unisphere.ui.composables.UniSphereAvatar
import com.example.unisphere.ui.composables.UniSphereButton
import com.example.unisphere.ui.composables.UniSphereListItem
import com.example.unisphere.ui.composables.UniSphereTextField

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

    val txToDeleteId = remember { mutableStateOf<Int?>(null) }
    val showDeleteHouseConfirm = remember { mutableStateOf(false) }
    val showLeaveHouseConfirm = remember { mutableStateOf(false) }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onAction(UniHomeAction.OnDismissSnackbar)
        }
    }

    Scaffold(
        topBar = { AppBar(title = if (state.hasHouse) state.houseName else "UniHome", navController = navController) },
        bottomBar = { BottomNavigationBar(navController) },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Card(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = if (state.isSuccessSnackbar) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(imageVector = if (state.isSuccessSnackbar) Icons.Default.CheckCircle else Icons.Default.Warning, contentDescription = null, tint = Color.White)
                        Text(text = data.visuals.message, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (!state.hasHouse) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues).background(Brush.verticalGradient(colors = listOf(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f), MaterialTheme.colorScheme.surface)))) {
                Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
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
                            UniSphereTextField(value = state.newHouseName, onValueChange = { viewModel.onAction(UniHomeAction.OnNewHouseNameChanged(it)) }, label = "Nome della casa", leadingIcon = Icons.Default.Home, modifier = Modifier.fillMaxWidth())
                            UniSphereButton(text = "Crea", onClick = { viewModel.onAction(UniHomeAction.OnCreateHouseClicked) }, modifier = Modifier.fillMaxWidth(), enabled = state.newHouseName.isNotBlank())
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
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = { showAllTxDetailsPage = false }) { Icon(Icons.Default.ArrowBack, "Indietro") }
                    Text("Registro Spese", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (state.transactionsWithSplits.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Nessuna spesa inserita finora.", color = Color.Gray) }
                } else {
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        state.transactionsWithSplits.forEach { item ->
                            val tx = item.transaction
                            val splitNames = item.splits.map { if(it.userUid == state.currentUserId) "Te" else it.username }.joinToString(", ")
                            UniSphereListItem(
                                headlineText = tx.title,
                                supportingText = "Pagato da: ${tx.payerUsername} • Diviso con: $splitNames",
                                leadingBarColor = MaterialTheme.colorScheme.primary,
                                trailingContent = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("${String.format(java.util.Locale.US, "%.2f", tx.amount)}€", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                                        IconButton(onClick = { txToDeleteId.value = tx.id }) { Icon(Icons.Default.DeleteOutline, null, tint = Color.Red.copy(alpha = 0.6f)) }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                RoommatesSection(members = state.members, adminUid = state.adminUid, currentUserId = state.currentUserId, onInviteClick = { showInviteDialog = true }, onRemoveMember = { viewModel.onAction(UniHomeAction.OnRemoveMember(it)) })

                CleaningSection(
                    rotations = state.cleaningRotations,
                    onAddServiceClick = { showServiceDialog = true },
                    onDeleteService = { viewModel.onAction(UniHomeAction.OnDeleteServiceClicked(it)) },
                    onToggleCompleted = { serviceId -> viewModel.onAction(UniHomeAction.OnToggleServiceCompleted(serviceId)) }
                )

                BalanceSection(balances = state.balances, onAddExpenseClick = { showExpenseDialog = true }, onSettleDebtClick = { showSettleDialog = true }, onOpenDetailsClick = { showAllTxDetailsPage = true })

                OutlinedButton(
                    onClick = {
                        if (state.currentUserId == state.adminUid) showDeleteHouseConfirm.value = true
                        else showLeaveHouseConfirm.value = true
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = if (state.currentUserId == state.adminUid) Color.Red else MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(if (state.currentUserId == state.adminUid) Icons.Default.DeleteForever else Icons.Default.ExitToApp, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.currentUserId == state.adminUid) "Elimina e Chiudi Casa" else "Abbandona Gruppo Casa", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    val targetTxId = txToDeleteId.value
    if (targetTxId != null) {
        UniSphereAlertDialog(
            title = "Elimina Spesa",
            text = "Sei sicuro di voler cancellare questa transazione? Il bilancio generale verrà ricalcolato.",
            confirmText = "Elimina",
            onConfirm = {
                viewModel.onAction(UniHomeAction.OnDeleteTransactionClicked(targetTxId))
                txToDeleteId.value = null
            },
            onDismiss = { txToDeleteId.value = null },
            dismissText = "Annulla"
        )
    }

    if (showDeleteHouseConfirm.value) {
        UniSphereAlertDialog(
            title = "Chiudi Casa",
            text = "Attenzione! Questa azione eliminerà definitivamento la UniHome e tutto il suo storico. Continuare?",
            confirmText = "Elimina",
            onConfirm = {
                viewModel.onAction(UniHomeAction.OnDeleteHouseClicked)
                showDeleteHouseConfirm.value = false
            },
            onDismiss = { showDeleteHouseConfirm.value = false },
            dismissText = "Annulla"
        )
    }

    if (showLeaveHouseConfirm.value) {
        UniSphereAlertDialog(
            title = "Abbandona Casa",
            text = "Sei sicuro di voler uscire da questo gruppo casa?",
            confirmText = "Abbandona",
            onConfirm = {
                viewModel.onAction(UniHomeAction.OnLeaveHouseClicked)
                showLeaveHouseConfirm.value = false
            },
            onDismiss = { showLeaveHouseConfirm.value = false },
            dismissText = "Annulla"
        )
    }

    if (showInviteDialog) {
        var dropdownExpanded by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showInviteDialog = false; viewModel.onAction(UniHomeAction.OnInviteQueryChanged("")) },
            title = { Text("Cerca Coinquilino", fontWeight = FontWeight.Bold) },
            text = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    UniSphereTextField(
                        value = state.inviteUserQuery,
                        onValueChange = { viewModel.onAction(UniHomeAction.OnInviteQueryChanged(it)); dropdownExpanded = it.length >= 2 },
                        label = "Digita lo username...",
                        leadingIcon = Icons.Default.Search,
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(expanded = dropdownExpanded && state.searchedUsers.isNotEmpty(), onDismissRequest = { dropdownExpanded = false }, modifier = Modifier.fillMaxWidth(), properties = PopupProperties(focusable = false)) {
                        state.searchedUsers.forEach { user -> DropdownMenuItem(text = { Text(user.username) }, onClick = { viewModel.onAction(UniHomeAction.OnSelectUserToInvite(user)); dropdownExpanded = false; showInviteDialog = false }) }
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
            text = { UniSphereTextField(value = state.newServiceName, onValueChange = { viewModel.onAction(UniHomeAction.OnNewServiceNameChanged(it)) }, label = "Nome Servizio (es. Cucina)", leadingIcon = Icons.Default.CleaningServices, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { UniSphereButton(text = "Aggiungi", onClick = { viewModel.onAction(UniHomeAction.OnAddCleaningServiceClicked); showServiceDialog = false }, enabled = state.newServiceName.isNotBlank()) },
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
                    UniSphereTextField(value = state.newExpenseTitle, onValueChange = { viewModel.onAction(UniHomeAction.OnNewExpenseTitleChanged(it)) }, label = "Descrizione spesa", leadingIcon = Icons.Default.Description, modifier = Modifier.fillMaxWidth())
                    UniSphereTextField(value = state.newExpenseAmount, onValueChange = { viewModel.onAction(UniHomeAction.OnNewExpenseAmountChanged(it)) }, label = "Importo Totale (€)", leadingIcon = Icons.Default.AttachMoney, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    Text("Dividi la quota con:", fontWeight = FontWeight.Bold)
                    state.members.forEach { member ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { if (selectedMembersForSplit.contains(member)) selectedMembersForSplit.remove(member) else selectedMembersForSplit.add(member) }) {
                            Checkbox(checked = selectedMembersForSplit.contains(member), onCheckedChange = { if (it == true) selectedMembersForSplit.add(member) else selectedMembersForSplit.remove(member) })
                            Text(member.username, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = { UniSphereButton(text = "Salva", onClick = { viewModel.onAction(UniHomeAction.OnAddExpenseClicked(selectedMembersForSplit.toList())); showExpenseDialog = false }, enabled = state.newExpenseTitle.isNotBlank() && state.newExpenseAmount.isNotBlank() && selectedMembersForSplit.isNotEmpty()) },
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
                        Row(modifier = Modifier.fillMaxWidth().background(if (selectedDebtor?.userUid == balance.userUid) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, RoundedCornerShape(8.dp)).clickable { selectedDebtor = balance }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedDebtor?.userUid == balance.userUid, onClick = null)
                            Text(balance.username, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    UniSphereTextField(value = settleAmount, onValueChange = { settleAmount = it }, label = "Somma da trasferire (€)", leadingIcon = Icons.Default.Payments, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = { UniSphereButton(text = "Conferma", onClick = { val amount = settleAmount.toDoubleOrNull() ?: 0.0; selectedDebtor?.let { viewModel.onAction(UniHomeAction.OnSettleDebtClicked(it.userUid, it.username, amount)) }; showSettleDialog = false }, enabled = selectedDebtor != null && settleAmount.isNotBlank()) },
            dismissButton = { TextButton(onClick = { showSettleDialog = false }) { Text("Annulla") } }
        )
    }
}

@Composable
fun RoommatesSection(members: List<HouseMemberEntity>, adminUid: String, currentUserId: String, onInviteClick: () -> Unit, onRemoveMember: (HouseMemberEntity) -> Unit) {
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
                        UniSphereAvatar(
                            username = member.username, profilePictureUri = member.profilePictureUri, size = 56.dp, showBorder = isAdmin, borderColor = Color(0xFFFFB300),
                            badge = {
                                if (isAdmin) {
                                    Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(Color(0xFFFFB300)), contentAlignment = Alignment.Center) { Icon(Icons.Default.WorkspacePremium, null, tint = Color.White, modifier = Modifier.size(12.dp)) }
                                } else if (currentUserId == adminUid) {
                                    Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(Color.Red).border(1.dp, MaterialTheme.colorScheme.surface, CircleShape).clickable { onRemoveMember(member) }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Remove, null, tint = Color.White, modifier = Modifier.size(12.dp)) }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // MODIFICATO: Mostra esplicitamente e tassativamente l'username del coinquilino
                        Text(text = member.username, style = MaterialTheme.typography.bodySmall, maxLines = 1, fontWeight = FontWeight.Medium)
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
    onDeleteService: (Int) -> Unit,
    onToggleCompleted: (Int) -> Unit
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
                Text("Nessun turno attivo.", color = Color.Gray, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 4.dp))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rotations.forEach { rotation ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // MODIFICATO: Checkbox premium customizzata molto più elegante e moderna
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (rotation.isCompleted) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .border(
                                        width = 1.5.dp,
                                        color = if (rotation.isCompleted) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { onToggleCompleted(rotation.serviceId) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (rotation.isCompleted) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Completato",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 14.dp)
                            ) {
                                Text(
                                    text = rotation.serviceName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Questa settimana: ${rotation.assigneeName}",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            IconButton(onClick = { onDeleteService(rotation.serviceId) }) {
                                Icon(Icons.Default.DeleteOutline, null, tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BalanceSection(balances: List<UserBalance>, onAddExpenseClick: () -> Unit, onSettleDebtClick: () -> Unit, onOpenDetailsClick: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth().clickable { onOpenDetailsClick() }, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(balance.username, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text(text = if (balance.netAmount >= 0) "+${String.format(java.util.Locale.US, "%.2f", balance.netAmount)}€" else "-${String.format(java.util.Locale.US, "%.2f", kotlin.math.abs(balance.netAmount))}€", color = if (balance.netAmount >= 0) Color(0xFF2E7D32) else Color(0xFFC62828), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = onSettleDebtClick, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f).height(40.dp)) { Text("Salda Conto", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                Button(onClick = onAddExpenseClick, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f).height(40.dp)) { Text("Nuova Spesa", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}