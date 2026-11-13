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
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import kotlin.math.sqrt

enum class StitchType { RUN, SATIN, TATAMI }
data class Stitch(val x: Float, val y: Float, val colorIdx: Int, val color: Int = 0, val type: StitchType = StitchType.TATAMI, val angle: Float = 0f)
data class QualityReport(val totalStitches: Int, val coverage: Float, val outOfBounds: Float, val colorCount: Int, val widthMM: Float, val heightMM: Float, val satinCount: Int = 0, val runCount: Int = 0, val tatamiCount: Int = 0, val colorChanges: Int = 0)
data class Project(val id: String = System.currentTimeMillis().toString(), val name: String = "مشروع جديد", val imageUri: Uri? = null, val extracted: Bitmap? = null, val colors: List<Int> = emptyList(), val stitches: List<Stitch> = emptyList(), val widthMM: Float = 0f, val heightMM: Float = 0f, val savedAt: Long = 0L, val quality: QualityReport? = null)

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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageCompareDialog(original: Bitmap?, extracted: Bitmap?, onDismiss: () -> Unit) {
    val pages = listOfNotNull(original?.let{"الأصل" to it}, extracted?.let{"المستخرجة" to it})
    if(pages.isEmpty()){onDismiss();return}
    val pagerState = rememberPagerState{pages.size}
    Dialog(onDismissRequest=onDismiss, properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(state=pagerState, modifier=Modifier.fillMaxSize()) { page ->
                var scale by remember{mutableFloatStateOf(1f)}; var offset by remember{mutableStateOf(Offset.Zero)}
                Box(Modifier.fillMaxSize().pointerInput(Unit){detectTransformGestures{_,pan,zoom,_->scale=(scale*zoom).coerceIn(1f,10f);offset+=pan}}) {
                    Image(pages[page].second.asImageBitmap(), null, modifier=Modifier.fillMaxSize().graphicsLayer(scaleX=scale,scaleY=scale,translationX=offset.x,translationY=offset.y), contentScale=ContentScale.Fit)
                }
            }
            Row(Modifier.align(Alignment.TopCenter).padding(top=16.dp), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                pages.forEachIndexed{i,(label,_)->val sel=pagerState.currentPage==i; Box(Modifier.background(if(sel)Color(0xFF9C6FDE) else Color(0x88FFFFFF),RoundedCornerShape(12.dp)).padding(horizontal=12.dp,vertical=4.dp)){Text(label,color=Color.White,fontSize=12.sp,fontWeight=if(sel)FontWeight.Bold else FontWeight.Normal)}}
            }
            Text("← اسحب للمقارنة →", color=Color.White.copy(0.5f), fontSize=11.sp, modifier=Modifier.align(Alignment.BottomCenter).padding(bottom=24.dp))
            IconButton(onClick=onDismiss, modifier=Modifier.align(Alignment.TopEnd).padding(16.dp)){Icon(Icons.Default.Close,null,tint=Color.White,modifier=Modifier.size(28.dp))}
        }
    }
}

@Composable
fun ZoomablePreviewDialog(stitches: List<Stitch>, colors: List<Int>, onDismiss: () -> Unit) {
    var scale by remember{mutableFloatStateOf(1f)}; var offset by remember{mutableStateOf(Offset.Zero)}
    var isPlaying by remember{mutableStateOf(false)}; var progress by remember{mutableIntStateOf(100)}
    LaunchedEffect(isPlaying){if(isPlaying){if(progress>=100)progress=0;while(isPlaying&&progress<100){progress+=1;delay(20)};isPlaying=false}}
    val mx=remember(stitches){stitches.maxOfOrNull{it.x}?:1f}; val my=remember(stitches){stitches.maxOfOrNull{it.y}?:1f}
    Dialog(onDismissRequest=onDismiss, properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Box(Modifier.fillMaxSize().background(Color(0xFF0A0A14))) {
            val showCount=(stitches.size*progress/100).coerceAtLeast(1)
            Canvas(Modifier.fillMaxSize().pointerInput(Unit){detectTransformGestures{_,pan,zoom,_->scale=(scale*zoom).coerceIn(0.3f,15f);offset+=pan}}.graphicsLayer(scaleX=scale,scaleY=scale,translationX=offset.x,translationY=offset.y)) {
                val sc=minOf(size.width/mx,size.height/my)*0.88f; val ox=(size.width-mx*sc)/2f; val oy=(size.height-my*sc)/2f
                stitches.take(showCount).forEachIndexed{idx,st->
                    val c=if(st.color!=0)Color(st.color) else if(st.colorIdx<colors.size)Color(colors[st.colorIdx]) else Color.Black
                    val sx=ox+st.x*sc; val sy=oy+st.y*sc
                    when(st.type){
                        StitchType.SATIN->{val len=10f;drawLine(c,Offset(sx,sy),Offset(sx+cos(st.angle)*len,sy+sin(st.angle)*len),strokeWidth=3.5f,cap=StrokeCap.Round)}
                        StitchType.RUN->{if(idx>0&&stitches[idx-1].colorIdx==st.colorIdx)drawLine(c,Offset(ox+stitches[idx-1].x*sc,oy+stitches[idx-1].y*sc),Offset(sx,sy),strokeWidth=1.8f,cap=StrokeCap.Round)}
                        StitchType.TATAMI->drawCircle(c,2.2f,Offset(sx,sy))
                    }
                }
            }
            Column(Modifier.align(Alignment.BottomCenter).background(Color(0xEE0A0A14)).padding(12.dp)) {
                Text("$progress%  •  $showCount / ${stitches.size} غرزة",color=Color.White,fontSize=12.sp,modifier=Modifier.align(Alignment.CenterHorizontally))
                Slider(value=progress.toFloat(),onValueChange={progress=it.toInt();isPlaying=false},valueRange=1f..100f,colors=SliderDefaults.colors(thumbColor=Color(0xFF9C6FDE),activeTrackColor=Color(0xFF9C6FDE)))
                Row(horizontalArrangement=Arrangement.SpaceAround,modifier=Modifier.fillMaxWidth()){
                    IconButton(onClick={progress=0;isPlaying=false}){Icon(Icons.Default.Refresh,null,tint=Color.White)}
                    IconButton(onClick={isPlaying=!isPlaying}){Icon(if(isPlaying)Icons.Default.Pause else Icons.Default.PlayArrow,null,tint=Color(0xFF9C6FDE),modifier=Modifier.size(42.dp))}
                    IconButton(onClick=onDismiss){Icon(Icons.Default.Close,null,tint=Color.White)}
                }
            }
        }
    }
}

