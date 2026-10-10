package com.jjcreations.frequencychat

import android.Manifest
import android.app.Activity
import android.bluetooth.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.util.UUID
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private val appUuid: UUID = UUID.fromString("8c4b1c40-2b35-4a5a-9b2a-66dba77a3f01")
    private var adapter: BluetoothAdapter? = null
    private var socket: BluetoothSocket? = null
    private var server: BluetoothServerSocket? = null
    private var writer: PrintWriter? = null
    private lateinit var status: TextView
    private lateinit var channel: EditText
    private lateinit var deviceList: LinearLayout
    private lateinit var chatLog: TextView
    private lateinit var message: EditText
    private lateinit var scroll: ScrollView
    private var receiverRegistered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
        buildUi()
        requestBtPermissions()
        if (adapter == null) status.text = "This phone does not support Bluetooth."
        else status.text = "Offline mode • Bluetooth only • No internet required"
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 24, 22, 18)
            setBackgroundColor(0xFF0B1220.toInt())
        }
        fun label(text: String, size: Float, color: Int = 0xFFFFFFFF.toInt()) =
            TextView(this).apply { this.text = text; textSize = size; setTextColor(color) }
        root.addView(label("JJ FREQUENCY CHAT", 24f))
        root.addView(label("Offline Bluetooth prototype", 13f, 0xFF93A4BC.toInt()))
        status = label("Checking Bluetooth…", 13f, 0xFF69D6B0.toInt())
        status.setPadding(0, 12, 0, 12)
        root.addView(status)
        channel = EditText(this).apply {
            hint = "Channel code (e.g. JJ-4821)"
            setSingleLine(true); setTextColor(-1); setHintTextColor(0xFF9BAAC0.toInt())
            setBackgroundColor(0xFF172338.toInt())
        }
        root.addView(channel, LinearLayout.LayoutParams(-1, 52))
        val row = LinearLayout(this)
        val host = Button(this).apply { text = "Create / Listen"; setOnClickListener { startListening() } }
        val scan = Button(this).apply { text = "Find Devices"; setOnClickListener { discoverDevices() } }
        row.addView(host, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(scan, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(row)
        root.addView(label("Nearby Bluetooth devices", 15f).apply { setPadding(0, 10, 0, 4) })
        deviceList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(deviceList)
        root.addView(label("Messages", 15f).apply { setPadding(0, 12, 0, 4) })
        scroll = ScrollView(this)
        chatLog = label("Not connected yet.\nBoth phones must use the same channel code.", 14f, 0xFFE4EAF3.toInt())
        chatLog.setPadding(12, 12, 12, 12)
        chatLog.setBackgroundColor(0xFF111C2E.toInt())
        scroll.addView(chatLog)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val sendRow = LinearLayout(this)
        message = EditText(this).apply {
            hint = "Write a message…"; setSingleLine(true); setTextColor(-1)
            setHintTextColor(0xFF9BAAC0.toInt()); setBackgroundColor(0xFF172338.toInt())
        }
        val send = Button(this).apply { text = "Send"; setOnClickListener { sendMessage() } }
        sendRow.addView(message, LinearLayout.LayoutParams(0, 54, 1f))
        sendRow.addView(send)
        root.addView(sendRow)
        root.addView(label("Range is nearby only. Channel code is a room label, not an FM frequency.", 11f, 0xFFFFC66D.toInt()).apply { setPadding(0, 8, 0, 0) })
        setContentView(root)
    }

    private fun requestBtPermissions() {
        val needed = if (Build.VERSION.SDK_INT >= 31) arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN
        ) else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val missing = needed.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) requestPermissions(missing.toTypedArray(), 42)
    }

    private fun allowed(): Boolean {
        if (Build.VERSION.SDK_INT >= 31) {
            return checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        }
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun channelCode(): String = channel.text.toString().trim().uppercase()

    private fun startListening() {
        if (!allowed()) { requestBtPermissions(); status.text = "Allow Bluetooth permissions, then try again."; return }
        if (channelCode().isBlank()) { channel.error = "Enter a channel code"; return }
        val bt = adapter ?: return
        try {
            if (!bt.isEnabled) { status.text = "Turn Bluetooth on in Android settings."; return }
            status.text = "Listening for another JJ Frequency Chat phone…"
            thread {
                try {
                    server = bt.listenUsingRfcommWithServiceRecord("JJ Frequency Chat", appUuid)
                    val incoming = server!!.accept()
                    runOnUiThread { status.text = "Device connected. Checking channel…" }
                    establish(incoming, true)
                } catch (e: Exception) {
                    runOnUiThread { status.text = "Listener stopped: ${e.message ?: "connection error"}" }
                }
            }
        } catch (e: SecurityException) { status.text = "Bluetooth permission required." }
    }

    private fun discoverDevices() {
        if (!allowed()) { requestBtPermissions(); return }
        if (channelCode().isBlank()) { channel.error = "Enter the same channel code on both phones"; return }
        val bt = adapter ?: return
        try {
            if (!bt.isEnabled) { status.text = "Turn Bluetooth on in Android settings."; return }
            deviceList.removeAllViews()
            val bonded = bt.bondedDevices
            if (bonded.isEmpty()) {
                status.text = "No paired devices. Pair both phones in Android Bluetooth settings first."
            } else {
                status.text = "Tap a paired device to connect."
                bonded.forEach { device ->
                    val b = Button(this).apply {
                        text = "${device.name ?: "Bluetooth device"} • ${device.address}"
                        setOnClickListener { connectTo(device) }
                    }
                    deviceList.addView(b)
                }
            }
        } catch (e: SecurityException) { status.text = "Bluetooth permission required." }
    }

    private fun connectTo(device: BluetoothDevice) {
        if (!allowed()) { requestBtPermissions(); return }
        status.text = "Connecting…"
        thread {
            try {
                adapter?.cancelDiscovery()
                val s = device.createRfcommSocketToServiceRecord(appUuid)
                s.connect()
                runOnUiThread { status.text = "Connected. Checking channel…" }
                establish(s, false)
            } catch (e: Exception) {
                runOnUiThread { status.text = "Could not connect. Pair both phones in Bluetooth settings first." }
            }
        }
    }

    private fun establish(s: BluetoothSocket, isServer: Boolean) {
        try {
            socket = s
            val out = PrintWriter(s.outputStream, true)
            val input = BufferedReader(InputStreamReader(s.inputStream))
            writer = out
            // Both sides exchange a channel handshake before messages are enabled.
            out.println("CHANNEL:${channelCode()}")
            val peer = input.readLine()
            if (peer != "CHANNEL:${channelCode()}") {
                s.close()
                runOnUiThread { status.text = "Channel mismatch. Enter the same code on both phones." }
                return
            }
            runOnUiThread { status.text = "Connected • Channel ${channelCode()} • Offline" ; append("System", "Secure channel match (channel code only; messages are not end-to-end encrypted).") }
            while (true) {
                val line = input.readLine() ?: break
                runOnUiThread { append("Peer", line) }
            }
            runOnUiThread { status.text = "Connection closed." }
        } catch (e: Exception) {
            runOnUiThread { status.text = "Connection ended." }
        }
    }

    private fun sendMessage() {
        val text = message.text.toString().trim()
        if (text.isEmpty()) return
        val w = writer
        if (w == null || socket?.isConnected != true) {
            status.text = "Connect to a phone first."
            return
        }
        w.println(text.replace("\n", " "))
        append("You", text)
        message.setText("")
    }

    private fun append(who: String, text: String) {
        chatLog.append("\n$who: $text")
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    override fun onDestroy() {
        try { socket?.close() } catch (_: Exception) {}
        try { server?.close() } catch (_: Exception) {}
        super.onDestroy()
    }
}
