package com.example.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.activity.ui.theme.MVVMDemoV2Theme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

//viewModel and the Model (The model is just a list of items, that's why I didn't create a separate class/file for it
class TodoViewModel : ViewModel()
{
    //Model
    private val tasks = MutableStateFlow(listOf<String>())
    val tasksReadOnly : StateFlow<List<String>> = tasks

    // Methods to modify the Model
    fun addTask (task: String){
        tasks.value += task
    }
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MVVMDemoV2Theme {
                val myVMObj: TodoViewModel = viewModel()
                TodoList (myVMObj)
            }
        }
    }
}

//View
@Composable
fun TodoList(myVM: TodoViewModel) {

    Column(Modifier.fillMaxWidth().statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {

         //Observe my tasks
        val observableTasks by myVM.tasksReadOnly.collectAsStateWithLifecycle()

        var itemText by remember { mutableStateOf("") }

        Row {
            OutlinedTextField(
                value = itemText,
                onValueChange = { itemText = it},
                label = { Text("Item") }
            )
        }
        Row {
            Button(onClick = {
                myVM.addTask(itemText)
                itemText=""

            }) {
                Text("Add Item")
            }

        }

        Spacer(Modifier.height(20.dp))
        Text("ToDo List", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = Color.Blue)
        Row{
            //display my list
           LazyColumn {
               items(observableTasks){ Text(it,
                   fontSize = 20.sp,
                   fontFamily = FontFamily.SansSerif,
                   fontWeight = FontWeight.Bold)}
               }

        }
    }
}