@Composable
fun StitchMapDialog(stitches: List<Stitch>, colors: List<Int>, onDismiss: () -> Unit) {
    var scale by remember{mutableFloatStateOf(1f)}; var offset by remember{mutableStateOf(Offset.Zero)}
    val mx=remember(stitches){stitches.maxOfOrNull{it.x}?:1f}; val my=remember(stitches){stitches.maxOfOrNull{it.y}?:1f}
    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Box(Modifier.fillMaxSize().background(Color(0xFF0A0A14))) {
            Canvas(Modifier.fillMaxSize().pointerInput(Unit){detectTransformGestures{_,pan,zoom,_->scale=(scale*zoom).coerceIn(0.3f,15f);offset+=pan}}.graphicsLayer(scaleX=scale,scaleY=scale,translationX=offset.x,translationY=offset.y)) {
                val sc=minOf(size.width/mx,size.height/my)*0.88f; val ox=(size.width-mx*sc)/2f; val oy=(size.height-my*sc)/2f
                stitches.forEachIndexed{idx,st->
                    val c=when(st.type){StitchType.SATIN->Color(0xFFFF6B6B);StitchType.RUN->Color(0xFF6BFF9E);StitchType.TATAMI->Color(0xFF6B9EFF)}
                    val sx=ox+st.x*sc; val sy=oy+st.y*sc
                    when(st.type){
                        StitchType.SATIN->{val len=10f;drawLine(c,Offset(sx,sy),Offset(sx+cos(st.angle)*len,sy+sin(st.angle)*len),strokeWidth=3.5f,cap=StrokeCap.Round)}
                        StitchType.RUN->{if(idx>0&&stitches[idx-1].colorIdx==st.colorIdx)drawLine(c,Offset(ox+stitches[idx-1].x*sc,oy+stitches[idx-1].y*sc),Offset(sx,sy),strokeWidth=2f,cap=StrokeCap.Round)}
                        StitchType.TATAMI->{if(idx>0){val prev=project.stitches[idx-1];if(prev.colorIdx==st.colorIdx && kotlin.math.hypot((prev.x-st.x).toDouble(),(prev.y-st.y).toDouble())<45.0)drawLine(c,Offset(ox+prev.x*sc,oy+prev.y*sc),Offset(sx,sy),strokeWidth=1.6f,cap=StrokeCap.Round)}}
                    }
                }
            }
            Column(Modifier.align(Alignment.TopStart).padding(12.dp).background(Color(0xCC000000),RoundedCornerShape(10.dp)).padding(10.dp)) {
                Text("خريطة الغرز",color=Color.White,fontSize=13.sp,fontWeight=FontWeight.Bold); Spacer(Modifier.height(4.dp))
                listOf(Triple(Color(0xFFFF6B6B),"Satin",stitches.count{it.type==StitchType.SATIN}),Triple(Color(0xFF6BFF9E),"Run",stitches.count{it.type==StitchType.RUN}),Triple(Color(0xFF6B9EFF),"Tatami",stitches.count{it.type==StitchType.TATAMI})).forEach{(c,n,cnt)->Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.padding(vertical=2.dp)){Box(Modifier.size(10.dp).background(c,CircleShape));Spacer(Modifier.width(6.dp));Text("$n: $cnt",color=Color.White,fontSize=11.sp)}}
            }
            IconButton(onClick=onDismiss,modifier=Modifier.align(Alignment.TopEnd).padding(12.dp)){Icon(Icons.Default.Close,null,tint=Color.White,modifier=Modifier.size(28.dp))}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    var screen by remember{mutableStateOf("home")}; var project by remember{mutableStateOf(Project())}
    var projects by remember{mutableStateOf(listOf<Project>())}; var loading by remember{mutableStateOf(false)}
    var loadingMsg by remember{mutableStateOf("")}; var showSaveDialog by remember{mutableStateOf(false)}
    var projectName by remember{mutableStateOf("")}
    val ctx=LocalContext.current; val scope=rememberCoroutineScope()
    LaunchedEffect(Unit){projects=loadProjects(ctx)}
    if(showSaveDialog){AlertDialog(onDismissRequest={showSaveDialog=false},containerColor=Color(0xFF1A1A2E),title={Text("حفظ المشروع",color=Color.White,fontWeight=FontWeight.Bold)},text={OutlinedTextField(value=projectName,onValueChange={projectName=it},label={Text("اسم المشروع",color=Color.Gray)},colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=Color(0xFF9C6FDE),unfocusedBorderColor=Color.Gray,focusedTextColor=Color.White,unfocusedTextColor=Color.White))},confirmButton={Button(onClick={scope.launch{val saved=project.copy(name=projectName.ifEmpty{"مشروع جديد"},savedAt=System.currentTimeMillis());saveProject(ctx,saved);projects=loadProjects(ctx);showSaveDialog=false}},colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF9C6FDE))){Text("حفظ")}},dismissButton={TextButton(onClick={showSaveDialog=false}){Text("إلغاء",color=Color.Gray)}})}
    Scaffold(containerColor=Color(0xFF0D0D1A),
        topBar={TopAppBar(title={Column{Text("Embroidery Studio Pro",fontSize=16.sp,fontWeight=FontWeight.Bold,color=Color.White);Text("v3.8 • 2026",fontSize=10.sp,color=Color(0xFF9C6FDE))}},colors=TopAppBarDefaults.topAppBarColors(containerColor=Color(0xFF0D0D1A)),actions={if(loading)CircularProgressIndicator(modifier=Modifier.size(20.dp),color=Color(0xFF9C6FDE),strokeWidth=2.dp);if(project.stitches.isNotEmpty())IconButton(onClick={projectName=project.name;showSaveDialog=true}){Icon(Icons.Default.Save,null,tint=Color(0xFF9C6FDE))}})},
        bottomBar={NavigationBar(containerColor=Color(0xFF1A1A2E)){listOf(Triple("home",Icons.Default.Home,"الرئيسية"),Triple("projects",Icons.Default.List,"مشاريعي"),Triple("settings",Icons.Default.Settings,"الإعدادات")).forEach{(s,icon,label)->NavigationBarItem(icon={Icon(icon,null)},label={Text(label,fontSize=11.sp)},selected=screen==s,onClick={screen=s},colors=NavigationBarItemDefaults.colors(selectedIconColor=Color(0xFF9C6FDE),selectedTextColor=Color(0xFF9C6FDE),unselectedIconColor=Color.Gray,unselectedTextColor=Color.Gray,indicatorColor=Color(0xFF2A2A3E)))}}}
    ){pad->Box(Modifier.padding(pad)){when(screen){"home"->HomeScreen(project,loading,loadingMsg,{project=it},{b,m->loading=b;loadingMsg=m});"projects"->ProjectsScreen(projects,{project=it;screen="home"},{p->scope.launch{deleteProject(ctx,p.id);projects=loadProjects(ctx)}});"settings"->SettingsScreen()}}}
}

