package micro.repl.ma7moud3ly.screens.plotter

import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import micro.repl.ma7moud3ly.MainViewModel // ایمپورت ویومدل
import micro.repl.ma7moud3ly.managers.TerminalManager
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlotterScreen(
    viewModel: MainViewModel, // اضافه شدن ویومدل برای تشخیص تم
    terminalManager: TerminalManager,
    onBack: () -> Unit
) {
    // *** تشخیص تم ***
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    // پالت رنگی بر اساس تم
    val bgColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F5F5)
    val cardColor = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
    val terminalBg = if (isDarkMode) Color(0xFF0F0F0F) else Color(0xFFEEEEEE)
    val textColor = if (isDarkMode) Color.White else Color.Black
    val terminalTextColor = if (isDarkMode) Color(0xFF69F0AE) else Color(0xFF00796B)
    val accentColor = Color(0xFF00E5FF)
    val borderColor = if (isDarkMode) Color(0xFF333333) else Color(0xFFDDDDDD)

    // رنگ‌های نمودار (چون کتابخانه جاوا است باید Integer باشد)
    val chartAxisColor = if (isDarkMode) android.graphics.Color.LTGRAY else android.graphics.Color.DKGRAY
    val chartGridColor = if (isDarkMode) android.graphics.Color.parseColor("#333333") else android.graphics.Color.parseColor("#E0E0E0")

    var isRunning by remember { mutableStateOf(true) }
    var showHelp by remember { mutableStateOf(false) }
    var selectedPoint by remember { mutableStateOf("") }

    val chartRef = remember { mutableStateOf<LineChart?>(null) }
    val entries = remember { mutableListOf<Entry>() }
    var xValue by remember { mutableLongStateOf(0L) }

    var terminalOutput by remember { mutableStateOf("") }
    var terminalInput by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    DisposableEffect(Unit) {
        onDispose {
            terminalManager.sendCommand("\u0003")
        }
    }

    LaunchedEffect(Unit) {
        terminalManager.output.collect { text ->
            if (isRunning && text.isNotEmpty()) {
                terminalOutput += text
                val lines = text.split("\n")
                for (line in lines) {
                    val cleanLine = line.trim()
                    val value = cleanLine.toFloatOrNull()
                    if (value != null) {
                        entries.add(Entry(xValue.toFloat(), value))
                        xValue++
                        if (entries.size > 150) entries.removeAt(0)

                        chartRef.value?.let { chart ->
                            val dataSet = chart.data?.getDataSetByIndex(0) as? LineDataSet
                            if (dataSet == null) {
                                val set = LineDataSet(entries, "Signal")
                                set.color = accentColor.toArgb()
                                set.setDrawCircles(false)
                                set.lineWidth = 2f
                                set.mode = LineDataSet.Mode.CUBIC_BEZIER
                                set.setDrawFilled(true)
                                set.fillColor = accentColor.toArgb()
                                set.fillAlpha = 50
                                set.setDrawValues(false)
                                set.highLightColor = chartAxisColor
                                chart.data = LineData(set)
                            } else {
                                dataSet.values = entries
                                chart.data.notifyDataChanged()
                                chart.notifyDataSetChanged()
                                chart.invalidate()
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(terminalOutput) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    // آپدیت رنگ‌های نمودار وقتی تم عوض میشه
    LaunchedEffect(isDarkMode) {
        chartRef.value?.let { chart ->
            chart.xAxis.textColor = chartAxisColor
            chart.axisLeft.textColor = chartAxisColor
            chart.axisLeft.gridColor = chartGridColor
            chart.invalidate()
        }
    }

    fun sendCommand() {
        if (terminalInput.isNotEmpty()) {
            val cmd = terminalInput
            if (cmd.contains("\n")) terminalManager.sendCommand("\u0005" + cmd + "\u0004")
            else terminalManager.sendCommand(cmd + "\r\n")
            terminalOutput += ">>> [SENT]\n"
            terminalInput = ""
        }
    }

    if (showHelp) {
        Dialog(onDismissRequest = { showHelp = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Plotter Guide", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Paste this code to visualize sine wave:", color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(terminalBg, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "import math\nimport time\nx = 0\nwhile True:\n  print(math.sin(x))\n  x += 0.2\n  time.sleep(0.05)",
                            color = terminalTextColor,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(15.dp))
                    Button(
                        onClick = { showHelp = false },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("OK", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Live Plotter", color = textColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.background(accentColor.copy(0.2f), RoundedCornerShape(4.dp)).padding(horizontal=4.dp)) {
                            Text("PRO", color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        terminalManager.sendCommand("\u0003")
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = textColor)
                    }
                },
                actions = {
                    IconButton(onClick = { showHelp = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Help", tint = Color(0xFFFFAB40))
                    }
                    IconButton(onClick = {
                        entries.clear()
                        xValue = 0
                        chartRef.value?.clear()
                        terminalOutput = ""
                        selectedPoint = ""
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear", tint = textColor)
                    }
                    IconButton(onClick = { isRunning = !isRunning }) {
                        Icon(
                            if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Pause",
                            tint = if(isRunning) Color(0xFF69F0AE) else Color.Red
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor)
            )
        },
        containerColor = bgColor
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Chart Card
            Card(
                modifier = Modifier.weight(0.5f).fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                border = BorderStroke(1.dp, borderColor)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        factory = { context ->
                            LineChart(context).apply {
                                layoutParams = LinearLayout.LayoutParams(-1, -1)
                                description.isEnabled = false
                                legend.isEnabled = false
                                axisRight.isEnabled = false
                                setTouchEnabled(true)
                                setDragEnabled(true)
                                setScaleEnabled(true)
                                setPinchZoom(true)
                                xAxis.position = XAxis.XAxisPosition.BOTTOM
                                xAxis.textColor = chartAxisColor
                                xAxis.setDrawGridLines(false)
                                axisLeft.textColor = chartAxisColor
                                axisLeft.gridColor = chartGridColor

                                setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                                    override fun onValueSelected(e: Entry?, h: Highlight?) {
                                        e?.let {
                                            val x = String.format(Locale.US, "%.1f", it.x)
                                            val y = String.format(Locale.US, "%.2f", it.y)
                                            selectedPoint = "T: $x | V: $y"
                                        }
                                    }
                                    override fun onNothingSelected() { selectedPoint = "" }
                                })
                                chartRef.value = this
                            }
                        },
                        // این بخش مهمه: وقتی تم عوض میشه رنگ‌ها رو آپدیت میکنه
                        update = { chart ->
                            chart.xAxis.textColor = chartAxisColor
                            chart.axisLeft.textColor = chartAxisColor
                            chart.axisLeft.gridColor = chartGridColor
                            chart.invalidate()
                        }
                    )

                    if (selectedPoint.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 10.dp)
                                .background(accentColor.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(selectedPoint, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Terminal Output Card
            Card(
                modifier = Modifier.weight(0.3f).fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = terminalBg),
                border = BorderStroke(1.dp, borderColor)
            ) {
                Box(modifier = Modifier.padding(8.dp).fillMaxSize()) {
                    Text(
                        text = if(terminalOutput.isEmpty()) "// Output logs..." else terminalOutput,
                        color = if(terminalOutput.isEmpty()) Color.Gray else terminalTextColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.verticalScroll(scrollState)
                    )
                }
            }

            // Input Area
            Row(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(cardColor, RoundedCornerShape(12.dp))
                        .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    if (terminalInput.isEmpty()) {
                        Text("Code here...", color = Color.Gray, fontSize = 12.sp)
                    }
                    BasicTextField(
                        value = terminalInput,
                        onValueChange = { terminalInput = it },
                        textStyle = TextStyle(
                            color = textColor,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        ),
                        cursorBrush = SolidColor(accentColor),
                        singleLine = false,
                        keyboardOptions = KeyboardOptions.Default,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Button(
                    onClick = { sendCommand() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    modifier = Modifier.width(70.dp).fillMaxHeight()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(24.dp))
                        Text("RUN", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}