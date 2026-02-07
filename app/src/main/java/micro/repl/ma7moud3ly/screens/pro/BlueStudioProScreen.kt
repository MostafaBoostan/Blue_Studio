package micro.repl.ma7moud3ly.screens.pro

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun BlueStudioProScreen(
    deviceIdName: String, // اسم آیدی رو از ورودی میگیریم
    onFinished: () -> Unit // تابعی که بعد از 3 ثانیه اجرا میشه
) {
    BackHandler(enabled = true) { }

    LaunchedEffect(Unit) {
        delay(1000) // 3000 میلی‌ثانیه = 3 ثانیه
        onFinished() // برو صفحه بعدی
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000)), // مشکی مطلق برای شروع مرموز
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "ACCESS GRANTED",
                color = Color.Green,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Welcome to Pro Mode",
                color = Color.White,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = deviceIdName,
                color = Color.Cyan,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}