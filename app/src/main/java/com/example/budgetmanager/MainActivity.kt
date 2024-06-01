package com.example.budgetmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import java.io.File
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExpenseTrackerApp()
        }
    }

}
@Composable
fun ExpenseTrackerApp() {
    val context = LocalContext.current
    val expenses = remember { mutableStateListOf<Expense>() }
    val currentExpense = remember { mutableStateOf("") }
    val currentCategory = remember { mutableStateOf("") }
    val selectedPeriod = remember { mutableStateOf(Period.DAILY) }
    val editingExpense = remember { mutableStateOf<Expense?>(null) }
    val showError = remember { mutableStateOf(false) }

    // Загрузка данных из локального хранилища
    LaunchedEffect(Unit) {
        loadExpenses(context, expenses)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Выбор периода
        PeriodSelector(selectedPeriod)

        // Добавление расхода
        AddExpense(
            currentExpense,
            currentCategory,
            showError,
            onAddExpense = { amount, category ->
                if (isValidAmount(amount)) {
                    expenses.add(Expense(amount.toFloat(), category, Date()))
                    currentExpense.value = ""
                    currentCategory.value = ""
                    saveExpenses(context, expenses)
                    showError.value = false // Сбросить ошибку
                } else {
                    showError.value = true // Показать ошибку
                }
            }
        )

        // Отображение расходов
        ExpenseList(
            expenses,
            selectedPeriod.value,
            onExpenseEdit = { expense ->
                editingExpense.value = expense
                //currentExpense.value = expense.amount.toString()
                //currentCategory.value = expense.name
            },
            onExpenseDelete = { expense ->
                expenses.remove(expense)
                saveExpenses(context, expenses)
            },
        )

        // Редактирование расхода
        if (editingExpense.value != null) {
            EditExpense(
                editingExpense.value!!,
                onEditExpense = { editedExpense ->
                    val index = expenses.indexOf(editingExpense.value)
                    if (index != -1) {
                        expenses[index] = editedExpense
                        saveExpenses(context, expenses)
                    }
                    editingExpense.value = null
                    showError.value = false // Сбросить ошибку
                },
                onCancelEdit = { editingExpense.value = null }
            )
        }

        // Итог расходов
        ExpenseSummary(
            expenses,
            selectedPeriod.value
        )

        // Отображение ошибки
        if (showError.value) {
            Text(
                text = "Неверный формат суммы. Например 152.56",
                color = Color.Red
            )
        }
    }
}
@Composable
fun EditExpense(
    expense: Expense,
    onEditExpense: (Expense) -> Unit,
    onCancelEdit: () -> Unit
) {
    val currentExpense = remember { mutableStateOf(expense.amount.toString()) } // Состояние для редактирования
    val currentCategory = remember { mutableStateOf(expense.name) } // Состояние для редактирования
    val showError = remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = currentExpense.value,
            onValueChange = { currentExpense.value = it },
            label = { Text("Сумма") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = currentCategory.value,
            onValueChange = { currentCategory.value = it },
            label = { Text("Название товара") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Button(
                onClick = {
                    val amount = currentExpense.value.toFloatOrNull()
                    if (amount != null) {
                        onEditExpense(Expense(amount, currentCategory.value, expense.date))
                        showError.value = false // Сбросить ошибку
                    } else {
                        showError.value = true // Показать ошибку
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Сохранить")
            }
            Button(
                onClick = onCancelEdit,
                modifier = Modifier.weight(1f)
            ) {
                Text("Отменить")
            }
        }

        // Отображение ошибки
        if (showError.value) {
            Text(
                text = "Неверный формат суммы. Например 152.56",
                color = Color.Red
            )
        }
    }
}
@Composable
fun PeriodSelector(selectedPeriod: MutableState<Period>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        RadioButton(
            selected = selectedPeriod.value == Period.DAILY,
            onClick = { selectedPeriod.value = Period.DAILY }
        )
        Text("День")
        RadioButton(
            selected = selectedPeriod.value == Period.WEEKLY,
            onClick = { selectedPeriod.value = Period.WEEKLY }
        )
        Text("Неделя")
        RadioButton(
            selected = selectedPeriod.value == Period.MONTHLY,
            onClick = { selectedPeriod.value = Period.MONTHLY }
        )
        Text("Месяц")
    }
}

@Composable
fun AddExpense(
    currentExpense: MutableState<String>,
    currentCategory: MutableState<String>,
    showError: MutableState<Boolean>,
    onAddExpense: (amount: String, category: String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = currentExpense.value,
            onValueChange = { currentExpense.value = it },
            label = { Text("Сумма") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = currentCategory.value,
            onValueChange = { currentCategory.value = it },
            label = { Text("Название товара") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                onAddExpense(currentExpense.value, currentCategory.value)
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Добавить товар")
        }
        if (showError.value) {
            Text(
                text = "Неверный формат суммы. Например 152.56",
                color = Color.Red
            )
        }
    }
}

@Composable
fun ExpenseList(
    expenses: List<Expense>,
    selectedPeriod: Period,
    onExpenseEdit: (Expense) -> Unit,
    onExpenseDelete: (Expense) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        items(getExpensesForPeriod(expenses, selectedPeriod)) { expense ->
            ExpenseItem(expense, onExpenseEdit, onExpenseDelete)
        }
    }
}

@Composable
fun ExpenseItem(
    expense: Expense,
    onExpenseEdit: (Expense) -> Unit,
    onExpenseDelete: (Expense) -> Unit,
) {
    var isEditing by remember { mutableStateOf(false) }
    if (isEditing) {
        // Редактирование
        EditExpense(
            expense,
            onEditExpense = { editedExpense ->
                onExpenseEdit(editedExpense) // Передаем отредактированный расход в onExpenseEdit
                isEditing = false
            },
            onCancelEdit = {
                isEditing = false
            }
        )
    } else {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = String.format("%.2f ₽", expense.amount),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = expense.name,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = { onExpenseEdit(expense) }) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Filled.Edit,
                contentDescription = "Редактировать"
            )
        }
        IconButton(onClick = { onExpenseDelete(expense) }) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Filled.Delete,
                contentDescription = "Удалить"
                )
            }
        }
    }
}

