package micro.repl.ma7moud3ly.dfu

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.*
import android.os.Build
import androidx.core.content.ContextCompat

class Usb(private val context: Context) {
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var connection: UsbDeviceConnection? = null
    private var iface: UsbInterface? = null

    // شناسه STM32 Bootloader
    private val VENDOR_ID = 0x0483
    private val PRODUCT_ID = 0xDF11
    private val ACTION_USB_PERMISSION = "micro.repl.ma7moud3ly.USB_PERMISSION"

    fun findDevice(): UsbDevice? {
        return usbManager.deviceList.values.find {
            it.vendorId == VENDOR_ID && it.productId == PRODUCT_ID
        }
    }

    fun requestPermission(device: UsbDevice, onResult: (Boolean) -> Unit) {
        if (usbManager.hasPermission(device)) {
            onResult(true)
            return
        }

        val flags = if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_UPDATE_CURRENT
        val intent = Intent(ACTION_USB_PERMISSION).setPackage(context.packageName)
        val pi = PendingIntent.getBroadcast(context, 0, intent, flags)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action == ACTION_USB_PERMISSION) {
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    context.unregisterReceiver(this)
                    onResult(granted)
                }
            }
        }
        ContextCompat.registerReceiver(context, receiver, IntentFilter(ACTION_USB_PERMISSION), ContextCompat.RECEIVER_EXPORTED)
        usbManager.requestPermission(device, pi)
    }

    fun connect(device: UsbDevice): Boolean {
        connection = usbManager.openDevice(device) ?: return false
        iface = device.getInterface(0) // DFU معمولا اینترفیس 0 است
        if (!connection!!.claimInterface(iface, true)) {
            disconnect()
            return false
        }
        return true
    }

    fun disconnect() {
        try {
            iface?.let { connection?.releaseInterface(it) }
            connection?.close()
        } catch (e: Exception) { e.printStackTrace() }
        connection = null
        iface = null
    }

    fun isConnected() = connection != null

    // ارسال دستورات کنترلی (قلب تپنده DFU)
    fun controlTransfer(reqType: Int, req: Int, value: Int, index: Int, data: ByteArray? = null, len: Int = 0): Int {
        return connection?.controlTransfer(reqType, req, value, iface?.id ?: 0, data, len, 5000) ?: -1
    }
}