@Composable
fun HomeScreen(project: Project, loading: Boolean, msg: String, onUpdate: (Project)->Unit, onLoad: (Boolean,String)->Unit) {
    val ctx=LocalContext.current; val scope=rememberCoroutineScope()
    var density by remember{mutableFloatStateOf(4f)}
    var showCompare by remember{mutableStateOf(false)}
    var showPreview by remember{mutableStateOf(false)}
    var showMap by remember{mutableStateOf(false)}
    var colorsExpanded by remember{mutableStateOf(false)}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri->uri?.let{onUpdate(Project(name=project.name,imageUri=it))}}
    if(showCompare) ImageCompareDialog(project.extracted,project.extracted){showCompare=false}
    if(showPreview&&project.stitches.isNotEmpty()) ZoomablePreviewDialog(project.stitches,project.colors){showPreview=false}
    if(showMap&&project.stitches.isNotEmpty()) StitchMapDialog(project.stitches,project.colors){showMap=false}
    LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item{Box(Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(Color(0xFF1A1A3E),Color(0xFF2A1A3E)))).border(1.dp,Brush.linearGradient(listOf(Color(0xFF9C6FDE),Color(0xFF6FCFDE))),RoundedCornerShape(20.dp)).padding(16.dp)){Column{Text("ابدأ مشروع تطريز 🧵",color=Color.White,fontSize=18.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(4.dp));Text(when{project.stitches.isNotEmpty()->"✅ ${project.stitches.size} غرزة • ${String.format("%.1f",project.widthMM)}×${String.format("%.1f",project.heightMM)} مم";project.colors.isNotEmpty()->"✅ ${project.colors.size} ألوان";project.extracted!=null->"✅ تم الاستخراج";project.imageUri!=null->"✅ تم تحميل الصورة";else->"6 خطوات للحصول على تصميم احترافي"},color=Color(0xFF9C6FDE),fontSize=12.sp)}}}
        if(loading) item{Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFF1A2A1A)),border=BorderStroke(1.dp,Color(0xFF4FDE9C))){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){CircularProgressIndicator(Modifier.size(20.dp),color=Color(0xFF4FDE9C),strokeWidth=2.dp);Spacer(Modifier.width(10.dp));Text(msg,color=Color(0xFF4FDE9C),fontSize=13.sp)}}}
        if(project.imageUri!=null) item{
            Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFF1A1A2E))){
                Column(Modifier.padding(10.dp)){
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        Box(Modifier.weight(1f)){AsyncImage(project.imageUri,null,Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(10.dp)),contentScale=ContentScale.Fit);IconButton(onClick={showCompare=true},modifier=Modifier.align(Alignment.TopEnd).background(Color(0x88000000),CircleShape)){Icon(Icons.Default.ZoomIn,null,tint=Color.White)}}
                        if(project.extracted!=null) Box(Modifier.weight(1f)){Image(project.extracted.asImageBitmap(),null,Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(10.dp)),contentScale=ContentScale.Fit);IconButton(onClick={showCompare=true},modifier=Modifier.align(Alignment.TopEnd).background(Color(0x88000000),CircleShape)){Icon(Icons.Default.ZoomIn,null,tint=Color.White)}}
                    }
                    if(project.extracted!=null) Text("← اضغط 🔍 للمقارنة والتكبير →",color=Color.Gray,fontSize=10.sp,modifier=Modifier.padding(top=4.dp).align(Alignment.CenterHorizontally))
                }
            }
        }
        item{StepCard(1,"استيراد صورة","اختر من المعرض",Color(0xFF9C6FDE),true){picker.launch("image/*")}}
        item{StepCard(2,"استخراج النقشة","فصل ذكي متقدم عن الخلفية",Color(0xFF4FBEAA),project.imageUri!=null){scope.launch{onLoad(true,"جاري الاستخراج المتقدم...");val bm=loadBmp(ctx,project.imageUri!!);if(bm!=null)onUpdate(project.copy(extracted=extractPatternAuto(bm),colors=emptyList(),stitches=emptyList(),quality=null));onLoad(false,"")}}}
        item{Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFF1A1A2E)),border=BorderStroke(if(project.extracted!=null)1.dp else 0.5.dp,if(project.extracted!=null)Color(0xFFDE9C6F) else Color(0xFF2A2A3E))){Column(Modifier.padding(14.dp)){Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(if(project.extracted!=null)Color(0xFFDE9C6F).copy(0.2f) else Color(0xFF2A2A3E)),contentAlignment=Alignment.Center){Text("3",color=if(project.extracted!=null)Color(0xFFDE9C6F) else Color.Gray,fontSize=17.sp,fontWeight=FontWeight.Bold)};Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text("تحليل الألوان",color=if(project.extracted!=null)Color.White else Color.Gray,fontSize=14.sp,fontWeight=FontWeight.SemiBold);Text("32 لون — اختياري",color=Color.Gray,fontSize=11.sp)};if(project.extracted!=null)Button(onClick={scope.launch{onLoad(true,"جاري تحليل الألوان...");onUpdate(project.copy(colors=getColors(project.extracted!!),stitches=emptyList(),quality=null));onLoad(false,"")}},colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFDE9C6F)),contentPadding=androidx.compose.foundation.layout.PaddingValues(horizontal=12.dp,vertical=6.dp)){Text("تحليل",fontSize=12.sp)}};if(project.colors.isNotEmpty()){Spacer(Modifier.height(8.dp));LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){items(project.colors){c->Column(horizontalAlignment=Alignment.CenterHorizontally){Box(Modifier.size(28.dp).clip(CircleShape).background(Color(c)).border(1.dp,Color.White.copy(0.2f),CircleShape));Text("#${Integer.toHexString(c).takeLast(6).uppercase()}",color=Color.Gray,fontSize=7.sp)}}}}}}
        }
        item{StepCard(4,"تحويل لغرز احترافية","Satin + Tatami + Run — دقة عالية",Color(0xFFDE6F6F),project.extracted!=null){scope.launch{onLoad(true,"جاري توليد الغرز الاحترافية...");val colors=project.colors.ifEmpty{getColors(project.extracted!!)};val s=genStitchesPro(project.extracted!!,colors,density.toInt());val q=calcQuality(project.extracted!!,s);onUpdate(project.copy(stitches=s,colors=colors,widthMM=project.extracted.width*0.1f,heightMM=project.extracted.height*0.1f,quality=q));onLoad(false,"")}}}
        if(project.stitches.isNotEmpty()) item{Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFF1A1A2E))){Column(Modifier.padding(14.dp)){Row(horizontalArrangement=Arrangement.SpaceAround,modifier=Modifier.fillMaxWidth()){InfoItem("الغرز","${project.stitches.size}");InfoItem("العرض","${String.format("%.1f",project.widthMM)}مم");InfoItem("الارتفاع","${String.format("%.1f",project.heightMM)}مم");InfoItem("الألوان","${project.colors.size}")};project.quality?.let{q->Spacer(Modifier.height(6.dp));Row(horizontalArrangement=Arrangement.SpaceAround,modifier=Modifier.fillMaxWidth()){InfoItem("التغطية","${String.format("%.1f",q.coverage)}%");InfoItem("Satin","${q.satinCount}");InfoItem("Tatami","${q.tatamiCount}");InfoItem("Run","${q.runCount}")}};Spacer(Modifier.height(8.dp));Text("كثافة الغرز: ${density.toInt()} مم",color=Color.Gray,fontSize=12.sp);Slider(value=density,onValueChange={density=it},valueRange=2f..8f,steps=5,colors=SliderDefaults.colors(thumbColor=Color(0xFF9C6FDE),activeTrackColor=Color(0xFF9C6FDE)))}}}
        if(project.colors.isNotEmpty()&&project.stitches.isNotEmpty()) item{Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFF1A1A2E))){Column(Modifier.padding(14.dp)){Text(if(colorsExpanded) "ترتيب الألوان ▲" else "ترتيب الألوان ▼",color=Color.White,fontSize=13.sp,fontWeight=FontWeight.Bold,modifier=Modifier.fillMaxWidth().clickable{colorsExpanded=!colorsExpanded});Spacer(Modifier.height(8.dp));if(colorsExpanded)project.colors.mapIndexed{i,c->Triple(i+1,c,project.stitches.count{it.colorIdx==i})}.sortedByDescending{it.third}.forEach{(num,color,count)->Row(Modifier.fillMaxWidth().padding(vertical=3.dp),verticalAlignment=Alignment.CenterVertically){Text("$num",color=Color.Gray,fontSize=11.sp,modifier=Modifier.width(20.dp));Box(Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(Color(color)).border(1.dp,Color.White.copy(0.2f),RoundedCornerShape(6.dp)));Spacer(Modifier.width(8.dp));Column(Modifier.weight(1f)){Text("${embroideryColorName(color)} • #${Integer.toHexString(color).takeLast(6).uppercase()}",color=Color.White,fontSize=11.sp);Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF2A2A3E))){val ratio=if(project.stitches.isNotEmpty())count.toFloat()/project.stitches.size else 0f;Box(Modifier.fillMaxWidth(ratio).height(4.dp).background(Color(color)))}};Spacer(Modifier.width(8.dp));Text("$count",color=Color(0xFF9C6FDE),fontSize=11.sp)}}}}}
        if(project.quality!=null) item{Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFF1A2A1A)),border=BorderStroke(1.dp,Color(0xFF4FDE9C))){Column(Modifier.padding(14.dp)){Text("📊 تقرير الجودة",color=Color(0xFF4FDE9C),fontSize=14.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(6.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column{Text("• الغرز: ${project.quality.totalStitches}",color=Color.White,fontSize=12.sp);Text("• Satin: ${project.quality.satinCount}",color=Color.White,fontSize=12.sp);Text("• Tatami: ${project.quality.tatamiCount}",color=Color.White,fontSize=12.sp);Text("• Run: ${project.quality.runCount}",color=Color.White,fontSize=12.sp)};Column{Text("• التغطية: ${String.format("%.1f",project.quality.coverage)}%",color=Color.White,fontSize=12.sp);Text("• تغيير ألوان: ${project.quality.colorChanges}",color=Color.White,fontSize=12.sp);Text("• يلزم فحص ملف DST على عارض غرز",color=Color(0xFFDE9C6F),fontSize=12.sp)}}}}}
        if(project.stitches.isNotEmpty()) item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Box(Modifier.weight(1f)){StepCard(5,"معاينة كاملة","محاكاة ماكينة ▶",Color(0xFF6F9CDE),true){showPreview=true}};Box(Modifier.weight(1f)){StepCard(0,"خريطة الغرز","Satin/Tatami/Run",Color(0xFFDE9C6F),true){showMap=true}}}}
        if(project.stitches.isNotEmpty()) item{Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFF1A1A2E))){Canvas(Modifier.fillMaxWidth().height(220.dp).padding(8.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF0A0A14)).clickable{showPreview=true}){val mx=project.stitches.maxOfOrNull{it.x}?:1f;val my=project.stitches.maxOfOrNull{it.y}?:1f;val sc=minOf(size.width/mx,size.height/my)*0.88f;val ox=(size.width-mx*sc)/2f;val oy=(size.height-my*sc)/2f;project.stitches.take(10000).forEachIndexed{idx,st->val c=if(st.color!=0)Color(st.color) else if(st.colorIdx<project.colors.size)Color(project.colors[st.colorIdx]) else Color.Black;val sx=ox+st.x*sc;val sy=oy+st.y*sc;when(st.type){StitchType.SATIN->{val len=8f;drawLine(c,Offset(sx,sy),Offset(sx+cos(st.angle)*len,sy+sin(st.angle)*len),strokeWidth=3f,cap=StrokeCap.Round)};StitchType.RUN->{if(idx>0&&project.stitches[idx-1].colorIdx==st.colorIdx)drawLine(c,Offset(ox+project.stitches[idx-1].x*sc,oy+project.stitches[idx-1].y*sc),Offset(sx,sy),strokeWidth=1.5f,cap=StrokeCap.Round)};StitchType.TATAMI->{if(idx>0){val prev=project.stitches[idx-1];if(prev.colorIdx==st.colorIdx && kotlin.math.hypot((prev.x-st.x).toDouble(),(prev.y-st.y).toDouble())<45.0)drawLine(c,Offset(ox+prev.x*sc,oy+prev.y*sc),Offset(sx,sy),strokeWidth=1.6f,cap=StrokeCap.Round)}}}}};Text("اضغط للمعاينة الكاملة مع محاكاة الماكينة ▶",color=Color.Gray,fontSize=11.sp,modifier=Modifier.padding(8.dp))}}
        item{StepCard(6,"تصدير DST و PNG","جاهز للماكينة",Color(0xFF6FDE9C),project.stitches.isNotEmpty()){scope.launch{onLoad(true,"جاري التصدير...");exportProject(ctx,project);onLoad(false,"")}}}
        item{Spacer(Modifier.height(16.dp))}
    }
}

