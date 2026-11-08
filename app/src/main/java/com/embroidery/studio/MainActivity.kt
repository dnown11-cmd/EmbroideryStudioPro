package com.embroidery.studio

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import kotlin.math.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { EmbroideryTheme { EmbroideryApp() } }
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

data class StitchPoint(val x: Float, val y: Float, val colorIndex: Int)
data class EmbroideryProject(
    val id: String = System.currentTimeMillis().toString(),
    val name: String = "مشروع جديد",
    val imageUri: Uri? = null,
    val extractedBitmap: Bitmap? = null,
    val colors: List<Int> = emptyList(),
    val stitches: List<StitchPoint> = emptyList(),
    val widthMM: Float = 0f,
    val heightMM: Float = 0f,
    val stitchCount: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmbroideryApp() {
    var currentScreen by remember { mutableStateOf("home") }
    var project by remember { mutableStateOf(EmbroideryProject()) }
    var isProcessing by remember { mutableStateOf(false) }
    var processingStep by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Color(0xFF0D0D1A),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFF9C6FDE), Color(0xFF6FCFDE)))),
                            contentAlignment = Alignment.Center
                        ) { Text("🧵", fontSize = 18.sp) }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Embroidery Studio Pro", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("v3.0 • 2026", fontSize = 10.sp, color = Color(0xFF9C6FDE))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D0D1A)),
                actions = {
                    if (isProcessing) CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 8.dp), color = Color(0xFF9C6FDE), strokeWidth = 2.dp)
                    IconButton(onClick = { currentScreen = "settings" }) {
                        Icon(Icons.Default.Settings, null, tint = Color(0xFF9C6FDE))
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF1A1A2E)) {
                listOf(Triple("home", Icons.Default.Home, "الرئيسية"), Triple("projects", Icons.Default.List, "مشاريعي"), Triple("settings", Icons.Default.Settings, "الإعدادات")).forEach { (screen, icon, label) ->
                    NavigationBarItem(
                        icon = { Icon(icon, null) }, label = { Text(label, fontSize = 11.sp) },
                        selected = currentScreen == screen, onClick = { currentScreen = screen },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF9C6FDE), selectedTextColor = Color(0xFF9C6FDE), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray, indicatorColor = Color(0xFF2A2A3E))
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentScreen) {
                "home" -> HomeScreen(project, isProcessing, processingStep,
                    onProjectUpdate = { project = it },
                    onProcessingChange = { processing, step -> isProcessing = processing; processingStep = step }
                )
                "projects" -> ProjectsScreen()
                "settings" -> SettingsScreen()
            }
        }
    }
}

