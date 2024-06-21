package com.example.bankhistory

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.height
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BottomNavigationBar(context: Context, selectedScreen: String) {
    BottomNavigation(
        modifier = Modifier.height(56.dp),
        backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
    ) {
        BottomNavigationItem(
            selected = selectedScreen == "Home",
            onClick = {
                if (selectedScreen != "Home") {
                    val intent = Intent(context, MainActivity::class.java)
                    context.startActivity(intent)
                }
            },
            label = { Text("Главная") },
            icon = { Icon(Icons.Default.Home, contentDescription = "Главная") }
        )
        BottomNavigationItem(
            selected = selectedScreen == "History",
            onClick = {
                if (selectedScreen != "History") {
                    val intent = Intent(context, HistoryActivity::class.java)
                    context.startActivity(intent)
                }
            },
            label = { Text("История") },
            icon = { Icon(Icons.Default.List, contentDescription = "История") }
        )
    }
}