@Composable
fun ProjectsScreen(projects: List<Project>, onOpen: (Project)->Unit, onDelete: (Project)->Unit) {
    if(projects.isEmpty()){Box(Modifier.fillMaxSize().background(Color(0xFF0D0D1A)),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally){Text("🗂️",fontSize=56.sp);Spacer(Modifier.height(12.dp));Text("لا توجد مشاريع محفوظة",color=Color.White,fontSize=18.sp,fontWeight=FontWeight.Bold);Text("احفظ مشروعك من الأيقونة أعلاه",color=Color.Gray,fontSize=13.sp)}}}
    else{LazyColumn(Modifier.fillMaxSize().background(Color(0xFF0D0D1A)).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){item{Text("مشاريعي (${projects.size})",color=Color.White,fontSize=20.sp,fontWeight=FontWeight.Bold)};items(projects){p->Card(Modifier.fillMaxWidth().clickable{onOpen(p)},shape=RoundedCornerShape(14.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFF1A1A2E)),border=BorderStroke(0.5.dp,Color(0xFF2A2A3E))){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){if(p.extracted!=null)Image(p.extracted.asImageBitmap(),null,Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)),contentScale=ContentScale.Crop) else Box(Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF2A2A3E)),contentAlignment=Alignment.Center){Text("🧵",fontSize=24.sp)};Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(p.name,color=Color.White,fontSize=15.sp,fontWeight=FontWeight.Bold);Text("${p.stitches.size} غرزة • ${p.colors.size} ألوان",color=Color.Gray,fontSize=12.sp);Text("${String.format("%.1f",p.widthMM)}×${String.format("%.1f",p.heightMM)} مم",color=Color(0xFF9C6FDE),fontSize=11.sp)};IconButton(onClick={onDelete(p)}){Icon(Icons.Default.Delete,null,tint=Color(0xFFDE6F6F))}}}}}}
}