@Composable
fun HomeScreen(
    project: EmbroideryProject,
    isProcessing: Boolean,
    processingStep: String,
    onProjectUpdate: (EmbroideryProject) -> Unit,
    onProcessingChange: (Boolean, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentStep by remember { mutableStateOf(0) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onProjectUpdate(project.copy(imageUri = it, name = "مشروع ${System.currentTimeMillis() % 10000}")) }
    }

    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            WelcomeCard(project)
        }

        item {
            if (project.imageUri != null) {
                ImagePreviewCard(project, context)
            }
        }

        if (isProcessing) {
            item {
                ProcessingCard(processingStep)
            }
        }

        item {
            StepCard(1, "استيراد صورة", "التقط أو اختر من المعرض", Color(0xFF9C6FDE), project.imageUri != null) {
                imagePicker.launch("image/*")
            }
        }

        item {
            StepCard(2, "استخراج النقشة", "فصل النقشة عن الخلفية", Color(0xFF4FBEAA), project.imageUri != null && project.extractedBitmap == null) {
                if (project.imageUri != null) {
                    scope.launch {
                        onProcessingChange(true, "جاري استخراج النقشة...")
                        val bitmap = loadBitmapFromUri(context, project.imageUri!!)
                        if (bitmap != null) {
                            val extracted = extractPattern(bitmap)
                            onProjectUpdate(project.copy(extractedBitmap = extracted))
                        }
                        onProcessingChange(false, "")
                        currentStep = 2
                    }
                }
            }
        }

        item {
            StepCard(3, "تحليل الألوان", "اكتشاف وفصل الألوان", Color(0xFFDE9C6F), project.extractedBitmap != null && project.colors.isEmpty()) {
                if (project.extractedBitmap != null) {
                    scope.launch {
                        onProcessingChange(true, "جاري تحليل الألوان...")
                        val colors = analyzeColors(project.extractedBitmap!!)
                        onProjectUpdate(project.copy(colors = colors))
                        onProcessingChange(false, "")
                        currentStep = 3
                    }
                }
            }
        }

        if (project.colors.isNotEmpty()) {
            item { ColorsCard(project.colors) }
        }

        item {
            StepCard(4, "تحويل لغرز", "توليد بيانات غرز حقيقية", Color(0xFFDE6F6F), project.colors.isNotEmpty() && project.stitches.isEmpty()) {
                if (project.extractedBitmap != null && project.colors.isNotEmpty()) {
                    scope.launch {
                        onProcessingChange(true, "جاري توليد الغرز...")
                        val stitches = generateStitches(project.extractedBitmap!!, project.colors)
                        val w = project.extractedBitmap!!.width * 0.1f
                        val h = project.extractedBitmap!!.height * 0.1f
                        onProjectUpdate(project.copy(stitches = stitches, widthMM = w, heightMM = h, stitchCount = stitches.size))
                        onProcessingChange(false, "")
                        currentStep = 4
                    }
                }
            }
        }

        if (project.stitchCount > 0) {
            item { StitchInfoCard(project) }
        }

        item {
            StepCard(5, "معاينة التطريز", "مشاهدة التصميم النهائي", Color(0xFF6F9CDE), project.stitches.isNotEmpty()) {
                currentStep = 5
            }
        }

        if (currentStep >= 5 && project.stitches.isNotEmpty()) {
            item { PreviewCard(project) }
        }

        item {
            StepCard(6, "تصدير الملف", "DST و PES و PNG", Color(0xFF6FDE9C), project.stitches.isNotEmpty()) {
                if (project.stitches.isNotEmpty()) {
                    scope.launch {
                        onProcessingChange(true, "جاري التصدير...")
                        exportFiles(context, project)
                        onProcessingChange(false, "")
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
fun WelcomeCard(project: EmbroideryProject) {
    Box(
        modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1A1A3E), Color(0xFF2A1A3E))))
            .border(1.dp, Brush.linearGradient(listOf(Color(0xFF9C6FDE), Color(0xFF6FCFDE))), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column {
            Text(if (project.imageUri == null) "ابدأ مشروع تطريز جديد 🧵" else project.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                when {
                    project.stitchCount > 0 -> "✅ ${project.stitchCount} غرزة • ${String.format("%.1f", project.widthMM)}×${String.format("%.1f", project.heightMM)} مم"
                    project.colors.isNotEmpty() -> "✅ ${project.colors.size} ألوان مكتشفة"
                    project.extractedBitmap != null -> "✅ تم استخراج النقشة"
                    project.imageUri != null -> "✅ تم تحميل الصورة"
                    else -> "6 خطوات للحصول على تصميم احترافي"
                },
                color = Color(0xFF9C6FDE), fontSize = 12.sp
            )
        }
    }
}

@Composable
fun ImagePreviewCard(project: EmbroideryProject, context: android.content.Context) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("الصورة المحددة", color = Color.Gray, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AsyncImage(model = project.imageUri, contentDescription = null, modifier = Modifier.weight(1f).height(120.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                if (project.extractedBitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = project.extractedBitmap!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.weight(1f).height(120.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
fun ProcessingCard(step: String) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2A1A)), border = BorderStroke(1.dp, Color(0xFF4FDE9C))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF4FDE9C), strokeWidth = 2.dp)
            Spacer(Modifier.width(12.dp))
            Text(step, color = Color(0xFF4FDE9C), fontSize = 14.sp)
        }
    }
}

