package com.embroidery.studio

import android.graphics.*
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppTheme { MainApp() } }
    }
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF9C6FDE),
            background = Color(0xFF0D0D1A),
            surface = Color(0xFF1A1A2E),
            onBackground = Color.White,
            onSurface = Color.White
        ),
        content = content
    )
}

data class Stitch(val x: Float, val y: Float, val colorIdx: Int)

data class Project(
    val name: String = "مشروع جديد",
    val imageUri: Uri? = null,
    val extracted: Bitmap? = null,
    val colors: List<Int> = emptyList(),
    val stitches: List<Stitch> = emptyList(),
    val widthMM: Float = 0f,
    val heightMM: Float = 0f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    var screen by remember { mutableStateOf("home") }
    var project by remember { mutableStateOf(Project()) }
    var loading by remember { mutableStateOf(false) }
    var loadingMsg by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Color(0xFF0D0D1A),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Embroidery Studio Pro", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("v3.0 • 2026", fontSize = 10.sp, color = Color(0xFF9C6FDE))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D0D1A)),
                actions = {
                    if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp).padding(end = 4.dp), color = Color(0xFF9C6FDE), strokeWidth = 2.dp)
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF1A1A2E)) {
                listOf(Triple("home", Icons.Default.Home, "الرئيسية"), Triple("projects", Icons.Default.List, "مشاريعي"), Triple("settings", Icons.Default.Settings, "الإعدادات")).forEach { (s, icon, label) ->
                    NavigationBarItem(icon = { Icon(icon, null) }, label = { Text(label, fontSize = 11.sp) }, selected = screen == s, onClick = { screen = s },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF9C6FDE), selectedTextColor = Color(0xFF9C6FDE), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray, indicatorColor = Color(0xFF2A2A3E)))
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when (screen) {
                "home" -> HomeScreen(project, loading, loadingMsg, { project = it }, { b, m -> loading = b; loadingMsg = m })
                "projects" -> EmptyScreen("مشاريعي", "🗂️", "لا توجد مشاريع محفوظة")
                "settings" -> SettingsScreen()
            }
        }
    }
}