@Composable
fun StepCard(n: Int, title: String, sub: String, color: Color, on: Boolean, onClick: ()->Unit) {
    Card(Modifier.fillMaxWidth().clickable(enabled=on){onClick()},shape=RoundedCornerShape(14.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFF1A1A2E)),border=BorderStroke(if(on)1.dp else 0.5.dp,if(on)color else Color(0xFF2A2A3E))){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(if(on)color.copy(alpha=0.2f) else Color(0xFF2A2A3E)),contentAlignment=Alignment.Center){Text(if(n>0)"$n" else "🗺",color=if(on)color else Color.Gray,fontSize=if(n>0)17.sp else 20.sp,fontWeight=FontWeight.Bold)};Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(title,color=if(on)Color.White else Color.Gray,fontSize=14.sp,fontWeight=FontWeight.SemiBold);Text(sub,color=Color.Gray,fontSize=11.sp)};if(on)Icon(Icons.Default.PlayArrow,null,tint=color,modifier=Modifier.size(18.dp))}}
}

@Composable
fun InfoItem(label: String, value: String) {
    Column(horizontalAlignment=Alignment.CenterHorizontally){Text(value,color=Color(0xFF9C6FDE),fontSize=15.sp,fontWeight=FontWeight.Bold);Text(label,color=Color.Gray,fontSize=10.sp)}
}

