package com.embroidery.studio

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

enum class StitchType { RUN, SATIN, TATAMI }

data class Stitch(val x: Float, val y: Float, val colorIdx: Int, val type: StitchType = StitchType.TATAMI, val angle: Float = 0f)

data class QualityReport(val totalStitches: Int, val coverage: Float, val outOfBounds: Float, val colorCount: Int, val widthMM: Float, val heightMM: Float)

data class Project(
    val id: String = System.currentTimeMillis().toString(),
    val name: String = "مشروع جديد",
    val imageUri: Uri? = null,
    val extracted: Bitmap? = null,
    val colors: List<Int> = emptyList(),
    val stitches: List<Stitch> = emptyList(),
    val widthMM: Float = 0f,
    val heightMM: Float = 0f,
    val savedAt: Long = 0L,
    val quality: QualityReport? = null
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppTheme { MainApp() } }
    }
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF9C6FDE), background = Color(0xFF0D0D1A), surface = Color(0xFF1A1A2E), onBackground = Color.White, onSurface = Color.White), content = content)
}

@Composable
fun ZoomableImageDialog(bitmap: Bitmap, onDismiss: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            Image(
                bitmap.asImageBitmap(), null,
                modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 8f)
                        offset += pan
                    }
                }.graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y),
                contentScale = ContentScale.Fit
            )
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Text("اضغط مطولاً للتكبير • اسحب للتحريك", color = Color.White.copy(0.6f), fontSize = 11.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
        }
    }
}