@Composable
fun HomeScreen(project: Project, loading: Boolean, msg: String, onUpdate: (Project) -> Unit, onLoad: (Boolean, String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { onUpdate(project.copy(imageUri = it)) } }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Box(Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(20.dp))
                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF1A1A3E), Color(0xFF2A1A3E))))
                .border(1.dp, androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF9C6FDE), Color(0xFF6FCFDE))), RoundedCornerShape(20.dp))
                .padding(16.dp)) {
                Column {
                    Text("ابدأ مشروع تطريز 🧵", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(when {
                        project.stitches.isNotEmpty() -> "✅ ${project.stitches.size} غرزة • ${String.format("%.1f", project.widthMM)}×${String.format("%.1f", project.heightMM)} مم"
                        project.colors.isNotEmpty() -> "✅ ${project.colors.size} ألوان مكتشفة"
                        project.extracted != null -> "✅ تم استخراج النقشة"
                        project.imageUri != null -> "✅ تم تحميل الصورة"
                        else -> "6 خطوات للحصول على تصميم احترافي"
                    }, color = Color(0xFF9C6FDE), fontSize = 12.sp)
                }
            }
        }

        if (loading) item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2A1A)), border = BorderStroke(1.dp, Color(0xFF4FDE9C))) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Color(0xFF4FDE9C), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(msg, color = Color(0xFF4FDE9C), fontSize = 13.sp)
                }
            }
        }

        if (project.imageUri != null) item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))) {
                Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AsyncImage(project.imageUri, null, Modifier.weight(1f).height(100.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                    if (project.extracted != null)
                        Image(project.extracted.asImageBitmap(), null, Modifier.weight(1f).height(100.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                }
            }
        }

        item { Step(1, "استيراد صورة", "اختر من المعرض", Color(0xFF9C6FDE), true) { picker.launch("image/*") } }
        item { Step(2, "استخراج النقشة", "فصل النقشة عن الخلفية", Color(0xFF4FBEAA), project.imageUri != null) {
            scope.launch {
                onLoad(true, "جاري الاستخراج...")
                val bm = loadBitmap(ctx, project.imageUri!!)
                if (bm != null) onUpdate(project.copy(extracted = extract(bm)))
                onLoad(false, "")
            }
        }}
        item { Step(3, "تحليل الألوان", "اكتشاف وفصل الألوان", Color(0xFFDE9C6F), project.extracted != null) {
            scope.launch {
                onLoad(true, "جاري تحليل الألوان...")
                val c = colors(project.extracted!!)
                onUpdate(project.copy(colors = c))
                onLoad(false, "")
            }
        }}
        if (project.colors.isNotEmpty()) item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))) {
                Column(Modifier.padding(14.dp)) {
                    Text("الألوان (${project.colors.size})", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(project.colors) { c ->
                            Box(Modifier.size(36.dp).clip(CircleShape).background(Color(c)).border(1.dp, Color.White.copy(0.2f), CircleShape))
                        }
                    }
                }
            }
        }
        item { Step(4, "تحويل لغرز", "توليد بيانات الغرز", Color(0xFFDE6F6F), project.colors.isNotEmpty()) {
            scope.launch {
                onLoad(true, "جاري توليد الغرز...")
                val s = stitches(project.extracted!!, project.colors)
                onUpdate(project.copy(stitches = s, widthMM = project.extracted.width * 0.1f, heightMM = project.extracted.height * 0.1f))
                onLoad(false, "")
            }
        }}
        if (project.stitches.isNotEmpty()) item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceAround) {
                    InfoBox("الغرز", "${project.stitches.size}")
                    InfoBox("العرض", "${String.format("%.1f", project.widthMM)}مم")
                    InfoBox("الارتفاع", "${String.format("%.1f", project.heightMM)}مم")
                    InfoBox("الألوان", "${project.colors.size}")
                }
            }
        }
        item { Step(5, "معاينة", "مشاهدة التصميم", Color(0xFF6F9CDE), project.stitches.isNotEmpty()) {} }
        if (project.stitches.isNotEmpty()) item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))) {
                Canvas(Modifier.fillMaxWidth().height(180.dp).padding(8.dp).clip(RoundedCornerShape(10.dp)).background(Color.White)) {
                    val mx = project.stitches.maxOfOrNull { it.x } ?: 1f
                    val my = project.stitches.maxOfOrNull { it.y } ?: 1f
                    val sc = minOf(size.width / mx, size.height / my) * 0.9f
                    val ox = (size.width - mx * sc) / 2f
                    val oy = (size.height - my * sc) / 2f
                    project.stitches.take(3000).forEach { st ->
                        val c = if (st.colorIdx < project.colors.size) Color(project.colors[st.colorIdx]) else Color.Black
                        drawCircle(c, 1.5f, androidx.compose.ui.geometry.Offset(ox + st.x * sc, oy + st.y * sc))
                    }
                }
            }
        }
        item { Step(6, "تصدير", "DST و PNG", Color(0xFF6FDE9C), project.stitches.isNotEmpty()) {
            scope.launch {
                onLoad(true, "جاري التصدير...")
                export(ctx, project)
                onLoad(false, "")
            }
        }}
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
fun Step(n: Int, title: String, sub: String, color: Color, on: Boolean, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(enabled = on) { onClick() }, shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
        border = BorderStroke(if (on) 1.dp else 0.5.dp, if (on) color else Color(0xFF2A2A3E))) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(if (on) color.copy(0.2f) else Color(0xFF2A2A3E)), contentAlignment = Alignment.Center) {
                Text("$n", color = if (on) color else Color.Gray, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = if (on) Color.White else Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(sub, color = Color.Gray, fontSize = 11.sp)
            }
            if (on) Icon(Icons.Default.PlayArrow, null, tint = color, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun InfoBox(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color(0xFF9C6FDE), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color.Gray, fontSize = 10.sp)
    }
}

@Composable
fun EmptyScreen(title: String, emoji: String, msg: String) {
    Box(Modifier.fillMaxSize().background(Color(0xFF0D0D1A)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 56.sp)
            Spacer(Modifier.height(12.dp))
            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(msg, color = Color.Gray, fontSize = 13.sp)
        }
    }
}