fun loadExpenses(context: android.content.Context, expenses: MutableList<Expense>) {
    val file = File(context.filesDir, "expenses.json")
    if (file.exists()) {
        val gson = Gson()
        val json = file.readText()
        val loadedExpenses = gson.fromJson(json, Array<Expense>::class.java).toList()
        expenses.addAll(loadedExpenses)
    }
}

fun saveExpenses(context: android.content.Context, expenses: List<Expense>) {
    val file = File(context.filesDir, "expenses.json")
    val gson = Gson()
    val json = gson.toJson(expenses)
    file.writeText(json)
}

data class Expense(
    val amount: Float,
    val name: String,
    val date: Date
)

enum class Period {
    DAILY,
    WEEKLY,
    MONTHLY
}

fun getExpensesForPeriod(expenses: List<Expense>, period: Period): List<Expense> {
    val calendar = Calendar.getInstance()
    val now = calendar.time
    when (period) {
        Period.DAILY -> {
            val startOfDay = calendar.clone() as Calendar
            startOfDay.set(Calendar.HOUR_OF_DAY, 0)
            startOfDay.set(Calendar.MINUTE, 0)
            startOfDay.set(Calendar.SECOND, 0)
            startOfDay.set(Calendar.MILLISECOND, 0)
            return expenses.filter { it.date >= startOfDay.time && it.date <= now }
        }
        Period.WEEKLY -> {
            val startOfWeek = calendar.clone() as Calendar
            startOfWeek.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            startOfWeek.set(Calendar.HOUR_OF_DAY, 0)
            startOfWeek.set(Calendar.MINUTE, 0)
            startOfWeek.set(Calendar.SECOND, 0)
            startOfWeek.set(Calendar.MILLISECOND, 0)
            return expenses.filter { it.date >= startOfWeek.time && it.date <= now }
        }
        Period.MONTHLY -> {
            val startOfMonth = calendar.clone() as Calendar
            startOfMonth.set(Calendar.DAY_OF_MONTH, 1)
            startOfMonth.set(Calendar.HOUR_OF_DAY, 0)
            startOfMonth.set(Calendar.MINUTE, 0)
            startOfMonth.set(Calendar.SECOND, 0)
            startOfMonth.set(Calendar.MILLISECOND, 0)
            return expenses.filter { it.date >= startOfMonth.time && it.date <= now }
        }
    }
}

fun getPeriodName(period: Period): String {
    return when (period) {
        Period.DAILY -> "день"
        Period.WEEKLY -> "неделю"
        Period.MONTHLY -> "месяц"
    }
}

@Composable
fun ExpenseSummary(
    expenses: List<Expense>,
    selectedPeriod: Period
) {
    val totalExpense = getExpensesForPeriod(expenses, selectedPeriod).sumOf { it.amount.toDouble() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Итого за ${getPeriodName(selectedPeriod)}:",
                style = MaterialTheme.typography.h6
            )
            Text(
                text = String.format("%.2f ₽", totalExpense),
                style = MaterialTheme.typography.h5,
                color = Color.Red
            )
        }
    }
}

fun isValidAmount(amount: String): Boolean {
    return try {
        amount.toFloat()
        true
    } catch (e: NumberFormatException) {
        false
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ExpenseTrackerApp()
}

