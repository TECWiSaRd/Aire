package com.aire.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.util.Log
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import java.util.Locale

/**
 * Data class representing the device's location with human-readable context.
 */
data class DeviceLocation(
    val latitude: Double,
    val longitude: Double,
    val name: String? = null,
)

/**
 * Provides access to the device's current location and reverse geocoding.
 */
class LocationProvider(private val context: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    /**
     * Fetches the current location and attempts to reverse geocode it to a human-readable name.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): DeviceLocation? {
        return try {
            val cancellationTokenSource = CancellationTokenSource()
            val location: Location? = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).await()

            location?.let {
                DeviceLocation(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    name = getAddressName(it.latitude, it.longitude)
                )
            }
        } catch (e: Exception) {
            Log.e("LocationProvider", "Failed to get location", e)
            null
        }
    }

    private fun getAddressName(latitude: Double, longitude: Double): String? {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            val address = addresses?.firstOrNull()
            
            // Format: "Neighborhood, City" or "City, Country"
            listOfNotNull(
                address?.subLocality ?: address?.locality,
                address?.adminArea ?: address?.countryName
            ).joinToString(", ").takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.e("LocationProvider", "Failed to geocode location", e)
            null
        }
    }
}
