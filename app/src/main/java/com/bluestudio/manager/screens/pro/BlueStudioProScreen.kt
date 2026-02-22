package com.bluestudio.manager.screens.pro

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.bluestudio.manager.MainViewModel

@Composable
fun BlueStudioProScreen(
    viewModel: MainViewModel,
    deviceIdName: String,
    onFinished: () -> Unit
) {
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val isFa = currentLanguage == "fa"
    val layoutDirection = if (isFa) LayoutDirection.Rtl else LayoutDirection.Ltr

    BackHandler(enabled = true) { }

    LaunchedEffect(Unit) {
        delay(3000)
        onFinished()
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF000000)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isFa) "دسترسی تایید شد" else "ACCESS GRANTED",
                    color = Color.Green,
                    fontSize = if (isFa) 32.sp else 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = if (isFa) 0.sp else 4.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = if (isFa) "خوش‌آمدید به حالت حرفه‌ای" else "Welcome to Pro Mode",
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
}