@Composable
fun ZoomablePreviewDialog(stitches: List<Stitch>, colors: List<Int>, onDismiss: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(100) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            if (progress >= 100) progress = 0
            while (isPlaying && progress < 100) { progress += 1; delay(50) }
            isPlaying = false
        }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color(0xFF0D0D1A))) {
            val showCount = (stitches.size * progress / 100).coerceAtLeast(1)
            Canvas(Modifier.fillMaxSize().pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 10f)
                    offset += pan
                }
            }.graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y)) {
                val visibleStitches = stitches.take(showCount)
                val mx = stitches.maxOfOrNull { it.x } ?: 1f
                val my = stitches.maxOfOrNull { it.y } ?: 1f
                val sc = minOf(size.width / mx, size.height / my) * 0.9f
                val ox = (size.width - mx * sc) / 2f
                val oy = (size.height - my * sc) / 2f
                visibleStitches.forEachIndexed { idx, st ->
                    val c = if (st.colorIdx < colors.size) Color(colors[st.colorIdx]) else Color.Black
                    val sx = ox + st.x * sc; val sy = oy + st.y * sc
                    when (st.type) {
                        StitchType.SATIN -> { val len = 8f; val ex = sx + cos(st.angle) * len; val ey = sy + sin(st.angle) * len; drawLine(c, Offset(sx, sy), Offset(ex, ey), strokeWidth = 3f, cap = StrokeCap.Round) }
                        StitchType.RUN -> { if (idx > 0) { val prev = visibleStitches[idx-1]; if (prev.colorIdx == st.colorIdx) drawLine(c, Offset(ox+prev.x*sc, oy+prev.y*sc), Offset(sx, sy), strokeWidth = 2f, cap = StrokeCap.Round) } }
                        StitchType.TATAMI -> drawCircle(c, 2f, Offset(sx, sy))
                    }
                }
            }
            Column(Modifier.align(Alignment.BottomCenter).background(Color(0xCC0D0D1A)).padding(12.dp)) {
                Text("$progress%  •  $showCount / ${stitches.size} غرزة", color = Color.White, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                Slider(value = progress.toFloat(), onValueChange = { progress = it.toInt(); isPlaying = false }, valueRange = 1f..100f, colors = SliderDefaults.colors(thumbColor = Color(0xFF9C6FDE), activeTrackColor = Color(0xFF9C6FDE)))
                Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = { progress = 0; isPlaying = false }) { Icon(Icons.Default.Refresh, null, tint = Color.White) }
                    IconButton(onClick = { isPlaying = !isPlaying }) { Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color(0xFF9C6FDE), modifier = Modifier.size(36.dp)) }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = Color.White) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    var screen by remember { mutableStateOf("home") }
    var project by remember { mutableStateOf(Project()) }
    var projects by remember { mutableStateOf(listOf<Project>()) }
    var loading by remember { mutableStateOf(false) }
    var loadingMsg by remember { mutableStateOf("") }
    var showSaveDialog by remember { mutableStateOf(false) }
    var projectName by remember { mutableStateOf("") }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { projects = loadProjects(ctx) }
    if (showSaveDialog) {
        AlertDialog(onDismissRequest = { showSaveDialog = false }, containerColor = Color(0xFF1A1A2E), title = { Text("حفظ المشروع", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { OutlinedTextField(value = projectName, onValueChange = { projectName = it }, label = { Text("اسم المشروع", color = Color.Gray) }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF9C6FDE), unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White)) },
            confirmButton = { Button(onClick = { scope.launch { val saved = project.copy(name = projectName.ifEmpty { "مشروع جديد" }, savedAt = System.currentTimeMillis()); saveProject(ctx, saved); projects = loadProjects(ctx); showSaveDialog = false } }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C6FDE))) { Text("حفظ") } },
            dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text("إلغاء", color = Color.Gray) } })
    }
    Scaffold(
        containerColor = Color(0xFF0D0D1A),
        topBar = {
            TopAppBar(title = { Column { Text("Embroidery Studio Pro", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White); Text("v3.5 • 2026", fontSize = 10.sp, color = Color(0xFF9C6FDE)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D0D1A)),
                actions = { if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF9C6FDE), strokeWidth = 2.dp); if (project.stitches.isNotEmpty()) IconButton(onClick = { projectName = project.name; showSaveDialog = true }) { Icon(Icons.Default.Save, null, tint = Color(0xFF9C6FDE)) } })
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF1A1A2E)) {
                listOf(Triple("home", Icons.Default.Home, "الرئيسية"), Triple("projects", Icons.Default.List, "مشاريعي"), Triple("settings", Icons.Default.Settings, "الإعدادات")).forEach { (s, icon, label) ->
                    NavigationBarItem(icon = { Icon(icon, null) }, label = { Text(label, fontSize = 11.sp) }, selected = screen == s, onClick = { screen = s }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF9C6FDE), selectedTextColor = Color(0xFF9C6FDE), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray, indicatorColor = Color(0xFF2A2A3E)))
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when (screen) {
                "home" -> HomeScreen(project, loading, loadingMsg, { project = it }, { b, m -> loading = b; loadingMsg = m })
                "projects" -> ProjectsScreen(projects, { project = it; screen = "home" }, { p -> scope.launch { deleteProject(ctx, p.id); projects = loadProjects(ctx) } })
                "settings" -> SettingsScreen()
            }
        }
    }
}

