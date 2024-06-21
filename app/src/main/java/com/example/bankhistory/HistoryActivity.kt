package com.example.bankhistory

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import com.example.bankhistory.database.DatabaseHandler
import com.example.bankhistory.model.*
import com.example.bankhistory.ui.theme.BankHistoryTheme
import com.example.bankhistory.ui.components.DatePickerDialog
import com.example.bankhistory.ui.components.DropdownMenuItem
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class HistoryActivity : FragmentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BankHistoryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HistoryScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterialScaffoldPaddingParameter", "UnusedMaterial3ScaffoldPaddingParameter")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val dbHandler = remember { DatabaseHandler(context) }
    var transactions by remember { mutableStateOf(listOf<Transaction>()) }
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }

    var selectedFilter by remember { mutableStateOf("Все операции") }
    val filterOptions = listOf("Все операции", "Расходы", "Приходы")

    val startDate = remember { MutableLiveData(LocalDate.now().minusMonths(2)) }
    val endDate = remember { MutableLiveData(LocalDate.now()) }

    val startDateValue by startDate.observeAsState()
    val endDateValue by endDate.observeAsState()

    LaunchedEffect(selectedFilter, startDateValue, endDateValue) {
        transactions = getFilteredTransactions(dbHandler, selectedFilter, startDateValue!!, endDateValue!!)
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(context, selectedScreen = "History")
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            FilterSection(
                selectedFilter = selectedFilter,
                filterOptions = filterOptions,
                onFilterChange = { selectedFilter = it },
                startDate = startDateValue!!,
                endDate = endDateValue!!,
                onStartDateChange = { startDate.value = it },
                onEndDateChange = { endDate.value = it }
            )
            LazyColumn {
                val groupedTransactions = transactions
                    .sortedWith(
                        compareByDescending<Transaction> { LocalDate.parse(it.date, DateTimeFormatter.ISO_DATE) }
                            .thenByDescending { LocalTime.parse(it.time, DateTimeFormatter.ISO_TIME) }
                    )
                    .groupBy { transaction ->
                        getTransactionGroup(transaction.date)
                    }
                groupedTransactions.forEach { (group, transactionsForDate) ->
                    item {
                        Text(
                            text = group,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(transactionsForDate) { transaction ->
                        TransactionItem(transaction) {
                            selectedTransaction = transaction
                        }
                    }
                }
            }
        }
    }

    selectedTransaction?.let { transaction ->
        TransactionDetailDialog(transaction) {
            selectedTransaction = null
        }
    }
}

@Composable
fun TransactionDetailDialog(transaction: Transaction, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Детали транзакции")
        },
        text = {
            Column {
                DetailRow(label = "Название", value = transaction.accName)
                DetailRow(label = "Тип операции", value = transaction.typeName)
                DetailRow(label = "Сумма", value = transaction.amount.toString())
                DetailRow(label = "Направление", value = transaction.direction)
                DetailRow(label = "Реквизиты", value = transaction.requisites)
                DetailRow(label = "Дата", value = transaction.date)
                DetailRow(label = "Время", value = transaction.time)
                DetailRow(label = "Описание", value = transaction.description)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FilterSection(
    selectedFilter: String,
    filterOptions: List<String>,
    onFilterChange: (String) -> Unit,
    startDate: LocalDate,
    endDate: LocalDate,
    onStartDateChange: (LocalDate) -> Unit,
    onEndDateChange: (LocalDate) -> Unit
) {
    var filterExpanded by remember { mutableStateOf(false) }
    var startExpanded by remember { mutableStateOf(false) }
    var endExpanded by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Фильтр:")
                Box {
                    Text(
                        text = selectedFilter,
                        color = Color.Black,
                        modifier = Modifier
                            .clickable { filterExpanded = true }
                            .padding(8.dp)
                            .background(Color.White)
                    )
                    DropdownMenu(
                        expanded = filterExpanded,
                        onDismissRequest = { filterExpanded = false }
                    ) {
                        filterOptions.forEach { option ->
                            DropdownMenuItem(onClick = {
                                onFilterChange(option)
                                filterExpanded = false
                            }, category = option)
                        }
                    }
                }
            }

            Column {
                Text("Период:")
                Row {
                    Box {
                        Text(
                            text = startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                            color = Color.Black,
                            modifier = Modifier
                                .clickable { startExpanded = true }
                                .padding(8.dp)
                                .background(Color.White)
                        )
                        DatePickerDialog(
                            selectedDate = startDate,
                            onDateSelected = { onStartDateChange(it) },
                            onDismissRequest = { startExpanded = false },
                            expanded = startExpanded
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box {
                        Text(
                            text = endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                            color = Color.Black,
                            modifier = Modifier
                                .clickable { endExpanded = true }
                                .padding(8.dp)
                                .background(Color.White)
                        )
                        DatePickerDialog(
                            selectedDate = endDate,
                            onDateSelected = { onEndDateChange(it) },
                            onDismissRequest = { endExpanded = false },
                            expanded = endExpanded
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction, onClick: () -> Unit) {
    val color = if (transaction.direction == "Приход") Color.Green else Color.Red
    val sign = if (transaction.direction == "Приход") "+" else "-"
    val icon = when (transaction.typeName) {
        "Пополнение с банкомата" -> androidx.compose.material.icons.Icons.Default.Place
        "Снятие с банкомата" -> androidx.compose.material.icons.Icons.Default.ExitToApp
        "Перевод на другую карту по номеру карты" -> androidx.compose.material.icons.Icons.Default.Menu
        "Перевод на другую карту по номеру телефона" -> androidx.compose.material.icons.Icons.Default.Call
        "Оплата услуг" -> androidx.compose.material.icons.Icons.Default.MailOutline
        "Оплата товаров" -> androidx.compose.material.icons.Icons.Default.ShoppingCart
        "Вложение" -> androidx.compose.material.icons.Icons.Default.DateRange
        else -> androidx.compose.material.icons.Icons.Default.PlayArrow
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = transaction.typeName,
                modifier = Modifier.padding(end = 8.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.accName,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = transaction.typeName,
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                )
            }
            Text(
                text = "$sign${transaction.amount}",
                color = color,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun HistoryScreenPreview() {
    BankHistoryTheme {
        HistoryScreen()
    }
}
