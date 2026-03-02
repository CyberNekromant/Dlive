package com.example.dlive

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import coil.compose.AsyncImage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

// --- МОДЕЛИ И МЕНЕДЖЕР (БЕЗ ИЗМЕНЕНИЙ) ---
data class SimpleNote(val id: String, val text: String, val label: String, val isAlert: Boolean)
data class SimplePet(val id: String, val name: String, val photo: String?, val notes: MutableList<SimpleNote> = mutableListOf())

class DataManager(context: Context) {
    private val prefs = context.getSharedPreferences("dlive_final_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    fun save(pets: List<SimplePet>) { prefs.edit().putString("data", gson.toJson(pets)).apply() }
    fun load(): List<SimplePet> {
        val json = prefs.getString("data", null) ?: return emptyList()
        return gson.fromJson(json, object : TypeToken<List<SimplePet>>() {}.type)
    }
}

// --- ВОРКЕР (БЕЗ ИЗМЕНЕНИЙ) ---
class NotificationWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val note = inputData.getString("note") ?: "Напоминание"
        val petName = inputData.getString("pet") ?: "Питомец"
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val chId = "dlive_reminders"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(chId, "DLive", NotificationManager.IMPORTANCE_HIGH))
        }
        val notification = NotificationCompat.Builder(applicationContext, chId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(petName).setContentText(note).setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).build()
        manager.notify(System.currentTimeMillis().toInt(), notification)
        return Result.success()
    }
}

// --- ACTIVITY С ЗАПРОСОМ РАЗРЕШЕНИЙ ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val manager = DataManager(this)

        setContent {
            val navController = rememberNavController()
            val pets = remember { mutableStateListOf<SimplePet>().apply { addAll(manager.load()) } }

            // --- ЛОГИКА ЗАПРОСА ПРАВ ---
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (!isGranted) {
                    Toast.makeText(this, "Уведомления отключены. Вы не получите напоминаний!", Toast.LENGTH_LONG).show()
                }
            }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            MaterialTheme {
                Surface(color = Color.White, modifier = Modifier.fillMaxSize()) {
                    NavHost(navController, startDestination = "main") {
                        composable("main") { MainScreen(pets, { manager.save(pets) }, navController) }
                        composable("details/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { b ->
                            val id = b.arguments?.getString("id")
                            DetailsScreen(pets.find { it.id == id }, { manager.save(pets) }) { navController.popBackStack() }
                        }
                    }
                }
            }
        }
    }
}

// --- ЭКРАНЫ (ОБНОВЛЕНА ЛОГИКА КНОПКИ НАПОМНИТЬ) ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(pets: MutableList<SimplePet>, onSave: () -> Unit, nav: androidx.navigation.NavController) {
    var name by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { photoUri = it }

    Column(Modifier.padding(16.dp).fillMaxSize()) {
        Text("DLive 🐾", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Card(Modifier.padding(vertical = 12.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(64.dp).clip(CircleShape).background(Color.LightGray).clickable { picker.launch("image/*") }) {
                    if (photoUri != null) AsyncImage(photoUri, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else Icon(Icons.Default.AddAPhoto, null, Modifier.align(Alignment.Center), tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                TextField(name, { name = it }, placeholder = { Text("Имя питомца") }, modifier = Modifier.weight(1f))
            }
            Button({
                if (name.isNotBlank()) {
                    pets.add(SimplePet(UUID.randomUUID().toString(), name.trim(), photoUri?.toString()))
                    onSave(); name = ""; photoUri = null
                }
            }, Modifier.fillMaxWidth().padding(8.dp)) { Text("ДОБАВИТЬ") }
        }
        LazyColumn {
            items(pets) { pet ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp).background(Color(0xFFEEEEEE), RoundedCornerShape(12.dp)).clickable { nav.navigate("details/${pet.id}") }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (pet.photo != null) AsyncImage(Uri.parse(pet.photo), null, Modifier.size(50.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                    else Icon(Icons.Default.Pets, null, Modifier.size(50.dp), tint = Color.Gray)
                    Text(pet.name, Modifier.padding(start = 12.dp).weight(1f), color = Color.Black, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton({ pets.remove(pet); onSave() }) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(pet: SimplePet?, onSave: () -> Unit, onBack: () -> Unit) {
    if (pet == null) return
    val ctx = LocalContext.current
    var noteMsg by remember { mutableStateOf("") }
    var timeVal by remember { mutableStateOf("1") }
    val units = listOf("Мин", "Час", "Дни")
    var selectedUnit by remember { mutableStateOf(units[0]) }
    var refresh by remember { mutableStateOf(0) }

    Column(Modifier.padding(16.dp).fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.Black) }
            Text(pet.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }
        OutlinedTextField(noteMsg, { noteMsg = it }, label = { Text("О чем напомнить?") }, modifier = Modifier.fillMaxWidth(), textStyle = TextStyle(color = Color.Black))
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextField(timeVal, { if(it.all { c -> c.isDigit() }) timeVal = it }, Modifier.width(70.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Spacer(Modifier.width(8.dp))
            units.forEach { u -> FilterChip(selected = selectedUnit == u, onClick = { selectedUnit = u }, label = { Text(u) }, modifier = Modifier.padding(end = 4.dp)) }
        }
        Row(Modifier.padding(top = 16.dp)) {
            Button({
                if (noteMsg.isNotBlank()) {
                    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                    pet.notes.add(0, SimpleNote(UUID.randomUUID().toString(), noteMsg.trim(), "Запись: $time", false))
                    onSave(); noteMsg = ""; refresh++
                }
            }, Modifier.weight(1f).padding(end = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)) { Text("ЗАМЕТКА", color = Color.Black) }

            Button({
                if (noteMsg.isNotBlank()) {
                    // ПРОВЕРКА РАЗРЕШЕНИЯ ПЕРЕД ОТПРАВКОЙ
                    val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                    } else true

                    if (hasPermission) {
                        val delay = timeVal.toLongOrNull() ?: 1L
                        val ms = when(selectedUnit) { "Мин" -> delay; "Час" -> delay * 60; else -> delay * 1440 }
                        pet.notes.add(0, SimpleNote(UUID.randomUUID().toString(), noteMsg.trim(), "Будильник через $delay $selectedUnit", true))
                        val data = workDataOf("note" to noteMsg.trim(), "pet" to pet.name)
                        val req = OneTimeWorkRequestBuilder<NotificationWorker>().setInitialDelay(ms, TimeUnit.MINUTES).setInputData(data).build()
                        WorkManager.getInstance(ctx).enqueue(req)
                        onSave(); noteMsg = ""; refresh++; Toast.makeText(ctx, "Напоминание создано!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(ctx, "Разрешите уведомления в настройках телефона!", Toast.LENGTH_LONG).show()
                    }
                }
            }, Modifier.weight(1f)) { Text("НАПОМНИТЬ") }
        }
        key(refresh) {
            LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
                items(pet.notes) { note ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).background(Color(0xFFF9F9F9), RoundedCornerShape(8.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)).padding(12.dp)) {
                        Icon(if(note.isAlert) Icons.Default.NotificationsActive else Icons.Default.ChatBubbleOutline, null, tint = if(note.isAlert) Color.Blue else Color.Gray)
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(note.text, color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Text(note.label, color = Color.Gray, fontSize = 12.sp)
                        }
                        IconButton({ pet.notes.remove(note); onSave(); refresh++ }) { Icon(Icons.Default.Close, null, Modifier.size(18.dp), tint = Color.LightGray) }
                    }
                }
            }
        }
    }
}