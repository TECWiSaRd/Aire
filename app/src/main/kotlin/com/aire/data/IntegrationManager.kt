package com.aire.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.provider.ContactsContract
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
                "ADD_CALENDAR" -> launchAddCalendar(action.data)
                "ADD_CONTACT" -> launchAddContact(action.data)
                "OPEN_WEB" -> launchWebBrowser(action.data["url"])
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
            val fallbackIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallbackIntent)
        }
    }

    private fun launchAddCalendar(data: Map<String, String>) {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            setData(CalendarContract.Events.CONTENT_URI)
            putExtra(CalendarContract.Events.TITLE, data["title"])
            putExtra(CalendarContract.Events.DESCRIPTION, data["description"])
            putExtra(CalendarContract.Events.EVENT_LOCATION, data["location"])
            
            data["beginTime"]?.toLongOrNull()?.let { 
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, it) 
            }
            data["endTime"]?.toLongOrNull()?.let { 
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, it) 
            }
            
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    private fun launchAddContact(data: Map<String, String>) {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            type = ContactsContract.Contacts.CONTENT_TYPE
            putExtra(ContactsContract.Intents.Insert.NAME, data["name"])
            putExtra(ContactsContract.Intents.Insert.PHONE, data["phone"])
            putExtra(ContactsContract.Intents.Insert.EMAIL, data["email"])
            putExtra(ContactsContract.Intents.Insert.NOTES, data["notes"])
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    private fun launchWebBrowser(url: String?) {
        val uri = url?.toUri() ?: return
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
