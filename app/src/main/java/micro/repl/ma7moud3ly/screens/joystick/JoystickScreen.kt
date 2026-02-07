package micro.repl.ma7moud3ly.screens.joystick

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import micro.repl.ma7moud3ly.MainViewModel
import micro.repl.ma7moud3ly.managers.TerminalManager
import kotlin.math.abs
import kotlin.math.roundToInt

const val MOTOR_LIB = """
import pyb, machine
class BMotor:
    MOTOR_CONFIG={1:('Y3','X4'),2:('Y4','X3'),3:('X2','X6'),4:('X1','X7')}
    PIN_TO_TIMER={'X1':(5,1),'X2':(2,2),'X3':(2,3),'X4':(5,4),'X6':(2,1),'X7':(3,1),'Y3':(4,3),'Y4':(4,4)}
    def __init__(self,mid,freq=20000):
        pa,pb=self.MOTOR_CONFIG[mid]
        ta,ca=self.PIN_TO_TIMER[pa]
        tb,cb=self.PIN_TO_TIMER[pb]
        self.cha=pyb.Timer(ta,freq=freq).channel(ca,pyb.Timer.PWM,pin=pyb.Pin(pa))
        self.chb=pyb.Timer(tb,freq=freq).channel(cb,pyb.Timer.PWM,pin=pyb.Pin(pb))
        self.stop()
    def forward(self,s):
        if s>100:s=100
        if s<0:s=0
        self.cha.pulse_width_percent(s);self.chb.pulse_width_percent(0)
    def backward(self,s):
        if s>100:s=100
        if s<0:s=0
        self.cha.pulse_width_percent(0);self.chb.pulse_width_percent(s)
    def stop(self):
        self.cha.pulse_width_percent(0);self.chb.pulse_width_percent(0)
try:
    m1=BMotor(1); m2=BMotor(2); m3=BMotor(3); m4=BMotor(4)
    print("Motors Ready")
except: pass
"""

val CustomOrange = Color(0xFFEE6C1B)
val CustomBlue = Color(0xFF2184AB)

object Colors {
    val Transparent = Color(0x00000000)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoystickScreen(
    viewModel: MainViewModel,
    terminalManager: TerminalManager,
    onBack: () -> Unit
) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    val bgBrush = if (isDarkMode) {
        Brush.verticalGradient(listOf(Color(0xFF121212), Color(0xFF0A0A0A)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFF5F5F5), Color(0xFFE0E0E0)))
    }

    val textColor = if (isDarkMode) Color.White else Color(0xFF333333)
    val trackBgColor = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFEEEEEE)
    val borderColor = if (isDarkMode) Color.Transparent else Color(0xFFBDBDBD)

    var isInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!isInitialized) {
            terminalManager.sendCommand("\u0005" + MOTOR_LIB + "\u0004")
            isInitialized = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            terminalManager.sendCommand("m1.stop();m2.stop();m3.stop();m4.stop()\r\n")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TANK COMMANDER", color = textColor, fontWeight = FontWeight.Black, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = textColor)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        terminalManager.sendCommand("\u0005" + MOTOR_LIB + "\u0004")
                    }) {
                        Icon(Icons.Default.Refresh, "Init", tint = textColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Colors.Transparent)
            )
        },
        containerColor = Colors.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgBrush)
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GraphicThrottle(
                    label = "LEFT TRACK (M3+M4)",
                    primaryColor = CustomBlue,
                    trackBgColor = trackBgColor,
                    borderColor = borderColor,
                    textColor = textColor,
                    onSpeedChange = { speed ->
                        val cmd = when {
                            speed > 0 -> "m3.forward($speed);m4.forward($speed)"
                            speed < 0 -> "m3.backward(${-speed});m4.backward(${-speed})"
                            else -> "m3.stop();m4.stop()"
                        }
                        terminalManager.sendCommand(cmd + "\r\n")
                    }
                )

                GraphicThrottle(
                    label = "RIGHT TRACK (M1+M2)",
                    primaryColor = CustomOrange,
                    trackBgColor = trackBgColor,
                    borderColor = borderColor,
                    textColor = textColor,
                    onSpeedChange = { speed ->
                        val cmd = when {
                            speed > 0 -> "m1.forward($speed);m2.forward($speed)"
                            speed < 0 -> "m1.backward(${-speed});m2.backward(${-speed})"
                            else -> "m1.stop();m2.stop()"
                        }
                        terminalManager.sendCommand(cmd + "\r\n")
                    }
                )
            }
        }
    }
}

@Composable
fun GraphicThrottle(
    label: String,
    primaryColor: Color,
    trackBgColor: Color,
    borderColor: Color,
    textColor: Color,
    onSpeedChange: (Int) -> Unit
) {
    val view = LocalView.current
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var lastSpeed by remember { mutableIntStateOf(0) }

    val trackHeight = 300.dp
    val trackWidth = 90.dp
    val thumbSize = 80.dp
    val maxDrag = 360f

    val currentSpeed = ((-dragOffsetY / maxDrag) * 100).toInt().coerceIn(-100, 100)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(primaryColor.copy(alpha = 0.2f), primaryColor.copy(alpha = 0.4f))
                    )
                )
                .border(2.dp, primaryColor.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "${abs(currentSpeed)}%",
                color = if (textColor == Color.White) Color.White else Color(0xFF333333),
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .height(trackHeight)
                .width(trackWidth)
                .shadow(elevation = 10.dp, shape = RoundedCornerShape(50.dp), spotColor = primaryColor)
                .clip(RoundedCornerShape(50.dp))
                .background(trackBgColor)
                .border(
                    width = 3.dp,
                    brush = Brush.verticalGradient(
                        listOf(primaryColor.copy(alpha = 0.5f), primaryColor, primaryColor.copy(alpha = 0.5f))
                    ),
                    shape = RoundedCornerShape(50.dp)
                )
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY) },
                        onDragEnd = {
                            dragOffsetY = 0f
                            onSpeedChange(0)
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val newY = dragOffsetY + dragAmount.y
                            dragOffsetY = newY.coerceIn(-maxDrag, maxDrag)
                            val speed = ((-dragOffsetY / maxDrag) * 100).toInt().coerceIn(-100, 100)
                            if (abs(speed - lastSpeed) > 5 || speed == 0) {
                                onSpeedChange(speed)
                                lastSpeed = speed
                                if (speed % 20 == 0 && speed != 0) {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                            }
                        }
                    )
                }
        ) {
            // *** خط وسط حذف شد ***

            Box(
                modifier = Modifier
                    .offset { IntOffset(0, dragOffsetY.roundToInt()) }
                    .size(thumbSize)
                    .align(Alignment.Center)
                    .shadow(20.dp, CircleShape, spotColor = primaryColor)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(primaryColor.copy(alpha = 0.8f), primaryColor),
                            center = androidx.compose.ui.geometry.Offset.Unspecified,
                            radius = thumbSize.value * 1.5f
                        )
                    )
                    .border(2.dp, Color.White, CircleShape)
            ) {
                Icon(
                    imageVector = if (dragOffsetY < 0) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = label,
            color = textColor.copy(alpha = 0.7f),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 1.5.sp
        )
    }
}