@Composable
fun SettingsScreen() {
    LazyColumn(Modifier.fillMaxSize().background(Color(0xFF0D0D1A)).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        item{Text("الإعدادات",color=Color.White,fontSize=20.sp,fontWeight=FontWeight.Bold)}
        listOf(Triple("🌐","اللغة","العربية"),Triple("📐","وحدة القياس","ملليمتر"),Triple("⚡","جودة الغرز","احترافية — 32 لون"),Triple("🌙","الوضع","داكن"),Triple("📱","الإصدار","3.8 • 2026")).forEach{(e,t,v)->item{Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFF1A1A2E))){Row(Modifier.padding(14.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Row(verticalAlignment=Alignment.CenterVertically){Text(e,fontSize=18.sp);Spacer(Modifier.width(10.dp));Text(t,color=Color.White,fontSize=14.sp)};Text(v,color=Color(0xFF9C6FDE),fontSize=13.sp)}}}}
    }
}

suspend fun loadBmp(ctx: android.content.Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO){try{ctx.contentResolver.openInputStream(uri)?.let{BitmapFactory.decodeStream(it)}}catch(e:Exception){null}}

suspend fun extractPatternAuto(bm: Bitmap): Bitmap = withContext(Dispatchers.Default) {
    // Preserve original RGB and internal holes. Remove only background connected
    // to the image border; unlike global color deletion, this protects motifs.
    val w = bm.width
    val h = bm.height
    require(w > 0 && h > 0 && w.toLong() * h.toLong() <= 25_000_000L) {
        "Image dimensions are unsupported"
    }
    val count = w * h
    val pixels = IntArray(count)
    bm.getPixels(pixels, 0, w, 0, 0, w, h)
    val histogram = HashMap<Int, Int>()
    fun bucket(c: Int): Int {
        val r = android.graphics.Color.red(c) / 16
        val g = android.graphics.Color.green(c) / 16
        val b = android.graphics.Color.blue(c) / 16
        return (r shl 8) or (g shl 4) or b
    }
    fun vote(index: Int) {
        val c = pixels[index]
        if (android.graphics.Color.alpha(c) < 16) return
        val k = bucket(c)
        histogram[k] = (histogram[k] ?: 0) + 1
    }
    for (x in 0 until w) { vote(x); vote((h - 1) * w + x) }
    for (y in 0 until h) { vote(y * w); vote(y * w + w - 1) }
    val dominant = histogram.maxByOrNull { it.value }?.key
    if (dominant == null) {
        return@withContext Bitmap.createBitmap(bm).copy(Bitmap.Config.ARGB_8888, true)
    }
    val br = ((dominant shr 8) and 15) * 16 + 8
    val bg = ((dominant shr 4) and 15) * 16 + 8
    val bb = (dominant and 15) * 16 + 8
    fun isBackground(index: Int): Boolean {
        val c = pixels[index]
        if (android.graphics.Color.alpha(c) < 16) return true
        val dr = android.graphics.Color.red(c) - br
        val dg = android.graphics.Color.green(c) - bg
        val db = android.graphics.Color.blue(c) - bb
        return dr * dr + dg * dg + db * db <= 38 * 38
    }
    val visited = BooleanArray(count)
    val queue = IntArray(count)
    var head = 0
    var tail = 0
    fun enqueue(index: Int) {
        if (!visited[index] && isBackground(index)) {
            visited[index] = true
            queue[tail++] = index
        }
    }
    for (x in 0 until w) { enqueue(x); enqueue((h - 1) * w + x) }
    for (y in 0 until h) { enqueue(y * w); enqueue(y * w + w - 1) }
    while (head < tail) {
        val i = queue[head++]
        val x = i % w
        if (x > 0) enqueue(i - 1)
        if (x + 1 < w) enqueue(i + 1)
        if (i >= w) enqueue(i - w)
        if (i < count - w) enqueue(i + w)
    }
    for (i in pixels.indices) {
        if (visited[i]) pixels[i] = android.graphics.Color.TRANSPARENT
    }
    // Remove isolated one-pixel specks only; do not join disconnected motifs.
    val cleaned = pixels.copyOf()
    for (y in 1 until h - 1) for (x in 1 until w - 1) {
        val i = y * w + x
        if (android.graphics.Color.alpha(pixels[i]) == 0) continue
        var neighbors = 0
        for (dy in -1..1) for (dx in -1..1) {
            if (dx != 0 || dy != 0) {
                if (android.graphics.Color.alpha(pixels[i + dy * w + dx]) > 0) neighbors++
            }
        }
        if (neighbors == 0) cleaned[i] = android.graphics.Color.TRANSPARENT
    }
    Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
        setPixels(cleaned, 0, w, 0, 0, w, h)
    }
}

fun embroideryColorName(c: Int): String {
 val r=android.graphics.Color.red(c);val g=android.graphics.Color.green(c);val b=android.graphics.Color.blue(c)
 val mx=maxOf(r,g,b);val mn=minOf(r,g,b)
 if(mx-mn<25)return when{mx<55->"أسود";mx<120->"رمادي داكن";mx<200->"رمادي";else->"أبيض"}
 if(r>170&&g>140&&b<120)return "ذهبي"
 if(r>g*1.35&&r>b*1.35)return if(g>90)"برتقالي" else "أحمر"
 if(g>r*1.25&&g>b*1.1)return "أخضر"
 if(b>r*1.25&&b>g*1.1)return "أزرق"
 if(r>110&&b>g*1.3)return "بنفسجي"
 if(r>g&&g>b)return if(r<150)"بني" else "بيج"
 return "لون مختلط"
}

