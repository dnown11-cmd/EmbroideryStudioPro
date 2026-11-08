package com.embroidery.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                EmbroideryApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmbroideryApp() {
    var currentScreen by remember { mutableStateOf("home") }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Embroidery Studio Pro", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6200EA),
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Text("🏠") },
                    label = { Text("الرئيسية") },
                    selected = currentScreen == "home",
                    onClick = { currentScreen = "home" }
                )
                NavigationBarItem(
                    icon = { Text("🖼️") },
                    label = { Text("مشاريعي") },
                    selected = currentScreen == "projects",
                    onClick = { currentScreen = "projects" }
                )
                NavigationBarItem(
                    icon = { Text("⚙️") },
                    label = { Text("الإعدادات") },
                    selected = currentScreen == "settings",
                    onClick = { currentScreen = "settings" }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentScreen) {
                "home" -> HomeScreen()
                "projects" -> ProjectsScreen()
                "settings" -> SettingsScreen()
            }
        }
    }
}

@Composable
fun HomeScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("ابدأ مشروع جديد", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }
        item {
            ActionCard("📷 استيراد صورة", "التقط أو اختر صورة من المعرض", Color(0xFF6200EA))
        }
        item {
            ActionCard("🧵 استخراج النقشة", "فصل النقشة عن الخلفية", Color(0xFF00897B))
        }
        item {
            ActionCard("🎨 تحليل الألوان", "اكتشاف وفصل الألوان", Color(0xFFE65100))
        }
        item {
            ActionCard("⚡ تحويل لغرز", "توليد بيانات غرز التطريز", Color(0xFFC62828))
        }
        item {
            ActionCard("👁️ معاينة", "مشاهدة التصميم النهائي", Color(0xFF283593))
        }
        item {
            ActionCard("📤 تصدير", "تصدير DST و PNG", Color(0xFF558B2F))
        }
    }
}

@Composable
fun ActionCard(title: String, subtitle: String, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            }
            Text("←", color = Color.White, fontSize = 24.sp)
        }
    }
}

@Composable
fun ProjectsScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🗂️", fontSize = 64.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("لا توجد مشاريع بعد", fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {}) { Text("إنشاء مشروع جديد") }
        }
    }
}

@Composable
fun SettingsScreen() {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item { Text("الإعدادات", fontSize = 22.sp, fontWeight = FontWeight.Bold) }
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item { SettingItem("اللغة", "العربية") }
        item { SettingItem("وحدة القياس", "ملليمتر") }
        item { SettingItem("جودة الاستخراج", "عالية") }
        item { SettingItem("الإصدار", "1.0.0") }
    }
}

@Composable
fun SettingItem(title: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 16.sp)
            Text(value, color = Color(0xFF6200EA), fontSize = 16.sp)
        }
    }
}