@Composable
fun SettingsScreen() {
    LazyColumn(Modifier.fillMaxSize().background(Color(0xFF0D0D1A)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("الإعدادات", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        listOf("🌐" to Pair("اللغة", "العربية"), "📐" to Pair("وحدة القياس", "ملليمتر"), "⚡" to Pair("جودة الاستخراج", "عالية"), "🌙" to Pair("الوضع", "داكن"), "📱" to Pair("الإصدار", "3.0 • 2026")).forEach { (e, p) ->
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))) {
                    Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) { Text(e, fontSize = 18.sp); Spacer(Modifier.width(10.dp)); Text(p.first, color = Color.White, fontSize = 14.sp) }
                        Text(p.second, color = Color(0xFF9C6FDE), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

suspend fun loadBitmap(ctx: android.content.Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    try { ctx.contentResolver.openInputStream(uri)?.let { BitmapFactory.decodeStream(it) } } catch (e: Exception) { null }
}

suspend fun extract(bm: Bitmap): Bitmap = withContext(Dispatchers.Default) {
    val w = bm.width; val h = bm.height
    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val px = IntArray(w * h); bm.getPixels(px, 0, w, 0, 0, w, h)
    val bg = px[0]
    val br = android.graphics.Color.red(bg); val bg2 = android.graphics.Color.green(bg); val bb = android.graphics.Color.blue(bg)
    for (i in px.indices) {
        val r = android.graphics.Color.red(px[i]); val g = android.graphics.Color.green(px[i]); val b = android.graphics.Color.blue(px[i])
        px[i] = if (abs(r - br) + abs(g - bg2) + abs(b - bb) > 60) px[i] else android.graphics.Color.TRANSPARENT
    }
    out.setPixels(px, 0, w, 0, 0, w, h); out
}

suspend fun colors(bm: Bitmap): List<Int> = withContext(Dispatchers.Default) {
    val map = mutableMapOf<Int, Int>()
    for (y in 0 until bm.height step 4) for (x in 0 until bm.width step 4) {
        val p = bm.getPixel(x, y)
        if (android.graphics.Color.alpha(p) > 128) {
            val q = android.graphics.Color.rgb((android.graphics.Color.red(p) / 32) * 32, (android.graphics.Color.green(p) / 32) * 32, (android.graphics.Color.blue(p) / 32) * 32)
            map[q] = (map[q] ?: 0) + 1
        }
    }
    map.entries.sortedByDescending { it.value }.take(8).map { it.key }
}

suspend fun stitches(bm: Bitmap, cols: List<Int>): List<Stitch> = withContext(Dispatchers.Default) {
    val list = mutableListOf<Stitch>()
    for (y in 0 until bm.height step 4) {
        val lr = (y / 4) % 2 == 0
        val xs = if (lr) (0 until bm.width step 4) else (bm.width - 1 downTo 0 step 4)
        for (x in xs) {
            val p = bm.getPixel(x, y)
            if (android.graphics.Color.alpha(p) > 128) {
                var md = Int.MAX_VALUE; var ni = 0
                cols.forEachIndexed { i, c ->
                    val d = (android.graphics.Color.red(p) - android.graphics.Color.red(c)).let { it * it } +
                            (android.graphics.Color.green(p) - android.graphics.Color.green(c)).let { it * it } +
                            (android.graphics.Color.blue(p) - android.graphics.Color.blue(c)).let { it * it }
                    if (d < md) { md = d; ni = i }
                }
                list.add(Stitch(x.toFloat(), y.toFloat(), ni))
            }
        }
    }
    list
}

suspend fun export(ctx: android.content.Context, p: Project) = withContext(Dispatchers.IO) {
    val dir = ctx.getExternalFilesDir(null) ?: return@withContext
    p.extracted?.let { bm ->
        FileOutputStream(File(dir, "${p.name}.png")).use { bm.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    if (p.stitches.isNotEmpty()) {
        FileOutputStream(File(dir, "${p.name}.dst")).use { fos ->
            val hdr = ByteArray(512) { 0x20 }
            fos.write(hdr)
            var lx = 0; var ly = 0
            p.stitches.forEach { s ->
                val dx = (s.x - lx).toInt().coerceIn(-121, 121)
                val dy = (s.y - ly).toInt().coerceIn(-121, 121)
                lx += dx; ly += dy
                fos.write(byteArrayOf(dx.toByte(), dy.toByte(), 0x03))
            }
            fos.write(byteArrayOf(0x00, 0x00, 0xF3.toByte()))
        }
    }
}