suspend fun getColors(bm: Bitmap): List<Int> = withContext(Dispatchers.Default) {
    val map=mutableMapOf<Int,Int>()
    for(y in 0 until bm.height step 2) for(x in 0 until bm.width step 2){val p=bm.getPixel(x,y);if(android.graphics.Color.alpha(p)>128){val q=android.graphics.Color.rgb((android.graphics.Color.red(p)/16)*16,(android.graphics.Color.green(p)/16)*16,(android.graphics.Color.blue(p)/16)*16);map[q]=(map[q]?:0)+1}}
    val ranked = map.entries.sortedByDescending { it.value }
    val minCount = maxOf(3, ranked.sumOf { it.value } / 200)
    ranked.filter { it.value >= minCount }.take(12).map { it.key }
}

fun getPixelWidth(px: IntArray, w: Int, h: Int, x: Int, y: Int): Int {
    var width=0;var nx=x
    while(nx<w&&android.graphics.Color.alpha(px[y*w+nx])>0){width++;nx++}
    return width
}

fun isEdgePixel(px: IntArray, w: Int, h: Int, x: Int, y: Int): Boolean {
    if(android.graphics.Color.alpha(px[y*w+x])==0) return false
    return listOf(x-1 to y,x+1 to y,x to y-1,x to y+1,x-1 to y-1,x+1 to y-1,x-1 to y+1,x+1 to y+1).any{(nx,ny)->nx<0||ny<0||nx>=w||ny>=h||android.graphics.Color.alpha(px[ny*w+nx])==0}
}

fun calcStitchAngle(px: IntArray, w: Int, h: Int, x: Int, y: Int): Float {
    var dx=0f;var dy=0f
    for(ny in maxOf(0,y-4)..minOf(h-1,y+4)) for(nx in maxOf(0,x-4)..minOf(w-1,x+4)){if(android.graphics.Color.alpha(px[ny*w+nx])>0){dx+=(nx-x).toFloat();dy+=(ny-y).toFloat()}}
    return if(dx==0f&&dy==0f)0f else kotlin.math.atan2(dy,dx)+(Math.PI/2).toFloat()
}

suspend fun genStitchesPro(bm: Bitmap, colors: List<Int>, density: Int=4): List<Stitch> = withContext(Dispatchers.Default) {
 val w=bm.width;val h=bm.height;val px=IntArray(w*h);bm.getPixels(px,0,w,0,0,w,h)
 val palette=colors.ifEmpty{listOf(android.graphics.Color.BLACK)};val labels=IntArray(px.size){-1}
 for(i in px.indices){val c=px[i];if(android.graphics.Color.alpha(c)<160)continue
  var best=Int.MAX_VALUE;var chosen=0
  palette.forEachIndexed{index,col->val dr=android.graphics.Color.red(c)-android.graphics.Color.red(col);val dg=android.graphics.Color.green(c)-android.graphics.Color.green(col);val db=android.graphics.Color.blue(c)-android.graphics.Color.blue(col);val d=dr*dr+dg*dg+db*db;if(d<best){best=d;chosen=index}}
  labels[i]=chosen
 }
 val rowStep=density.coerceIn(3,8);val stitches=ArrayList<Stitch>()
 for(ci in palette.indices)for(y in 0 until h step rowStep){
  val runs=mutableListOf<Pair<Int,Int>>();var x=0
  while(x<w){if(labels[y*w+x]!=ci){x++;continue};val a=x;while(x<w&&labels[y*w+x]==ci)x++;if(x-a>=2)runs.add(a to (x-1))}
  val forward=(y/rowStep)%2==0
  for((a,b) in if(forward)runs else runs.asReversed()){
   val start=if(forward)a else b;val finish=if(forward)b else a
   val n=maxOf(1,kotlin.math.ceil((b-a).toDouble()/35.0).toInt())
   for(j in 0..n){val xx=start+(finish-start)*j.toFloat()/n;stitches.add(Stitch(xx,y.toFloat(),ci,palette[ci],StitchType.TATAMI,0f))}
  }
 }
 stitches
}

fun calcQuality(bm: Bitmap, stitches: List<Stitch>): QualityReport {
    val total=stitches.size;val w=bm.width;val h=bm.height;val px=IntArray(w*h);bm.getPixels(px,0,w,0,0,w,h)
    val totalSolid=px.count{android.graphics.Color.alpha(it)>128};val covered=stitches.map{it.y.toInt()*w+it.x.toInt()}.toSet().size
    val coverage=if(totalSolid>0)(covered.toFloat()/totalSolid*100f).coerceAtMost(100f) else 0f
    val outOfBounds=if(total>0)stitches.count{it.x<0||it.y<0||it.x>=w||it.y>=h}.toFloat()/total*100f else 0f
    val colorChanges=stitches.zipWithNext().count{(a,b)->a.colorIdx!=b.colorIdx}
    return QualityReport(total,coverage,outOfBounds,stitches.map{it.colorIdx}.toSet().size,w*0.1f,h*0.1f,stitches.count{it.type==StitchType.SATIN},stitches.count{it.type==StitchType.RUN},stitches.count{it.type==StitchType.TATAMI},colorChanges)
}


private fun tajimaRecord(dx: Int, dy: Int, command: Int = 0x03): ByteArray {
    require(dx in -121..121 && dy in -121..121)
    val b=IntArray(3)
    fun axis(value: Int, weights: IntArray, positive: IntArray, negative: IntArray, indices: IntArray) {
        var remainder=value
        for(i in weights.indices) {
            val weight=weights[i]
            if(remainder>weight/2){b[indices[i]]=b[indices[i]] or positive[i];remainder-=weight}
            else if(remainder< -weight/2){b[indices[i]]=b[indices[i]] or negative[i];remainder+=weight}
        }
        require(remainder==0)
    }
    val weights=intArrayOf(81,27,9,3,1)
    val indices=intArrayOf(2,1,0,1,0)
    axis(dx,weights,intArrayOf(0x04,0x04,0x04,0x01,0x01),intArrayOf(0x08,0x08,0x08,0x02,0x02),indices)
    axis(dy,weights,intArrayOf(0x20,0x20,0x20,0x80,0x80),intArrayOf(0x10,0x10,0x10,0x40,0x40),indices)
    b[2]=b[2] or command
    return byteArrayOf(b[0].toByte(),b[1].toByte(),b[2].toByte())
}

