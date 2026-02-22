package com.bluestudio.manager.managers

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_MUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.bluestudio.manager.model.toMicroDevice
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.ProbeTable
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import kotlinx.serialization.json.Json
import com.bluestudio.manager.managers.CommandsManager.isSilentExecutionDone
import com.bluestudio.manager.managers.CommandsManager.trimSilentResult
import com.bluestudio.manager.model.ConnectionError
import com.bluestudio.manager.model.ConnectionStatus
import com.bluestudio.manager.model.toMicroDevice
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList

class BoardManager(
    private val context: Context,
    private val onStatusChanges: ((status: com.bluestudio.manager.model.ConnectionStatus) -> Unit)? = null,
    // این پارامتر قدیمی رو نگه میداریم که جاهای دیگه کد خراب نشه
    private val onReceiveData: ((data: String, clear: Boolean) -> Unit)? = null,
) : SerialInputOutputManager.Listener, DefaultLifecycleObserver {

    enum class ExecutionMode {
        INTERACTIVE,
        SCRIPT
    }

    companion object {
        private const val ACTION_USB_PERMISSION = "micro.repl.ma7moud3ly.USB_PERMISSION"
        private const val WRITING_TIMEOUT = 2000
    }

    private val activity = context as Activity
    private lateinit var usbManager: UsbManager
    private var serialInputOutputManager: SerialInputOutputManager? = null

    // *** لیست جدید برای ذخیره چندین شنونده (ترمینال + پلاتر) ***
    private val dataListeners = CopyOnWriteArrayList<(String) -> Unit>()

    // *** تابعی که TerminalManager دنبالش میگرده ***
    fun addOnDataReceivedListener(listener: (String) -> Unit) {
        dataListeners.add(listener)
    }

    var port: UsbSerialPort? = null
    val isPortOpen: Boolean get() = port?.isOpen == true

    val usbSerialPort: UsbSerialPort? get() = port

    fun isConnected(): Boolean {
        return isPortOpen
    }

    private var onReadSync: ((data: String) -> Unit)? = null
    private var syncData = StringBuilder("")
    private var executionMode = ExecutionMode.INTERACTIVE
    private var permissionGranted = false

    private val supportedManufacturers = mutableListOf("MicroPython")
    private var supportedProducts = mutableSetOf<Int>()

    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        (activity as ComponentActivity).lifecycle.addObserver(this)
        getProducts()

        // اگر در سازنده کدی داده شده بود، اون رو هم به لیست اضافه میکنیم تا کار کنه
        if (onReceiveData != null) {
            addOnDataReceivedListener { data ->
                onReceiveData.invoke(data, false)
            }
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        detectUsbDevices()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        try {
            context.unregisterReceiver(usbReceiver)
            disconnect()
        } catch (e: Exception) {}
    }

    fun disconnect() {
        try {
            if (serialInputOutputManager != null) {
                serialInputOutputManager?.listener = null
                serialInputOutputManager?.stop()
                serialInputOutputManager = null
            }
            if (port != null) {
                try {
                    port?.dtr = false
                    port?.rts = false
                } catch (ignored: Exception) {}
                port?.close()
                port = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        onStatusChanges?.invoke(
            _root_ide_package_.com.bluestudio.manager.model.ConnectionStatus.Error(
                _root_ide_package_.com.bluestudio.manager.model.ConnectionError.CONNECTION_LOST, "Disconnected"))
    }

    fun writeInSilentMode(code: String, onResponse: ((data: String) -> Unit)? = null) {
        writeCommand(CommandsManager.SILENT_MODE)
        executionMode = ExecutionMode.SCRIPT
        syncData.clear()
        onReadSync = { result ->
            onResponse?.invoke(result)
            executionMode = ExecutionMode.INTERACTIVE
            syncData.clear()
            onReadSync = null
        }
        val cmd = "\u000D" + code + "\u000D"
        writeToPort(cmd.toByteArray(Charsets.UTF_8))
        writeCommand(CommandsManager.RESET)
    }

    fun write(code: String) {
        val cmd = "\u000D" + code + "\u000D"
        writeToPort(cmd.toByteArray(Charsets.UTF_8))
    }

    fun writeCommand(code: String) {
        writeToPort(code.toByteArray(Charsets.UTF_8))
    }

    private fun writeToPort(data: ByteArray) {
        if (!isPortOpen) return
        try {
            port?.write(data, WRITING_TIMEOUT)
        } catch (e: Exception) {
            onRunError(e)
        }
    }

    fun detectUsbDevices() {
        if (isPortOpen) disconnect()

        usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val deviceList = usbManager.deviceList

        if (deviceList.isEmpty()) {
            onStatusChanges?.invoke(
                _root_ide_package_.com.bluestudio.manager.model.ConnectionStatus.Error(
                    _root_ide_package_.com.bluestudio.manager.model.ConnectionError.NO_DEVICES))
            return
        }

        val supportedDevice: UsbDevice? = deviceList.values.firstOrNull {
            supportedManufacturers.contains(it.manufacturerName) || supportedProducts.contains(it.productId)
        } ?: deviceList.values.firstOrNull()

        if (supportedDevice != null) {
            onStatusChanges?.invoke(_root_ide_package_.com.bluestudio.manager.model.ConnectionStatus.Connecting)
            approveDevice(supportedDevice)
        } else {
            onStatusChanges?.invoke(
                _root_ide_package_.com.bluestudio.manager.model.ConnectionStatus.Error(
                    _root_ide_package_.com.bluestudio.manager.model.ConnectionError.NO_DEVICES))
        }
    }

    fun approveDevice(usbDevice: UsbDevice) {
        try {
            if (usbManager.hasPermission(usbDevice)) {
                connectToSerial(usbDevice)
            } else {
                requestUsbPermission(usbDevice)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onStatusChanges?.invoke(
                _root_ide_package_.com.bluestudio.manager.model.ConnectionStatus.Error(
                    _root_ide_package_.com.bluestudio.manager.model.ConnectionError.CANT_OPEN_PORT))
        }
    }

    fun onDisconnectDevice() {
        disconnect()
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun requestUsbPermission(usbDevice: UsbDevice) {
        val permissionIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(ACTION_USB_PERMISSION).apply { `package` = context.packageName },
            if (SDK_INT >= 31) FLAG_MUTABLE or FLAG_UPDATE_CURRENT else 0
        )
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        if (SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else context.registerReceiver(usbReceiver, filter)

        permissionGranted = false
        usbManager.requestPermission(usbDevice, permissionIntent)
    }

    private val usbReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (isPortOpen) return
            if (ACTION_USB_PERMISSION == intent.action) {
                synchronized(this) {
                    val device: UsbDevice = intent.parcelable(UsbManager.EXTRA_DEVICE) ?: return
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        permissionGranted = true
                        connectToSerial(device)
                    } else {
                        onStatusChanges?.invoke(
                            _root_ide_package_.com.bluestudio.manager.model.ConnectionStatus.Error(
                                _root_ide_package_.com.bluestudio.manager.model.ConnectionError.PERMISSION_DENIED))
                    }
                }
            }
        }
    }

    private fun getCustomProber(): UsbSerialProber {
        val customTable = ProbeTable()
        customTable.addProduct(0x0483, 0xDF11, CdcAcmSerialDriver::class.java)
        customTable.addProduct(0x0483, 0x3748, CdcAcmSerialDriver::class.java)
        customTable.addProduct(0x0483, 0x5740, CdcAcmSerialDriver::class.java)
        return UsbSerialProber(customTable)
    }

    private fun connectToSerial(usbDevice: UsbDevice) {
        var driver: UsbSerialDriver? = null
        try {
            driver = getCustomProber().probeDevice(usbDevice)
        } catch (ignored: Exception) { }

        if (driver == null) {
            driver = UsbSerialProber.getDefaultProber().probeDevice(usbDevice)
        }
        if (driver == null) {
            driver = CdcAcmSerialDriver(usbDevice)
        }

        val ports = driver.ports
        if (ports.isEmpty()) {
            onStatusChanges?.invoke(
                _root_ide_package_.com.bluestudio.manager.model.ConnectionStatus.Error(
                    _root_ide_package_.com.bluestudio.manager.model.ConnectionError.CANT_OPEN_PORT))
            return
        }

        val connection = usbManager.openDevice(usbDevice)
        if (connection == null) {
            onStatusChanges?.invoke(
                _root_ide_package_.com.bluestudio.manager.model.ConnectionStatus.Error(
                    _root_ide_package_.com.bluestudio.manager.model.ConnectionError.CANT_OPEN_PORT))
            return
        }

        port = ports[0]

        try {
            port?.open(connection)
            port?.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            port?.dtr = true
            port?.rts = true
        } catch (e: IOException) {
            disconnect()
            onStatusChanges?.invoke(
                _root_ide_package_.com.bluestudio.manager.model.ConnectionStatus.Error(
                    _root_ide_package_.com.bluestudio.manager.model.ConnectionError.CANT_OPEN_PORT))
            return
        }

        serialInputOutputManager = SerialInputOutputManager(port, this)
        serialInputOutputManager?.start()

        if (isPortOpen) {
            storeProductId(usbDevice.productId)
            onStatusChanges?.invoke(_root_ide_package_.com.bluestudio.manager.model.ConnectionStatus.Connected(usbDevice.toMicroDevice()))
        } else {
            disconnect()
        }
    }

    override fun onNewData(bytes: ByteArray?) {
        if (bytes == null || bytes.isEmpty()) return
        val data = String(bytes, Charsets.UTF_8)

        mainHandler.post {
            when (executionMode) {
                ExecutionMode.SCRIPT -> {
                    syncData.append(data)
                    if (_root_ide_package_.com.bluestudio.manager.managers.CommandsManager.isSilentExecutionDone(
                            data
                        ) || _root_ide_package_.com.bluestudio.manager.managers.CommandsManager.isSilentExecutionDone(
                            syncData.toString()
                        )
                    ) {
                        val result =
                            _root_ide_package_.com.bluestudio.manager.managers.CommandsManager.trimSilentResult(
                                syncData.toString()
                            )
                        onReadSync?.invoke(result)
                    }
                }
                ExecutionMode.INTERACTIVE -> {
                    if (data.isNotEmpty()) {
                        // *** تغییر مهم: ارسال به تمام شنونده‌ها ***
                        dataListeners.forEach { listener ->
                            listener.invoke(data)
                        }
                    }
                }
            }
        }
    }

    override fun onRunError(e: Exception?) {
        mainHandler.post {
            disconnect()
        }
    }

    private fun removeEnding(input: String): String {
        return input
    }

    private inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? = when {
        SDK_INT >= 33 -> getParcelableExtra(key, T::class.java)
        else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
    }

    private fun removeProduct(productId: Int) {
        supportedProducts.remove(productId)
        storeProducts()
    }

    private fun storeProductId(productId: Int) {
        supportedProducts.add(productId)
        storeProducts()
    }

    private fun storeProducts() {
        val json = Json.encodeToString(supportedProducts)
        val sharedPref = activity.getPreferences(Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putString("products", json)
            apply()
        }
    }

    private fun getProducts() {
        val sharedPref = activity.getPreferences(Context.MODE_PRIVATE) ?: return
        val json = sharedPref.getString("products", "").orEmpty()
        if (json.isNotEmpty()) {
            try {
                supportedProducts = Json.decodeFromString(json)
            } catch (ignored: Exception) {}
        }
    }
}