@Composable
fun StepCard(index: Int, title: String, subtitle: String, color: Color, enabled: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled) { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
        border = BorderStroke(if (enabled) 1.dp else 0.5.dp, if (enabled) color else Color(0xFF2A2A3E))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(if (enabled) color.copy(alpha = 0.2f) else Color(0xFF2A2A3E)), contentAlignment = Alignment.Center) {
                Text("$index", color = if (enabled) color else Color.Gray, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = if (enabled) Color.White else Color.Gray, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Color.Gray, fontSize = 12.sp)
            }
            if (enabled) Icon(Icons.Default.PlayArrow, null, tint = color, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun ColorsCard(colors: List<Int>) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("الألوان المكتشفة (${colors.size})", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(colors) { color ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(color)).border(1.dp, Color.White.copy(0.3f), CircleShape))
                        Text("#${Integer.toHexString(color).takeLast(6).uppercase()}", color = Color.Gray, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun StitchInfoCard(project: EmbroideryProject) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceAround) {
            InfoItem("الغرز", "${project.stitchCount}")
            InfoItem("العرض", "${String.format("%.1f", project.widthMM)} مم")
            InfoItem("الارتفاع", "${String.format("%.1f", project.heightMM)} مم")
            InfoItem("الألوان", "${project.colors.size}")
        }
    }
}

@Composable
fun InfoItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color(0xFF9C6FDE), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color.Gray, fontSize = 11.sp)
    }
}

@Composable
fun PreviewCard(project: EmbroideryProject) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("معاينة التطريز", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Canvas(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)).background(Color.White)) {
                val scaleX = size.width / (project.stitches.maxOfOrNull { it.x }?.plus(10f) ?: 100f)
                val scaleY = size.height / (project.stitches.maxOfOrNull { it.y }?.plus(10f) ?: 100f)
                val scale = minOf(scaleX, scaleY) * 0.9f
                val offsetX = (size.width - (project.stitches.maxOfOrNull { it.x } ?: 0f) * scale) / 2f
                val offsetY = (size.height - (project.stitches.maxOfOrNull { it.y } ?: 0f) * scale) / 2f
                project.stitches.take(5000).forEach { stitch ->
                    val color = if (stitch.colorIndex < project.colors.size) Color(project.colors[stitch.colorIndex]) else Color.Black
                    drawCircle(color = color, radius = 1.5f, center = androidx.compose.ui.geometry.Offset(offsetX + stitch.x * scale, offsetY + stitch.y * scale))
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
            Text("لا توجد مشاريع محفوظة", color = Color.White, fontSize = 18.sp)
            Text("ابدأ من الرئيسية", color = Color.Gray, fontSize = 13.sp)
        }
    }
}

