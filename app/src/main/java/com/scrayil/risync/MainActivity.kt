package com.scrayil.risync

import android.app.Activity
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

class MainActivity : Activity() {
    data class GuiConfig(val url: String, val apiKey: String?)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)

        val start = Button(this).apply {
            text = "Start Syncthing"
            setOnClickListener {
                startForegroundService(Intent(this@MainActivity, SyncthingService::class.java))
            }
        }

        val stop = Button(this).apply {
            text = "Stop Syncthing"
            setOnClickListener {
                stopService(Intent(this@MainActivity, SyncthingService::class.java))
            }
        }

        val openGui = Button(this).apply {
            text = "Open GUI"
            setOnClickListener {
                val config = readGuiConfig()
                if (config == null) {
                    Toast.makeText(this@MainActivity, "Config not found", Toast.LENGTH_SHORT).show()
                } else {
                    startActivity(Intent(Intent.ACTION_VIEW, config.url.toUri()))
                }
            }
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(start)
            addView(stop)
            addView(openGui)
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