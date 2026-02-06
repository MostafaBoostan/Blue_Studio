package micro.repl.ma7moud3ly.dfu

import android.util.Log
import kotlinx.coroutines.delay
import kotlin.math.min

class DfuSeEngine(private val usb: Usb) {

    // دستورات استاندارد
    private val DFU_DNLOAD = 1
    private val DFU_GETSTATUS = 3
    private val DFU_CLRSTATUS = 4
    private val DFU_ABORT = 6

    // وضعیت‌ها
    private val STATE_DFU_IDLE = 2
    private val STATE_DFU_DNLOAD_IDLE = 5
    private val STATE_DFU_ERROR = 10

    private val REQ_HOST_TO_DEVICE = 0x21
    private val REQ_DEVICE_TO_HOST = 0xA1

    data class DfuStatus(val state: Int, val pollTimeout: Long, val status: Int)

    // --- FLASH FIRMWARE ---
    suspend fun flashFirmware(fileContent: ByteArray, onProgress: (String) -> Unit): Boolean {
        if (!usb.isConnected()) {
            onProgress("Error: Device disconnected.")
            return false
        }
        try {
            // پاکسازی وضعیت‌های قبلی
            usb.controlTransfer(REQ_HOST_TO_DEVICE, DFU_ABORT, 0, 0, null, 0)
            ensureIdle()

            val segments = parseDfuFile(fileContent)
            if (segments.isEmpty()) {
                onProgress("Binary file detected. Flashing to 0x08000000")
                segments.add(DfuSegment(0x08000000, fileContent))
            } else {
                onProgress("File parsed: ${segments.size} segments found.")
            }

            for ((index, seg) in segments.withIndex()) {
                onProgress("Segment ${index + 1}: Erasing sector 0x${Integer.toHexString(seg.address)}...")

                // پاک کردن سکتور (Smart Erase)
                if (!eraseSector(seg.address)) {
                    onProgress("Erase Failed! (Is chip write-protected?)")
                    return false
                }

                onProgress("Writing ${seg.data.size} bytes...")
                if (!writeBlock(seg.address, seg.data)) {
                    onProgress("Write Failed.")
                    return false
                }
            }
            onProgress("Done. Resetting...")
            leaveDfuMode()
            return true
        } catch (e: Exception) {
            onProgress("Exception: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    // --- FULL CHIP ERASE (بر اساس لاجیک قدیمی GitHub) ---
    suspend fun fullChipErase(onProgress: (String) -> Unit): Boolean {
        if (!usb.isConnected()) {
            onProgress("Error: Not connected.")
            return false
        }
        try {
            onProgress("Preparing Mass Erase...")
            // 1. اول مطمئن میشویم وضعیت IDLE است (مثل کد قدیمی)
            ensureIdle()

            // 2. ارسال دستور Mass Erase (0x41)
            val cmd = ByteArray(1)
            cmd[0] = 0x41.toByte()

            onProgress("Sending Mass Erase Command...")
            val ret = usb.controlTransfer(REQ_HOST_TO_DEVICE, DFU_DNLOAD, 0, 0, cmd, 1)

            if (ret < 0) {
                onProgress("Command Transmission Failed.")
                return false
            }

            // 3. حلقه چک کردن وضعیت (دقیقاً مثل کد قدیمی جاوا)
            // کد قدیمی فقط وضعیت را میخواند و اگر ارور بود، خارج میشد.
            val startTime = System.currentTimeMillis()
            onProgress("Erasing... (Please wait)")

            while (System.currentTimeMillis() - startTime < 40000) { // 40 ثانیه تایم اوت
                val statusBuffer = ByteArray(6)
                usb.controlTransfer(REQ_DEVICE_TO_HOST, DFU_GETSTATUS, 0, 0, statusBuffer, 6)

                val state = statusBuffer[4].toInt() and 0xFF

                if (state == STATE_DFU_ERROR) {
                    // اگر ارور داد، یعنی پاک کردن شکست خورد. وضعیت را پاک کن و فالس برگردان
                    usb.controlTransfer(REQ_HOST_TO_DEVICE, DFU_CLRSTATUS, 0, 0, null, 0)
                    onProgress("Mass Erase FAILED (Device reported Error).")
                    return false
                }

                if (state == STATE_DFU_DNLOAD_IDLE || state == STATE_DFU_IDLE) {
                    onProgress("Mass Erase SUCCESSFUL!")
                    return true
                }

                // تاخیر ساده مثل کد قدیمی
                delay(100)
            }

            onProgress("Mass Erase Timed Out.")
            return false

        } catch (e: Exception) {
            onProgress("Error: ${e.message}")
            return false
        }
    }

    // --- توابع کمکی ---

    private suspend fun eraseSector(address: Int): Boolean {
        // دستور Erase سکتور: 0x41 + آدرس
        val buffer = ByteArray(5)
        buffer[0] = 0x41.toByte()
        buffer[1] = (address and 0xFF).toByte()
        buffer[2] = ((address shr 8) and 0xFF).toByte()
        buffer[3] = ((address shr 16) and 0xFF).toByte()
        buffer[4] = ((address shr 24) and 0xFF).toByte()

        usb.controlTransfer(REQ_HOST_TO_DEVICE, DFU_DNLOAD, 0, 0, buffer, 5)
        return waitForIdle(5000)
    }

    private suspend fun writeBlock(startAddress: Int, data: ByteArray): Boolean {
        if (!setAddress(startAddress)) return false
        var offset = 0
        var blockNum = 2
        val chunkSize = 2048

        while (offset < data.size) {
            val len = min(chunkSize, data.size - offset)
            val buffer = ByteArray(len)
            System.arraycopy(data, offset, buffer, 0, len)

            val ret = usb.controlTransfer(REQ_HOST_TO_DEVICE, DFU_DNLOAD, blockNum, 0, buffer, len)
            if (ret < 0) return false

            if (!waitForIdle(1000)) return false

            offset += len
            blockNum++
        }
        return true
    }

    private suspend fun setAddress(address: Int): Boolean {
        val buffer = ByteArray(5)
        buffer[0] = 0x21.toByte()
        buffer[1] = (address and 0xFF).toByte()
        buffer[2] = ((address shr 8) and 0xFF).toByte()
        buffer[3] = ((address shr 16) and 0xFF).toByte()
        buffer[4] = ((address shr 24) and 0xFF).toByte()

        usb.controlTransfer(REQ_HOST_TO_DEVICE, DFU_DNLOAD, 0, 0, buffer, 5)
        return waitForIdle(1000)
    }

    private suspend fun leaveDfuMode() {
        setAddress(0x08000000)
        usb.controlTransfer(REQ_HOST_TO_DEVICE, DFU_DNLOAD, 0, 0, null, 0)
        try {
            val buf = ByteArray(6)
            usb.controlTransfer(REQ_DEVICE_TO_HOST, DFU_GETSTATUS, 0, 0, buf, 6)
        } catch (e: Exception) {}
    }

    private fun ensureIdle(): Boolean {
        // روش ساده و خشن برای ریست کردن وضعیت: CLRSTATUS
        val buf = ByteArray(6)
        usb.controlTransfer(REQ_DEVICE_TO_HOST, DFU_GETSTATUS, 0, 0, buf, 6)
        val state = buf[4].toInt() and 0xFF

        if (state == STATE_DFU_ERROR) {
            usb.controlTransfer(REQ_HOST_TO_DEVICE, DFU_CLRSTATUS, 0, 0, null, 0)
        }
        return true
    }

    private suspend fun waitForIdle(timeout: Long): Boolean {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeout) {
            val buf = ByteArray(6)
            usb.controlTransfer(REQ_DEVICE_TO_HOST, DFU_GETSTATUS, 0, 0, buf, 6)

            val state = buf[4].toInt() and 0xFF
            val pollTime = (buf[1].toLong() and 0xFF) or ((buf[2].toLong() and 0xFF) shl 8)

            if (state == STATE_DFU_DNLOAD_IDLE || state == STATE_DFU_IDLE) return true
            if (state == STATE_DFU_ERROR) {
                usb.controlTransfer(REQ_HOST_TO_DEVICE, DFU_CLRSTATUS, 0, 0, null, 0)
                return false
            }
            delay(pollTime.coerceAtLeast(10))
        }
        return false
    }

    // --- PARSER (رفع مشکل اعداد منفی) ---
    data class DfuSegment(val address: Int, val data: ByteArray)

    private fun parseDfuFile(bytes: ByteArray): MutableList<DfuSegment> {
        val list = mutableListOf<DfuSegment>()
        if (bytes.size > 10 && String(bytes.copyOfRange(0, 5)) == "DfuSe") {
            var pos = 0
            while (pos < bytes.size - 10) {
                if (pos + 6 <= bytes.size && String(bytes.copyOfRange(pos, pos + 6)) == "Target") {
                    // استفاده از & 0xFF برای جلوگیری از اعداد منفی
                    val nbElements = (bytes[pos + 267].toInt() and 0xFF) or ((bytes[pos + 268].toInt() and 0xFF) shl 8)
                    var elementPos = pos + 274

                    for (i in 0 until nbElements) {
                        if (elementPos + 8 > bytes.size) break

                        val addr = (bytes[elementPos].toInt() and 0xFF) or
                                ((bytes[elementPos+1].toInt() and 0xFF) shl 8) or
                                ((bytes[elementPos+2].toInt() and 0xFF) shl 16) or
                                ((bytes[elementPos+3].toInt() and 0xFF) shl 24)

                        val size = (bytes[elementPos+4].toInt() and 0xFF) or
                                ((bytes[elementPos+5].toInt() and 0xFF) shl 8) or
                                ((bytes[elementPos+6].toInt() and 0xFF) shl 16) or
                                ((bytes[elementPos+7].toInt() and 0xFF) shl 24)

                        elementPos += 8
                        if (size > 0 && elementPos + size <= bytes.size) {
                            list.add(DfuSegment(addr, bytes.copyOfRange(elementPos, elementPos + size)))
                        }
                        elementPos += size
                    }
                    pos = elementPos
                } else { pos++ }
            }
        }
        return list
    }
}