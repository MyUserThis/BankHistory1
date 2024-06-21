package com.example.bankhistory.ui.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.google.android.material.datepicker.MaterialDatePicker
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DatePickerDialog(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismissRequest: () -> Unit,
    expanded: Boolean
) {
    if (expanded) {
        val context = LocalContext.current
        if (context is FragmentActivity) {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setSelection(selectedDate.toEpochDay() * 24 * 60 * 60 * 1000)
                .build()
            picker.addOnPositiveButtonClickListener { selection ->
                val newDate = LocalDate.ofEpochDay(selection / (24 * 60 * 60 * 1000))
                onDateSelected(newDate)
            }
            picker.addOnDismissListener {
                onDismissRequest()
            }
            picker.show(context.supportFragmentManager, "datePicker")
        }
    }
}