@Composable
fun HomeScreen(project: Project, loading: Boolean, msg: String, onUpdate: (Project) -> Unit, onLoad: (Boolean, String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var density by remember { mutableFloatStateOf(4f) }
    var showImageZoom by remember { mutableStateOf(false) }
    var showExtractedZoom by remember { mutableStateOf(false) }
    var showPreviewFull by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { onUpdate(project.copy(imageUri = it)) } }

    if (showImageZoom && project.extracted != null) ZoomableImageDialog(project.extracted) { showImageZoom = false }
    if (showExtractedZoom && project.extracted != null) ZoomableImageDialog(project.extracted) { showExtractedZoom = false }
    if (showPreviewFull && project.stitches.isNotEmpty()) ZoomablePreviewDialog(project.stitches, project.colors) { showPreviewFull = false }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Box(Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(Color(0xFF1A1A3E), Color(0xFF2A1A3E)))).border(1.dp, Brush.linearGradient(listOf(Color(0xFF9C6FDE), Color(0xFF6FCFDE))), RoundedCornerShape(20.dp)).padding(16.dp)) {
                Column {
                    Text("ابدأ مشروع تطريز 🧵", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(when { project.stitches.isNotEmpty() -> "✅ ${project.stitches.size} غرزة • ${String.format("%.1f", project.widthMM)}×${String.format("%.1f", project.heightMM)} مم"; project.colors.isNotEmpty() -> "✅ ${project.colors.size} ألوان"; project.extracted != null -> "✅ تم الاستخراج"; project.imageUri != null -> "✅ تم تحميل الصورة"; else -> "6 خطوات للحصول على تصميم احترافي" }, color = Color(0xFF9C6FDE), fontSize = 12.sp)
                }
            }
        }
        if (loading) item { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2A1A)), border = BorderStroke(1.dp, Color(0xFF4FDE9C))) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(20.dp), color = Color(0xFF4FDE9C), strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)); Text(msg, color = Color(0xFF4FDE9C), fontSize = 13.sp) } } }
        if (project.imageUri != null) item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))) {
                Column(Modifier.padding(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) {
                            AsyncImage(project.imageUri, null, Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                            IconButton(onClick = { if (project.extracted != null) showImageZoom = true }, modifier = Modifier.align(Alignment.TopEnd)) { Icon(Icons.Default.ZoomIn, null, tint = Color.White) }
                        }
                        if (project.extracted != null) Box(Modifier.weight(1f)) {
                            Image(project.extracted.asImageBitmap(), null, Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                            IconButton(onClick = { showExtractedZoom = true }, modifier = Modifier.align(Alignment.TopEnd)) { Icon(Icons.Default.ZoomIn, null, tint = Color.White) }
                        }
                    }
                    if (project.extracted != null) Text("اضغط 🔍 للتكبير والتحريك", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        item { StepCard(1, "استيراد صورة", "اختر من المعرض", Color(0xFF9C6FDE), true) { picker.launch("image/*") } }
        item { StepCard(2, "استخراج النقشة", "فصل ذكي عن الخلفية", Color(0xFF4FBEAA), project.imageUri != null) { scope.launch { onLoad(true, "جاري الاستخراج..."); val bm = loadBmp(ctx, project.imageUri!!); if (bm != null) onUpdate(project.copy(extracted = extractPatternAuto(bm))); onLoad(false, "") } } }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)), border = BorderStroke(if (project.extracted != null) 1.dp else 0.5.dp, if (project.extracted != null) Color(0xFFDE9C6F) else Color(0xFF2A2A3E))) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(if (project.extracted != null) Color(0xFFDE9C6F).copy(0.2f) else Color(0xFF2A2A3E)), contentAlignment = Alignment.Center) { Text("3", color = if (project.extracted != null) Color(0xFFDE9C6F) else Color.Gray, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) { Text("تحليل الألوان", color = if (project.extracted != null) Color.White else Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.SemiBold); Text("اكتشاف الألوان — اختياري", color = Color.Gray, fontSize = 11.sp) }
                        if (project.extracted != null) Button(onClick = { scope.launch { onLoad(true, "جاري تحليل الألوان..."); onUpdate(project.copy(colors = getColors(project.extracted!!))); onLoad(false, "") } }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDE9C6F)), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)) { Text("تحليل", fontSize = 12.sp) }
                    }
                    if (project.colors.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(project.colors) { c -> Column(horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(32.dp).clip(CircleShape).background(Color(c)).border(1.dp, Color.White.copy(0.2f), CircleShape)); Text("#${Integer.toHexString(c).takeLast(6).uppercase()}", color = Color.Gray, fontSize = 8.sp) } }
                        }
                    }
                }
            }
        }
        item { StepCard(4, "تحويل لغرز", "Satin + Tatami + Run", Color(0xFFDE6F6F), project.extracted != null) {
            scope.launch { onLoad(true, "جاري توليد الغرز..."); val s = genStitchesPro(project.extracted!!, project.colors.ifEmpty { getColors(project.extracted!!) }, density.toInt()); val q = calcQuality(project.extracted!!, s); onUpdate(project.copy(stitches = s, widthMM = project.extracted.width * 0.1f, heightMM = project.extracted.height * 0.1f, quality = q)); onLoad(false, "") }
        }}
        if (project.stitches.isNotEmpty()) item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))) {
                Column(Modifier.padding(14.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()) { InfoItem("الغرز", "${project.stitches.size}"); InfoItem("العرض", "${String.format("%.1f", project.widthMM)}مم"); InfoItem("الارتفاع", "${String.format("%.1f", project.heightMM)}مم"); InfoItem("الألوان", "${project.colors.size}") }
                    project.quality?.let { q -> Spacer(Modifier.height(8.dp)); Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()) { InfoItem("التغطية", "${String.format("%.1f", q.coverage)}%"); InfoItem("Satin", "${project.stitches.count{it.type==StitchType.SATIN}}"); InfoItem("Tatami", "${project.stitches.count{it.type==StitchType.TATAMI}}"); InfoItem("Run", "${project.stitches.count{it.type==StitchType.RUN}}") } }
                    Spacer(Modifier.height(8.dp)); Text("كثافة الغرز: ${density.toInt()} مم", color = Color.Gray, fontSize = 12.sp)
                    Slider(value = density, onValueChange = { density = it }, valueRange = 2f..8f, steps = 5, colors = SliderDefaults.colors(thumbColor = Color(0xFF9C6FDE), activeTrackColor = Color(0xFF9C6FDE)))
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { StepCard(5, "معاينة كاملة", "تكبير + محاكاة ماكينة", Color(0xFF6F9CDE), project.stitches.isNotEmpty()) { showPreviewFull = true } }
            }
        }
        if (project.stitches.isNotEmpty()) item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))) {
                Canvas(Modifier.fillMaxWidth().height(200.dp).padding(8.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFF5F5F0)).clickable { showPreviewFull = true }) {
                    val mx = project.stitches.maxOfOrNull { it.x } ?: 1f; val my = project.stitches.maxOfOrNull { it.y } ?: 1f
                    val sc = minOf(size.width/mx, size.height/my) * 0.92f; val ox = (size.width-mx*sc)/2f; val oy = (size.height-my*sc)/2f
                    project.stitches.take(5000).forEach { st -> val c = if (st.colorIdx < project.colors.size) Color(project.colors[st.colorIdx]) else Color.Black; drawCircle(c, 1.5f, Offset(ox+st.x*sc, oy+st.y*sc)) }
                }
                Text("اضغط للمعاينة الكاملة مع محاكاة الماكينة ▶", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(8.dp))
            }
        }
        if (project.quality != null) item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2A1A)), border = BorderStroke(1.dp, Color(0xFF4FDE9C))) {
                Column(Modifier.padding(14.dp)) {
                    Text("✅ تقرير الجودة", color = Color(0xFF4FDE9C), fontSize = 14.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(6.dp))
                    Text("• إجمالي الغرز: ${project.quality.totalStitches}", color = Color.White, fontSize = 12.sp)
                    Text("• تغطية النقشة: ${String.format("%.1f", project.quality.coverage)}%", color = Color.White, fontSize = 12.sp)
                    Text("• الأبعاد: ${String.format("%.1f", project.quality.widthMM)}×${String.format("%.1f", project.quality.heightMM)} مم", color = Color.White, fontSize = 12.sp)
                    Text("• الملف جاهز للتصدير ✓", color = Color(0xFF4FDE9C), fontSize = 12.sp)
                }
            }
        }
        item { StepCard(6, "تصدير", "DST و PNG", Color(0xFF6FDE9C), project.stitches.isNotEmpty()) { scope.launch { onLoad(true, "جاري التصدير..."); exportProject(ctx, project); onLoad(false, "") } } }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
