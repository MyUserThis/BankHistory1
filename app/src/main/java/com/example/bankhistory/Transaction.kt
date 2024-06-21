package com.example.bankhistory.model

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.bankhistory.database.DatabaseHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class Transaction(
    val id: Int,
    val accName: String,
    val amount: Double,
    val transactionTypeID: Int,
    val direction: String,
    val requisites: String,
    val date: String,
    val time: String,
    val description: String,
    val typeName: String
)

@RequiresApi(Build.VERSION_CODES.O)
suspend fun getFilteredTransactions(
    dbHandler: DatabaseHandler,
    filter: String,
    startDate: LocalDate,
    endDate: LocalDate
): List<Transaction> {
    return withContext(Dispatchers.IO) {
        val db = dbHandler.readableDatabase
        val filterCondition = when (filter) {
            "Расходы" -> "AND t.Direction = 'Расход'"
            "Приходы" -> "AND t.Direction = 'Приход'"
            else -> ""
        }
        val cursor = db.rawQuery("""
            SELECT t.*, ot.TypeName 
            FROM Transactions t 
            JOIN OperationTypes ot ON t.TransactionTypeID = ot.ID
            WHERE t.Date BETWEEN '${startDate.format(DateTimeFormatter.ISO_DATE)}' AND '${endDate.format(DateTimeFormatter.ISO_DATE)}'
            $filterCondition
            ORDER BY t.Date DESC, t.Time DESC
        """, null)
        val transactions = mutableListOf<Transaction>()
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("ID"))
                val accName = cursor.getString(cursor.getColumnIndexOrThrow("AccName"))
                val amount = cursor.getDouble(cursor.getColumnIndexOrThrow("Amount"))
                val transactionTypeID = cursor.getInt(cursor.getColumnIndexOrThrow("TransactionTypeID"))
                val direction = cursor.getString(cursor.getColumnIndexOrThrow("Direction"))
                val requisites = cursor.getString(cursor.getColumnIndexOrThrow("Requisites"))
                val date = cursor.getString(cursor.getColumnIndexOrThrow("Date"))
                val time = cursor.getString(cursor.getColumnIndexOrThrow("Time"))
                val description = cursor.getString(cursor.getColumnIndexOrThrow("Description"))
                val typeName = cursor.getString(cursor.getColumnIndexOrThrow("TypeName"))

                transactions.add(
                    Transaction(
                        id, accName, amount, transactionTypeID, direction,
                        requisites, date, time, description, typeName
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return@withContext transactions
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun getTransactionGroup(date: String): String {
    val transactionDate = LocalDate.parse(date, DateTimeFormatter.ISO_DATE)
    val today = LocalDate.now()
    return when {
        transactionDate.isEqual(today) -> "Сегодня"
        transactionDate.isEqual(today.minus(1, ChronoUnit.DAYS)) -> "Вчера"
        else -> transactionDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", java.util.Locale.getDefault()))
    }
}
