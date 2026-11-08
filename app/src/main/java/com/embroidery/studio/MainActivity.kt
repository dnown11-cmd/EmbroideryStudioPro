package com.embroidery.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EmbroideryTheme {
                EmbroideryApp()
            }
        }
    }
}

@Composable
fun EmbroideryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF9C6FDE),
            secondary = Color(0xFF6FCFDE),
            background = Color(0xFF0D0D1A),
            surface = Color(0xFF1A1A2E),
            onPrimary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White
        ),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmbroideryApp() {
    var currentScreen by remember { mutableStateOf("home") }
    var selectedStep by remember { mutableStateOf(-1) }

    Scaffold(
        containerColor = Color(0xFF0D0D1A),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF9C6FDE), Color(0xFF6FCFDE))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🧵", fontSize = 18.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "Embroidery Studio Pro",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                "v2.0 • 2026",
                                fontSize = 10.sp,
                                color = Color(0xFF9C6FDE)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D0D1A)
                ),
                actions = {
                    IconButton(onClick = { currentScreen = "settings" }) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF9C6FDE))
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF1A1A2E)) {
                listOf(
                    Triple("home", Icons.Default.Home, "الرئيسية"),
                    Triple("projects", Icons.Default.List, "مشاريعي"),
                    Triple("settings", Icons.Default.Settings, "الإعدادات")
                ).forEach { (screen, icon, label) ->
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = null) },
                        label = { Text(label, fontSize = 11.sp) },
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF9C6FDE),
                            selectedTextColor = Color(0xFF9C6FDE),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color(0xFF2A2A3E)
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentScreen) {
                "home" -> HomeScreen(selectedStep) { selectedStep = it }
                "projects" -> ProjectsScreen()
                "settings" -> SettingsScreen()
            }
        }
    }
}

@Composable
fun HomeScreen(selectedStep: Int, onStepSelect: (Int) -> Unit) {
    val steps = listOf(
        StepData("استيراد صورة", "التقط أو اختر من المعرض", Icons.Default.Add, Color(0xFF9C6FDE), Color(0xFF7B4FBE)),
        StepData("استخراج النقشة", "فصل النقشة عن الخلفية", Icons.Default.Star, Color(0xFF4FBEAA), Color(0xFF3A9E8A)),
        StepData("تحليل الألوان", "اكتشاف وفصل الألوان", Icons.Default.Favorite, Color(0xFFDE9C6F), Color(0xFFBE7A4F)),
        StepData("تحويل لغرز", "توليد بيانات غرز حقيقية", Icons.Default.Build, Color(0xFFDE6F6F), Color(0xFFBE4F4F)),
        StepData("معاينة", "محاكاة التطريز", Icons.Default.PlayArrow, Color(0xFF6F9CDE), Color(0xFF4F7CBE)),
        StepData("تصدير", "DST و PES و PNG", Icons.Default.Share, Color(0xFF6FDE9C), Color(0xFF4FBE7A))
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF1A1A3E), Color(0xFF2A1A3E))
                        )
                    )
                    .border(
                        1.dp,
                        Brush.linearGradient(listOf(Color(0xFF9C6FDE), Color(0xFF6FCFDE))),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Text("مرحباً بك 👋", color = Color.Gray, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("ابدأ مشروع تطريز جديد", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("6 خطوات للحصول على تصميم احترافي", color = Color(0xFF9C6FDE), fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {},
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C6FDE)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text("مشروع جديد", fontSize = 13.sp)
                    }
                }
            }
        }

        item {
            Text("خطوات العمل", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        steps.forEachIndexed { index, step ->
            item {
                StepCard(
                    step = step,
                    index = index + 1,
                    isSelected = selectedStep == index,
                    onClick = { onStepSelect(if (selectedStep == index) -1 else index) }
                )
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

data class StepData(val title: String, val subtitle: String, val icon: ImageVector, val color: Color, val darkColor: Color)

@Composable
fun StepCard(step: StepData, index: Int, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
        border = BorderStroke(
            if (isSelected) 1.5.dp else 0.5.dp,
            if (isSelected) step.color else Color(0xFF2A2A3E)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(step.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("$index", color = step.color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(step.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(step.subtitle, color = Color.Gray, fontSize = 12.sp)
            }
            Icon(
                if (isSelected) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                contentDescription = null,
                tint = step.color,
                modifier = Modifier.size(20.dp)
            )
        }

        if (isSelected) {
            Divider(color = Color(0xFF2A2A3E))
            Box(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = step.color),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("بدء ${step.title}", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun ProjectsScreen() {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0D0D1A)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🗂️", fontSize = 64.sp)
            Spacer(Modifier.height(16.dp))
            Text("لا توجد مشاريع بعد", color = Color.White, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            Text("ابدأ مشروعك الأول من الرئيسية", color = Color.Gray, fontSize = 13.sp)
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C6FDE)),
                shape = RoundedCornerShape(12.dp)
            ) { Text("إنشاء مشروع جديد") }
        }
    }
}

@Composable
fun SettingsScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0D0D1A)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Text("الإعدادات", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        item { Spacer(Modifier.height(4.dp)) }

        listOf(
            Triple("اللغة", "العربية", "🌐"),
            Triple("وحدة القياس", "ملليمتر", "📐"),
            Triple("جودة الاستخراج", "عالية", "⚡"),
            Triple("الوضع", "داكن", "🌙"),
            Triple("الإصدار", "2.0.0 • 2026", "📱")
        ).forEach { (title, value, emoji) ->
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(emoji, fontSize = 20.sp)
                            Spacer(Modifier.width(12.dp))
                            Text(title, color = Color.White, fontSize = 15.sp)
                        }
                        Text(value, color = Color(0xFF9C6FDE), fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