fun ProjectsScreen(projects: List<Project>, onOpen: (Project) -> Unit, onDelete: (Project) -> Unit) {
    if (projects.isEmpty()) {
        Box(Modifier.fillMaxSize().background(Color(0xFF0D0D1A)), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("🗂️", fontSize = 56.sp); Spacer(Modifier.height(12.dp)); Text("لا توجد مشاريع محفوظة", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text("احفظ مشروعك من الأيقونة أعلاه", color = Color.Gray, fontSize = 13.sp) } }
    } else {
        LazyColumn(Modifier.fillMaxSize().background(Color(0xFF0D0D1A)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("مشاريعي (${projects.size})", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            items(projects) { p ->
                Card(Modifier.fillMaxWidth().clickable { onOpen(p) }, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)), border = BorderStroke(0.5.dp, Color(0xFF2A2A3E))) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(p.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold); Text("${p.stitches.size} غرزة • ${p.colors.size} ألوان", color = Color.Gray, fontSize = 12.sp); Text("${String.format("%.1f", p.widthMM)}×${String.format("%.1f", p.heightMM)} مم", color = Color(0xFF9C6FDE), fontSize = 11.sp) }
                        IconButton(onClick = { onDelete(p) }) { Icon(Icons.Default.Delete, null, tint = Color(0xFFDE6F6F)) }
                    }
                }
            }
        }
    }
}

