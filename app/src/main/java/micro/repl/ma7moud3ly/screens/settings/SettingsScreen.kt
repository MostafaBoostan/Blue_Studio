package micro.repl.ma7moud3ly.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import micro.repl.ma7moud3ly.MainViewModel
import java.text.DecimalFormat

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    // دریافت وضعیت‌ها
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val fontScale by viewModel.fontScale.collectAsState()

    val bgColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F5F5)
    val cardColor = if (isDarkMode) Color(0xFF252525) else Color(0xFFFFFFFF)
    val textColor = if (isDarkMode) Color.White else Color.Black
    val neonColor = Color(0xFFFF4081)

    Scaffold(
        containerColor = bgColor,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onBack() },
                    colors = IconButtonDefaults.iconButtonColors(contentColor = textColor)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SETTINGS",
                    color = textColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // --- Appearance ---
            SettingsSectionTitle("APPEARANCE", neonColor)
            SettingsCard(cardColor) {
                // Dark Mode Switch
                SettingsSwitchItem(
                    title = "Dark Mode",
                    icon = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                    checked = isDarkMode,
                    textColor = textColor,
                    onCheckedChange = { viewModel.setDarkMode(it) }
                )

                Divider(color = bgColor, thickness = 1.dp)

                // Font Size Slider
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FormatSize, null, tint = textColor, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Text("Font Size", color = textColor, fontSize = 16.sp)
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "${DecimalFormat("#.##").format(fontScale)}x",
                            color = neonColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Slider(
                        value = fontScale,
                        onValueChange = { viewModel.setFontScale(it) },
                        valueRange = 0.8f..1.4f, // محدوده تغییر سایز (از 0.8 تا 1.4 برابر)
                        steps = 5,
                        colors = SliderDefaults.colors(
                            thumbColor = neonColor,
                            activeTrackColor = neonColor,
                            inactiveTrackColor = if(isDarkMode) Color.DarkGray else Color.LightGray
                        )
                    )
                }
            }

            // --- About ---
            SettingsSectionTitle("ABOUT", neonColor)
            SettingsCard(cardColor) {
                SettingsActionItem("Version", "v1.3", Icons.Default.Info, {}, textColor)
                Divider(color = bgColor, thickness = 1.dp)
                // اضافه شدن نام Mostafa Boustan
                SettingsActionItem("Developer", "Mostafa Boustan", Icons.Default.Code, {}, textColor)
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "MicroBluePy Manager 2026",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

// --- کامپوننت‌های کمکی ---
@Composable
fun SettingsSectionTitle(title: String, color: Color) {
    Text(
        text = title,
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun SettingsCard(color: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(color),
        content = content
    )
}

@Composable
fun SettingsSwitchItem(
    title: String, icon: ImageVector, checked: Boolean, textColor: Color, onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = textColor, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(title, color = textColor, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFFFF4081)
            )
        )
    }
}

@Composable
fun SettingsActionItem(
    title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit, textColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = textColor, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = textColor, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, color = Color.Gray, fontSize = 12.sp)
        }
    }
}