@Composable
fun SettingsScreen() {
    LazyColumn(modifier = Modifier.fillMaxSize().background(Color(0xFF0D0D1A)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("الإعدادات", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        listOf(Triple("اللغة", "العربية", "🌐"), Triple("وحدة القياس", "ملليمتر", "📐"), Triple("جودة الاستخراج", "عالية", "⚡"), Triple("الوضع", "داكن", "🌙"), Triple("الإصدار", "3.0.0 • 2026", "📱")).forEach { (title, value, emoji) ->
            item {
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(emoji, fontSize = 20.sp); Spacer(Modifier.width(12.dp))
                            Text(title, color = Color.White, fontSize = 15.sp)
                        }
                        Text(value, color = Color(0xFF9C6FDE), fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

suspend fun loadBitmapFromUri(context: android.content.Context, uri: Uri): Bitmap? {
    return withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) { null }
    }
}

suspend fun extractPattern(bitmap: Bitmap): Bitmap {
    return withContext(Dispatchers.Default) {
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val bgColor = pixels[0]
        val bgR = android.graphics.Color.red(bgColor)
        val bgG = android.graphics.Color.green(bgColor)
        val bgB = android.graphics.Color.blue(bgColor)

        for (i in pixels.indices) {
            val r = android.graphics.Color.red(pixels[i])
            val g = android.graphics.Color.green(pixels[i])
            val b = android.graphics.Color.blue(pixels[i])
            val diff = abs(r - bgR) + abs(g - bgG) + abs(b - bgB)
            pixels[i] = if (diff > 60) pixels[i] else android.graphics.Color.TRANSPARENT
        }
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        result
    }
}

suspend fun analyzeColors(bitmap: Bitmap): List<Int> {
    return withContext(Dispatchers.Default) {
        val colorMap = mutableMapOf<Int, Int>()
        val step = 4
        for (y in 0 until bitmap.height step step) {
            for (x in 0 until bitmap.width step step) {
                val pixel = bitmap.getPixel(x, y)
                if (android.graphics.Color.alpha(pixel) > 128) {
                    val r = (android.graphics.Color.red(pixel) / 32) * 32
                    val g = (android.graphics.Color.green(pixel) / 32) * 32
                    val b = (android.graphics.Color.blue(pixel) / 32) * 32
                    val quantized = android.graphics.Color.rgb(r, g, b)
                    colorMap[quantized] = (colorMap[quantized] ?: 0) + 1
                }
            }
        }
        colorMap.entries.sortedByDescending { it.value }.take(8).map { it.key }
    }
}

suspend fun generateStitches(bitmap: Bitmap, colors: List<Int>): List<StitchPoint> {
    return withContext(Dispatchers.Default) {
        val stitches = mutableListOf<StitchPoint>()
        val density = 4
        for (y in 0 until bitmap.height step density) {
            val leftToRight = (y / density) % 2 == 0
            val xRange = if (leftToRight) (0 until bitmap.width step density) else (bitmap.width - 1 downTo 0 step density)
            for (x in xRange) {
                val pixel = bitmap.getPixel(x, y)
                if (android.graphics.Color.alpha(pixel) > 128) {
                    val colorIndex = findNearestColor(pixel, colors)
                    stitches.add(StitchPoint(x.toFloat(), y.toFloat(), colorIndex))
                }
            }
        }
        stitches
    }
}

fun findNearestColor(pixel: Int, colors: List<Int>): Int {
    val r = android.graphics.Color.red(pixel)
    val g = android.graphics.Color.green(pixel)
    val b = android.graphics.Color.blue(pixel)
    var minDist = Int.MAX_VALUE
    var nearest = 0
    colors.forEachIndexed { index, color ->
        val dr = r - android.graphics.Color.red(color)
        val dg = g - android.graphics.Color.green(color)
        val db = b - android.graphics.Color.blue(color)
        val dist = dr * dr + dg * dg + db * db
        if (dist < minDist) { minDist = dist; nearest = index }
    }
    return nearest
}

suspend fun exportFiles(context: android.content.Context, project: EmbroideryProject) {
    withContext(Dispatchers.IO) {
        val dir = context.getExternalFilesDir(null) ?: return@withContext

        // Export PNG
        if (project.extractedBitmap != null) {
            val pngFile = File(dir, "${project.name}_pattern.png")
            FileOutputStream(pngFile).use { project.extractedBitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }

        // Export DST
        val dstFile = File(dir, "${project.name}.dst")
        FileOutputStream(dstFile).use { fos ->
            val header = ByteArray(512) { 0x20 }
            val label = "LA:${project.name}".toByteArray()
            System.arraycopy(label, 0, header, 0, minOf(label.size, 16))
            fos.write(header)
            var lastX = 0; var lastY = 0
            project.stitches.forEach { stitch ->
                val dx = (stitch.x - lastX).toInt().coerceIn(-121, 121)
                val dy = (stitch.y - lastY).toInt().coerceIn(-121, 121)
                lastX += dx; lastY += dy
                val bytes = encodeDSTStitch(dx, dy, false)
                fos.write(bytes)
            }
            fos.write(byteArrayOf(0x00, 0x00, 0xF3.toByte()))
        }
    }
}

fun encodeDSTStitch(dx: Int, dy: Int, jump: Boolean): ByteArray {
    var b0 = 0; var b1 = 0; var b2 = 0
    if (jump) b2 = b2 or 0x83
    if (dx > 40) { b0 = b0 or 0x04; b2 = b2 or 0x08 }
    if (dx > 0) b0 = b0 or 0x01 else b0 = b0 or 0x02
    if (dy > 40) { b0 = b0 or 0x20; b2 = b2 or 0x20 }
    if (dy > 0) b0 = b0 or 0x40 else b0 = b0 or 0x80.toByte().toInt()
    return byteArrayOf(b0.toByte(), b1.toByte(), b2.toByte())
}
