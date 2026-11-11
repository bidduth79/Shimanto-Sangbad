package com.example.widget

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.example.R

/**
 * Invisible stop between the widget's WhatsApp button and WhatsApp itself.
 *
 * A widget cannot start WhatsApp's "send to" screen directly: Glance stamps every list item's
 * intent with a data URI, which erases the "text/plain" type WhatsApp's share screen is matched by
 * (seen on 2026-11-11: the launch failed with ActivityNotFoundException). This activity receives
 * the text, builds the proper share intent and finishes at once, without showing anything.
 */
class WhatsAppShareActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = intent?.getStringExtra(EXTRA_TEXT)
        if (!text.isNullOrBlank() && !sendTo(WHATSAPP, text) && !sendTo(WHATSAPP_BUSINESS, text)) {
            Toast.makeText(this, R.string.widget_whatsapp_missing, Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    private fun sendTo(packageName: String, text: String): Boolean = try {
        startActivity(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage(packageName)
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

    companion object {
        const val EXTRA_TEXT = "share_text"
        private const val WHATSAPP = "com.whatsapp"
        private const val WHATSAPP_BUSINESS = "com.whatsapp.w4b"
    }
}
