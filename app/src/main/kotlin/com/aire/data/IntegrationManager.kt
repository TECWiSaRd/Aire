package com.aire.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.aire.claude.AssistantAction

/**
 * Executes interactive actions suggested by the AI, integrating with Android system services.
 */
class IntegrationManager(private val context: Context) {

    /**
     * Executes the provided [action].
     */
    fun execute(action: AssistantAction) {
        try {
            when (action.type) {
                "MAPS_SEARCH" -> launchMapsSearch(action.data["query"] ?: "restaurants")
                // Future integrations like ADD_CALENDAR, ADD_CONTACT will go here
                else -> Log.w("IntegrationManager", "Unknown action type: ${action.type}")
            }
        } catch (e: Exception) {
            Log.e("IntegrationManager", "Failed to execute action: ${action.type}", e)
        }
    }

    private fun launchMapsSearch(query: String) {
        val uri = "geo:0,0?q=${Uri.encode(query)}".toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // Fallback to any app that can handle the geo intent
            val fallbackIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallbackIntent)
        }
    }
}
