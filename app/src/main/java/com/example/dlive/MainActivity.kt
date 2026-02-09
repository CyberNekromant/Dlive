package com.example.dlive

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import java.util.Calendar


data class Pet(val id: String, val name: String, val photoUri: Uri? = null)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DLiveApp()
        }
    }
}

@Composable
fun DLiveApp() {
    val navController = rememberNavController()
    val petsState = remember { mutableStateListOf<Pet>() }
    var isDarkTheme by rememberSaveable { mutableStateOf(false) }

    MaterialTheme(colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()) {

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            NavHost(navController = navController, startDestination = "main") {
                composable("main") {
                    MainScreen(
                        pets = petsState,
                        onPetClick = { id -> navController.navigate("details/$id") },
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        isDarkTheme = isDarkTheme
                    )
                }
                composable(
                    "details/{petId}",
                    arguments = listOf(navArgument("petId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val petId = backStackEntry.arguments?.getString("petId")
                    val pet = petsState.find { it.id == petId }
                    PetDetailsScreen(pet = pet, onBack = { navController.popBackStack() })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    pets: MutableList<Pet>,
    onPetClick: (String) -> Unit,
    onToggleTheme: () -> Unit,
    isDarkTheme: Boolean
) {
    var newPetName by rememberSaveable { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { selectedImageUri = it }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DLive 🐾") },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally // Исправлено выравнивание
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .clickable { photoLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedImageUri != null) {
                            AsyncImage(selectedImageUri, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Icon(Icons.Default.AddAPhoto, null)
                        }
                    }
                    TextField(
                        value = newPetName,
                        onValueChange = { newPetName = it },
                        label = { Text("Имя питомца") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                    Button(
                        onClick = {
                            if (newPetName.isNotBlank()) {
                                pets.add(Pet(System.currentTimeMillis().toString(), newPetName, selectedImageUri))
                                newPetName = ""; selectedImageUri = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Text("Добавить")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            LazyColumn(Modifier.fillMaxSize()) {
                items(pets) { pet ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onPetClick(pet.id) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            if (pet.photoUri != null) {
                                AsyncImage(
                                    model = pet.photoUri,
                                    contentDescription = null,
                                    modifier = Modifier.size(50.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier.size(50.dp).clip(CircleShape).background(Color.Gray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Pets, null, tint = Color.White)
                                }
                            }

                            Text(
                                text = pet.name,
                                modifier = Modifier.padding(start = 12.dp).weight(1f),
                                style = MaterialTheme.typography.titleLarge
                            )

                            IconButton(onClick = { pets.remove(pet) }) {
                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetDetailsScreen(pet: Pet?, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pet?.name ?: "Детали") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        if (pet == null) return@Scaffold
        Column(Modifier.padding(padding).padding(16.dp)) {
            Text("Задачи по уходу", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))

            ReminderBlock("Лекарства", Icons.Default.Medication, hasDate = true)
            ReminderBlock("Когти", Icons.Default.ContentCut, hasPeriod = true)
            ReminderBlock("Уши", Icons.Default.Hearing, hasPeriod = true)
        }
    }
}

@Composable
fun ReminderBlock(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    hasDate: Boolean = false,
    hasPeriod: Boolean = false
) {
    var enabled by remember { mutableStateOf(false) }
    var dateText by remember { mutableStateOf("Выбрать дату") }
    val context = LocalContext.current

    val calendar = Calendar.getInstance()
    val picker = DatePickerDialog(context, { _, y, m, d ->
        dateText = "$d/${m+1}/$y"
    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                Text(title, Modifier.padding(start = 12.dp).weight(1f))
                Switch(checked = enabled, onCheckedChange = { enabled = it })
            }
            if (enabled) {
                if (hasDate) {
                    OutlinedButton(onClick = { picker.show() }, modifier = Modifier.padding(top = 8.dp)) {
                        Text(dateText)
                    }
                }
                if (hasPeriod) {
                    Row(Modifier.padding(top = 8.dp)) {
                        listOf("7 дн", "14 дн", "30 дн").forEach {
                            AssistChip(onClick = {}, label = { Text(it) }, modifier = Modifier.padding(end = 4.dp))
                        }
                    }
                }
            }
        }
    }
}