@Composable
fun StepCard(n: Int, title: String, sub: String, color: Color, on: Boolean, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(enabled = on) { onClick() }, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)), border = BorderStroke(if (on) 1.dp else 0.5.dp, if (on) color else Color(0xFF2A2A3E))) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(if (on) color.copy(alpha = 0.2f) else Color(0xFF2A2A3E)), contentAlignment = Alignment.Center) { Text("$n", color = if (on) color else Color.Gray, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text(title, color = if (on) Color.White else Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.SemiBold); Text(sub, color = Color.Gray, fontSize = 11.sp) }
            if (on) Icon(Icons.Default.PlayArrow, null, tint = color, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun InfoItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(value, color = Color(0xFF9C6FDE), fontSize = 15.sp, fontWeight = FontWeight.Bold); Text(label, color = Color.Gray, fontSize = 10.sp) }
}

@Composable
fun SettingsScreen() {
    LazyColumn(Modifier.fillMaxSize().background(Color(0xFF0D0D1A)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("الإعدادات", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        listOf(Triple("🌐", "اللغة", "العربية"), Triple("📐", "وحدة القياس", "ملليمتر"), Triple("⚡", "جودة الغرز", "احترافية"), Triple("🌙", "الوضع", "داكن"), Triple("📱", "الإصدار", "3.5 • 2026")).forEach { (e, t, v) ->
            item { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))) { Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Row(verticalAlignment = Alignment.CenterVertically) { Text(e, fontSize = 18.sp); Spacer(Modifier.width(10.dp)); Text(t, color = Color.White, fontSize = 14.sp) }; Text(v, color = Color(0xFF9C6FDE), fontSize = 13.sp) } } }
        }
    }
}

suspend fun loadBmp(ctx: android.content.Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) { try { ctx.contentResolver.openInputStream(uri)?.let { BitmapFactory.decodeStream(it) } } catch (e: Exception) { null } }

suspend fun extractPatternAuto(bm: Bitmap): Bitmap = withContext(Dispatchers.Default) {
    val w = bm.width; val h = bm.height; val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); val px = IntArray(w * h); bm.getPixels(px, 0, w, 0, 0, w, h)
    val edgeColors = mutableMapOf<Int, Int>()
    for (x in 0 until w) { listOf(px[x], px[(h-1)*w+x]).forEach { c -> val q = android.graphics.Color.rgb((android.graphics.Color.red(c)/16)*16,(android.graphics.Color.green(c)/16)*16,(android.graphics.Color.blue(c)/16)*16); edgeColors[q] = (edgeColors[q] ?: 0) + 1 } }
    for (y in 0 until h) { listOf(px[y*w], px[y*w+w-1]).forEach { c -> val q = android.graphics.Color.rgb((android.graphics.Color.red(c)/16)*16,(android.graphics.Color.green(c)/16)*16,(android.graphics.Color.blue(c)/16)*16); edgeColors[q] = (edgeColors[q] ?: 0) + 1 } }
    val bgColor = edgeColors.maxByOrNull { it.value }?.key ?: px[0]; val bgR = android.graphics.Color.red(bgColor); val bgG = android.graphics.Color.green(bgColor); val bgB = android.graphics.Color.blue(bgColor)
    for (i in px.indices) { val r = android.graphics.Color.red(px[i]); val g = android.graphics.Color.green(px[i]); val b = android.graphics.Color.blue(px[i]); px[i] = if (abs(r-bgR)+abs(g-bgG)+abs(b-bgB) > 45) px[i] else android.graphics.Color.TRANSPARENT }
    out.setPixels(px, 0, w, 0, 0, w, h); out
}

