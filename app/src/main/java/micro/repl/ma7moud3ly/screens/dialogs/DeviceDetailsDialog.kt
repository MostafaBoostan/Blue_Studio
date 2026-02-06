package micro.repl.ma7moud3ly.screens.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import micro.repl.ma7moud3ly.model.MicroDevice

@Composable
fun DeviceDetailsDialog(
    device: MicroDevice?,
    onDisconnect: () -> Unit,
    onDismiss: () -> Unit
) {
    if (device == null) return

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = Color(0xFF252525))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Device Info",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // نمایش اطلاعات دستگاه بدون نیاز به فایل اضافه
                DeviceDetailsList(device)

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        Text("Close")
                    }

                    Button(
                        onClick = {
                            onDisconnect()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                    ) {
                        Text("Disconnect")
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceDetailsList(device: MicroDevice) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DetailItem("Product", device.usbDevice?.productName ?: "Unknown")
        DetailItem("Manufacturer", device.usbDevice?.manufacturerName ?: "Unknown")
        DetailItem("Board", device.board)
        DetailItem("Port", device.port)
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray, fontSize = 14.sp)
        Text(text = value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}