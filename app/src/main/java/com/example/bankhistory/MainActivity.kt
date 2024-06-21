@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.bankhistory

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bankhistory.database.DatabaseHandler
import com.example.bankhistory.ui.theme.BankHistoryTheme
import com.example.bankhistory.ui.components.DatePickerDialog
import com.example.bankhistory.ui.components.DropdownMenuItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BankHistoryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val dbHandler = remember { DatabaseHandler(context) }
    var balance by remember { mutableStateOf(0.0) }
    var showAddOperationDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        balance = getBalance(dbHandler)
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(context, selectedScreen = "Home")
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Банк",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = "Баланс",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "$balance ₽",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            Button(
                onClick = { showAddOperationDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(text = "Добавить операцию")
            }
        }

        if (showAddOperationDialog) {
            AddOperationDialog(
                dbHandler = dbHandler,
                onDismissRequest = { showAddOperationDialog = false },
                onOperationAdded = {
                    showAddOperationDialog = false
                    coroutineScope.launch {
                        balance = getBalance(dbHandler)
                    }
                }
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AddOperationDialog(
    dbHandler: DatabaseHandler,
    onDismissRequest: () -> Unit,
    onOperationAdded: () -> Unit
) {
    val operationTypes = listOf(
        "Пополнение с банкомата", "Снятие с банкомата", "Перевод по номеру карты",
        "Перевод по номеру телефона", "Оплата услуг", "Оплата товаров", "Вложение"
    )
    var selectedOperationType by remember { mutableStateOf(operationTypes[0]) }
    var accName by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf("Приход") }
    var requisites by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var time by remember { mutableStateOf(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDropdownMenu by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun validateDateTime(): Boolean {
        val selectedDateTime = LocalDate.parse(date.toString()).atTime(LocalTime.parse(time, timeFormatter))
        return selectedDateTime.isBefore(LocalDate.now().atTime(LocalTime.now()))
    }

    fun validateFields(): Boolean {
        return accName.isNotEmpty() && amount.isNotEmpty() && requisites.isNotEmpty() && description.isNotEmpty() && validateDateTime()
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = "Добавить операцию")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = accName,
                    onValueChange = { accName = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Сумма") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Тип операции:")
                    Box {
                        Text(
                            text = selectedOperationType,
                            modifier = Modifier
                                .clickable { showDropdownMenu = true }
                                .padding(8.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        )
                        DropdownMenu(
                            expanded = showDropdownMenu,
                            onDismissRequest = { showDropdownMenu = false }
                        ) {
                            operationTypes.forEach { type ->
                                DropdownMenuItem(
                                    onClick = {
                                        selectedOperationType = type
                                        showDropdownMenu = false
                                    },
                                    category = type
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = direction == "Приход",
                            onClick = { direction = "Приход" }
                        )
                        Text(text = "Приход")
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(
                            selected = direction == "Расход",
                            onClick = { direction = "Расход" }
                        )
                        Text(text = "Расход")
                    }
                }
                OutlinedTextField(
                    value = requisites,
                    onValueChange = { requisites = it },
                    label = { Text("Реквизиты") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Дата")
                        Box(
                            modifier = Modifier.clickable { showDatePicker = true }
                        ) {
                            Text(
                                text = date.format(dateFormatter),
                                modifier = Modifier
                                    .padding(8.dp)
                            )
                        }
                        DatePickerDialog(
                            selectedDate = date,
                            onDateSelected = { date = it },
                            onDismissRequest = { showDatePicker = false },
                            expanded = showDatePicker
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Время")
                        OutlinedTextField(
                            value = time,
                            onValueChange = { time = it },
                            label = { Text("Время (HH:mm)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                errorMessage?.let {
                    Text(
                        text = it,
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (validateFields()) {
                    coroutineScope.launch {
                        addTransaction(
                            dbHandler,
                            accName,
                            amount.toDoubleOrNull() ?: 0.0,
                            selectedOperationType,
                            direction,
                            requisites,
                            date,
                            LocalTime.parse(time, timeFormatter),
                            description
                        )
                        onOperationAdded()
                    }
                }
            }) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Отмена")
            }
        }
    )
}

@RequiresApi(Build.VERSION_CODES.O)
suspend fun addTransaction(
    dbHandler: DatabaseHandler,
    accName: String,
    amount: Double,
    typeName: String,
    direction: String,
    requisites: String,
    date: LocalDate,
    time: LocalTime,
    description: String
) {
    withContext(Dispatchers.IO) {
        val db = dbHandler.writableDatabase
        val cursor = db.rawQuery("SELECT ID FROM OperationTypes WHERE TypeName = ?", arrayOf(typeName))
        var transactionTypeID = 1
        if (cursor.moveToFirst()) {
            transactionTypeID = cursor.getInt(cursor.getColumnIndexOrThrow("ID"))
        }
        cursor.close()
        db.execSQL(
            """
            INSERT INTO Transactions (AccName, Amount, TransactionTypeID, Direction, Requisites, Date, Time, Description)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """, arrayOf(
                accName, amount, transactionTypeID, direction, requisites,
                date.format(DateTimeFormatter.ISO_DATE),
                time.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                description
            )
        )
        db.close()
    }
}

suspend fun getBalance(dbHandler: DatabaseHandler): Double {
    return withContext(Dispatchers.IO) {
        val db = dbHandler.readableDatabase
        val cursorIn = db.rawQuery("SELECT SUM(Amount) as TotalIn FROM Transactions WHERE Direction = 'Приход'", null)
        var totalIn = 0.0
        if (cursorIn.moveToFirst()) {
            totalIn = cursorIn.getDouble(cursorIn.getColumnIndexOrThrow("TotalIn"))
        }
        cursorIn.close()
        val cursorOut = db.rawQuery("SELECT SUM(Amount) as TotalOut FROM Transactions WHERE Direction = 'Расход'", null)
        var totalOut = 0.0
        if (cursorOut.moveToFirst()) {
            totalOut = cursorOut.getDouble(cursorOut.getColumnIndexOrThrow("TotalOut"))
        }
        cursorOut.close()
        db.close()
        return@withContext totalIn - totalOut
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    BankHistoryTheme {
        MainScreen()
    }
}
