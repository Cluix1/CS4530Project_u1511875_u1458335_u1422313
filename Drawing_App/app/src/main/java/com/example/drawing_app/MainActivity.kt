package com.example.drawing_app

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.drawing_app.ui.theme.Drawing_AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.painterResource
import com.example.drawing_app.R


// Model
class Course(){
    var dept: String = "department";
    var number: String = "0000"
    var location: String = "loc"

}

//viewModel
class TodoViewModel : ViewModel()
{
    //Model
    private val tasks = MutableStateFlow(listOf<Course>())
    val tasksReadOnly : StateFlow<List<Course>> = tasks

    var visible: MutableStateFlow<Boolean> = MutableStateFlow(true)

    // Methods to modify the Model
    fun addCourse (task: Course){
        tasks.value += task
    }

    fun removeCourse(c: Course){
        tasks.value -= c;
    }

    fun editCourse(old: Course, dept: String, number: String, location: String){
        val updated = Course()
        updated.dept = dept
        updated.number = number
        updated.location = location

        val list = tasks.value.toMutableList()
        list[list.indexOf(old)] = updated
        tasks.value = list
    }

    suspend fun closeScreen(){
        delay(3000)
        visible.value = false
    }
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Drawing_AppTheme{
                val myVMObj: TodoViewModel = viewModel()
                SScreen(myVMObj)
                CourseList(myVMObj)
            }
        }
    }
}

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun SScreen(myVM: TodoViewModel){
    val scope = rememberCoroutineScope()
    AnimatedVisibility(myVM.visible.collectAsState().value, modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.crossed),
            contentDescription = "Splash Screen PNG"
        )
    }
    scope.launch {
        myVM.closeScreen()
    }
}

//View
@Composable
fun CourseList(myVM: TodoViewModel) {
    AnimatedVisibility(!myVM.visible.collectAsState().value) {
        Column(Modifier.fillMaxSize().statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {

            //Observe my tasks
            val observableTasks by myVM.tasksReadOnly.collectAsStateWithLifecycle()

            var depText by remember { mutableStateOf("") }
            var numberText by remember { mutableStateOf("") }
            var locText by remember { mutableStateOf("") }

            Row {
                OutlinedTextField(
                    value = depText,
                    onValueChange = { depText = it},
                    label = { Text("Department") }
                )
            }
            Row {
                OutlinedTextField(
                    value = numberText,
                    onValueChange = { numberText = it},
                    label = { Text("Class Number:") }
                )
            }
            Row {
                OutlinedTextField(
                    value = locText,
                    onValueChange = { locText = it},
                    label = { Text("Location:") }
                )
            }
            Row {
                Button(onClick = {
                    val newCourse = Course();
                    newCourse.dept = depText;
                    newCourse.number = numberText;
                    newCourse.location = locText;
                    myVM.addCourse(newCourse)
                    depText=""
                    locText=""
                    numberText=""

                }) {
                    Text("Add Course")
                }

            }

            Spacer(Modifier.height(20.dp))
            Text("Classes list:", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = Color.Blue)
            Row(Modifier.weight(1f).fillMaxWidth()) {
                //display my list
                LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)) {
                    items(observableTasks){
                        var showDetails by remember { mutableStateOf(false) }

                        Row(Modifier.fillMaxWidth()
                            .clickable { showDetails = !showDetails }
                            .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {

                            Text(it.dept + it.number, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) //printing concatenated title
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { myVM.editCourse(it, depText, numberText, locText) }) {Text("Edit", fontSize = 16.sp)}
                                Button(onClick = { myVM.removeCourse(it) }) {Text("Delete", fontSize = 16.sp)} //current object is 'it. ' like 'this. '
                            }

                        }
                        if (showDetails) {
                            Text("Department: " + it.dept)
                            Text("Number: " + it.number)
                            Text("Location: " + it.location)
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}