suspend fun exportProject(ctx: android.content.Context, p: Project) = withContext(Dispatchers.IO) {
    require(p.stitches.isNotEmpty()) {"No stitches to export"}
    val dir=ctx.getExternalFilesDir(null)?:error("Export directory unavailable")
    val safeName=p.name.replace(Regex("[^\\p{L}\\p{N} _.-]"),"_").take(70).ifBlank{"embroidery"}
    p.extracted?.let{bm->FileOutputStream(File(dir,safeName+".png")).use{bm.compress(Bitmap.CompressFormat.PNG,100,it)}}
    FileOutputStream(File(dir,safeName+".dst")).use{fos->
        val hdr=ByteArray(512){0x20}
        val header=("LA:"+safeName.take(12)+"\rST:"+p.stitches.size.toString().padStart(7,' ')+"\rCO:"+p.stitches.zipWithNext().count{(a,b)->a.colorIdx!=b.colorIdx}.toString().padStart(3,' ')+"\r")
        val hb=header.toByteArray(Charsets.US_ASCII)
        System.arraycopy(hb,0,hdr,0,minOf(hb.size,511))
        hdr[511]=0x1A;fos.write(hdr)
        var lastX=0;var lastY=0;var lastColor=p.stitches.first().colorIdx
        val scaleX=if(p.widthMM>0f && p.extracted!=null) p.widthMM*10f/p.extracted.width else 1f
        val scaleY=if(p.heightMM>0f && p.extracted!=null) p.heightMM*10f/p.extracted.height else scaleX
        p.stitches.forEachIndexed{index,st->
            if(index>0 && st.colorIdx!=lastColor){fos.write(tajimaRecord(0,0,0xC3));lastColor=st.colorIdx}
            val targetX=(st.x*scaleX).toInt()
            val targetY= -(st.y*scaleY).toInt()
            var dx=targetX-lastX;var dy=targetY-lastY
            // A disconnected region must never be sewn together merely because
            // its distance happens to be less than the DST 12.1 mm record limit.
            val distanceMM = kotlin.math.hypot(dx.toDouble(), dy.toDouble()) / 10.0
            val isJump = index == 0 || distanceMM > 5.0 || (index > 0 && st.type != p.stitches[index-1].type && distanceMM > 2.0)
            if(dx==0 && dy==0)fos.write(tajimaRecord(0,0))
            while(dx!=0 || dy!=0){
                val sx=dx.coerceIn(-121,121);val sy=dy.coerceIn(-121,121)
                fos.write(tajimaRecord(sx,sy,if(isJump)0x83 else 0x03))
                dx-=sx;dy-=sy
            }
            lastX=targetX;lastY=targetY
        }
        fos.write(byteArrayOf(0,0,0xF3.toByte()))
    }
}

suspend fun saveProject(ctx: android.content.Context, p: Project) = withContext(Dispatchers.IO) {
    val dir=File(ctx.filesDir,"projects");dir.mkdirs()
    val json=JSONObject().apply{put("id",p.id);put("name",p.name);put("savedAt",p.savedAt);put("widthMM",p.widthMM);put("heightMM",p.heightMM);put("stitchCount",p.stitches.size);put("colorCount",p.colors.size);val ca=JSONArray();p.colors.forEach{ca.put(it)};put("colors",ca)}
    File(dir,p.id+".json").writeText(json.toString())
    java.io.DataOutputStream(java.io.BufferedOutputStream(FileOutputStream(File(dir,p.id+".stitches")))).use{out->
        out.writeInt(0x4E515348);out.writeInt(p.stitches.size)
        p.stitches.forEach{st->out.writeFloat(st.x);out.writeFloat(st.y);out.writeInt(st.colorIdx);out.writeInt(st.color);out.writeByte(st.type.ordinal);out.writeFloat(st.angle)}
    }
    p.extracted?.let{bm->FileOutputStream(File(dir,"${p.id}.png")).use{bm.compress(Bitmap.CompressFormat.PNG,100,it)}}
}

suspend fun loadProjects(ctx: android.content.Context): List<Project> = withContext(Dispatchers.IO) {
    val dir=File(ctx.filesDir,"projects");if(!dir.exists()) return@withContext emptyList()
    dir.listFiles{f->f.extension=="json"}?.mapNotNull{f->try{val json=JSONObject(f.readText());val colors=mutableListOf<Int>();val arr=json.getJSONArray("colors");for(i in 0 until arr.length())colors.add(arr.getInt(i));val imgFile=File(dir,"${json.getString("id")}.png");val bm=if(imgFile.exists())BitmapFactory.decodeFile(imgFile.absolutePath) else null;val sf=File(dir,json.getString("id")+".stitches")
        val stitches=if(sf.exists())try{java.io.DataInputStream(java.io.BufferedInputStream(sf.inputStream())).use{input->
            require(input.readInt()==0x4E515348)
            val count=input.readInt();require(count in 0..2000000)
            List(count){Stitch(input.readFloat(),input.readFloat(),input.readInt(),input.readInt(),StitchType.values()[input.readUnsignedByte()],input.readFloat())}
        }}catch(e:Exception){emptyList()} else emptyList()
        Project(id=json.getString("id"),name=json.getString("name"),extracted=bm,colors=colors,stitches=stitches,widthMM=json.getDouble("widthMM").toFloat(),heightMM=json.getDouble("heightMM").toFloat(),savedAt=json.getLong("savedAt"))}catch(e:Exception){null}}?.sortedByDescending{it.savedAt}?:emptyList()
}

suspend fun deleteProject(ctx: android.content.Context, id: String) = withContext(Dispatchers.IO) {
    val dir=File(ctx.filesDir,"projects");File(dir,"$id.json").delete();File(dir,"$id.png").delete();File(dir,"$id.stitches").delete()
}
