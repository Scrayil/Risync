package com.scrayil.risync

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Xml
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import org.xmlpull.v1.XmlPullParser
import java.io.File
import androidx.core.net.toUri
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat

class MainActivity : Activity() {
    data class GuiConfig(val url: String, val apiKey: String?)

    fun requestStoragePermission() {
        if (!Environment.isExternalStorageManager()) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data = String.format("package:%s", packageName).toUri()
                startActivity(intent)
            } catch (_: Exception) {
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                startActivity(intent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        requestStoragePermission()

        val start = Button(this).apply {
            text = "Start Syncthing"
            setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.play_24, 0, 0, 0)
            compoundDrawablePadding = 24
            setOnClickListener {
                startForegroundService(Intent(this@MainActivity, SyncthingService::class.java))
            }
        }

        val stop = Button(this).apply {
            text = "Stop Syncthing"
            setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.stop_24, 0, 0, 0)
            compoundDrawablePadding = 24
            setOnClickListener {
                stopService(Intent(this@MainActivity, SyncthingService::class.java))
            }
        }

        val openGui = Button(this).apply {
            text = "Open GUI"
            setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.globe_24, 0, 0, 0)
            compoundDrawablePadding = 24
            setOnClickListener {
                val config = readGuiConfig()
                if (config == null) {
                    Toast.makeText(this@MainActivity, "Config not found", Toast.LENGTH_SHORT).show()
                } else {
                    startActivity(Intent(Intent.ACTION_VIEW, config.url.toUri()))
                }
            }
        }

        val basePath = Environment.getExternalStorageDirectory().absolutePath
        val copyPath = Button(this).apply {
            text = "Copy base path"
            setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.copy_24, 0, 0, 0)
            compoundDrawablePadding = 24
        }
        copyPath.setOnClickListener {
            val clipboard = ContextCompat.getSystemService(this, ClipboardManager::class.java)
            val clip = ClipData.newPlainText("Path", basePath)
            clipboard?.setPrimaryClip(clip)
            Toast.makeText(this, "Copied: $basePath", Toast.LENGTH_SHORT).show()
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
            addView(start)
            addView(stop)
            addView(openGui)
            addView(copyPath)
        }

        setContentView(layout)
    }

    private fun readGuiConfig(): GuiConfig? {
        val file = File(filesDir, "syncthing/config.xml")
        if (!file.exists()) return null

        var tls = false
        var address: String? = null
        var apiKey: String? = null
        var inGui = false

        file.inputStream().use { stream ->
            val parser = Xml.newPullParser()
            parser.setInput(stream, null)

            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                when (parser.eventType) {
                    XmlPullParser.START_TAG -> when (parser.name) {
                        "gui" -> {
                            inGui = true
                            tls = parser.getAttributeValue(null, "tls")?.toBoolean() ?: false
                        }
                        "address" -> if (inGui && address == null) address = parser.nextText()
                        "apikey" -> if (inGui) apiKey = parser.nextText()
                    }
                    XmlPullParser.END_TAG -> if (parser.name == "gui") inGui = false
                }
            }
        }

        val addr = address ?: return null
        return GuiConfig(if (tls) "https://$addr" else "http://$addr", apiKey)
    }
}