suspend fun getColors(bm: Bitmap): List<Int> = withContext(Dispatchers.Default) {
    val map = mutableMapOf<Int, Int>()
    for (y in 0 until bm.height step 4) for (x in 0 until bm.width step 4) { val p = bm.getPixel(x, y); if (android.graphics.Color.alpha(p) > 128) { val q = android.graphics.Color.rgb((android.graphics.Color.red(p)/32)*32,(android.graphics.Color.green(p)/32)*32,(android.graphics.Color.blue(p)/32)*32); map[q] = (map[q] ?: 0) + 1 } }
    map.entries.sortedByDescending { it.value }.take(8).map { it.key }
}

fun isEdgePixel(px: IntArray, w: Int, h: Int, x: Int, y: Int): Boolean { if (android.graphics.Color.alpha(px[y*w+x]) == 0) return false; return listOf(x-1 to y, x+1 to y, x to y-1, x to y+1).any { (nx, ny) -> nx < 0 || ny < 0 || nx >= w || ny >= h || android.graphics.Color.alpha(px[ny*w+nx]) == 0 } }

fun calcStitchAngle(px: IntArray, w: Int, h: Int, x: Int, y: Int): Float { var dx = 0f; var dy = 0f; for (ny in maxOf(0,y-2)..minOf(h-1,y+2)) for (nx in maxOf(0,x-2)..minOf(w-1,x+2)) { if (android.graphics.Color.alpha(px[ny*w+nx]) > 0) { dx += (nx-x).toFloat(); dy += (ny-y).toFloat() } }; return if (dx == 0f && dy == 0f) 0f else kotlin.math.atan2(dy, dx) + (Math.PI/2).toFloat() }

suspend fun genStitchesPro(bm: Bitmap, colors: List<Int>, density: Int = 4): List<Stitch> = withContext(Dispatchers.Default) {
    val list = mutableListOf<Stitch>(); val w = bm.width; val h = bm.height; val px = IntArray(w * h); bm.getPixels(px, 0, w, 0, 0, w, h); val step = density.coerceIn(2, 8)
    for (y in 0 until h step step) { val lr = (y/step)%2==0; val xs = if (lr) 0 until w step step else w-1 downTo 0 step step
        for (x in xs) { val p = px[y*w+x]; if (android.graphics.Color.alpha(p) > 128) { var md = Int.MAX_VALUE; var ni = 0; colors.forEachIndexed { i, c -> val d = (android.graphics.Color.red(p)-android.graphics.Color.red(c)).let{it*it}+(android.graphics.Color.green(p)-android.graphics.Color.green(c)).let{it*it}+(android.graphics.Color.blue(p)-android.graphics.Color.blue(c)).let{it*it}; if (d < md) { md = d; ni = i } }; val isEdge = isEdgePixel(px,w,h,x,y); val type = when { isEdge && step<=4 -> StitchType.SATIN; isEdge -> StitchType.RUN; else -> StitchType.TATAMI }; val angle = if (type==StitchType.SATIN) calcStitchAngle(px,w,h,x,y) else 0f; list.add(Stitch(x.toFloat(),y.toFloat(),ni,type,angle)) } }
    }; list
}

fun calcQuality(bm: Bitmap, stitches: List<Stitch>): QualityReport { val total = stitches.size; val w = bm.width; val h = bm.height; val px = IntArray(w*h); bm.getPixels(px,0,w,0,0,w,h); val totalSolid = px.count { android.graphics.Color.alpha(it) > 128 }; val covered = stitches.map { it.y.toInt()*w+it.x.toInt() }.toSet().size; val coverage = if (totalSolid>0) (covered.toFloat()/totalSolid*100f).coerceAtMost(100f) else 0f; val outOfBounds = stitches.count { it.x<0||it.y<0||it.x>=w||it.y>=h }.toFloat()/total*100f; return QualityReport(total, coverage, outOfBounds, stitches.map{it.colorIdx}.toSet().size, w*0.1f, h*0.1f) }

suspend fun exportProject(ctx: android.content.Context, p: Project) = withContext(Dispatchers.IO) {
    val dir = ctx.getExternalFilesDir(null) ?: return@withContext
    p.extracted?.let { bm -> FileOutputStream(File(dir, "${p.name}.png")).use { bm.compress(Bitmap.CompressFormat.PNG, 100, it) } }
    if (p.stitches.isNotEmpty()) { FileOutputStream(File(dir, "${p.name}.dst")).use { fos -> val hdr = ByteArray(512){0x20}; val lbl = "LA:${p.name}".toByteArray(); System.arraycopy(lbl,0,hdr,0,minOf(lbl.size,16)); fos.write(hdr); var lx=0; var ly=0; p.stitches.forEach { s -> val dx=(s.x-lx).toInt().coerceIn(-121,121); val dy=(s.y-ly).toInt().coerceIn(-121,121); lx+=dx; ly+=dy; fos.write(byteArrayOf(dx.toByte(),dy.toByte(),0x03)) }; fos.write(byteArrayOf(0x00,0x00,0xF3.toByte())) } }
}

suspend fun saveProject(ctx: android.content.Context, p: Project) = withContext(Dispatchers.IO) { val dir = File(ctx.filesDir,"projects"); dir.mkdirs(); val json = JSONObject().apply { put("id",p.id); put("name",p.name); put("savedAt",p.savedAt); put("widthMM",p.widthMM); put("heightMM",p.heightMM); put("stitchCount",p.stitches.size); put("colorCount",p.colors.size); val ca=JSONArray(); p.colors.forEach{ca.put(it)}; put("colors",ca) }; File(dir,"${p.id}.json").writeText(json.toString()); p.extracted?.let { bm -> FileOutputStream(File(dir,"${p.id}.png")).use{bm.compress(Bitmap.CompressFormat.PNG,100,it)} } }

suspend fun loadProjects(ctx: android.content.Context): List<Project> = withContext(Dispatchers.IO) { val dir=File(ctx.filesDir,"projects"); if (!dir.exists()) return@withContext emptyList(); dir.listFiles{f->f.extension=="json"}?.mapNotNull{f -> try { val json=JSONObject(f.readText()); val colors=mutableListOf<Int>(); val arr=json.getJSONArray("colors"); for(i in 0 until arr.length()) colors.add(arr.getInt(i)); val imgFile=File(dir,"${json.getString("id")}.png"); val bm=if(imgFile.exists()) BitmapFactory.decodeFile(imgFile.absolutePath) else null; Project(id=json.getString("id"),name=json.getString("name"),extracted=bm,colors=colors,widthMM=json.getDouble("widthMM").toFloat(),heightMM=json.getDouble("heightMM").toFloat(),savedAt=json.getLong("savedAt")) } catch(e:Exception){null} }?.sortedByDescending{it.savedAt} ?: emptyList() }

suspend fun deleteProject(ctx: android.content.Context, id: String) = withContext(Dispatchers.IO) { val dir=File(ctx.filesDir,"projects"); File(dir,"$id.json").delete(); File(dir,"$id.png").delete() }
