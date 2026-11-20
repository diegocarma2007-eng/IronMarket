package com.ironmarket.app.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

/** Resultado de la geolocalización: coordenadas y dirección legible. */
data class Place(val lat: Double, val lng: Double, val address: String)

/**
 * Geolocalización con las APIs de Google Play Services.
 * Usa FusedLocationProviderClient para obtener la ubicación actual y Geocoder para
 * convertir las coordenadas en una dirección (geocodificación inversa).
 */
object LocationHelper {

    /** Obtiene la ubicación actual. Requiere que el permiso de ubicación ya esté concedido. */
    @SuppressLint("MissingPermission")
    suspend fun currentPlace(context: Context): Place? {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val location = client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token).await()
            ?: client.lastLocation.await() ?: return null
        val address = withContext(Dispatchers.IO) {
            runCatching {
                @Suppress("DEPRECATION")
                Geocoder(context, Locale("es", "CO")).getFromLocation(location.latitude, location.longitude, 1)
                    ?.firstOrNull()?.let { a ->
                        listOfNotNull(a.thoroughfare?.let { t -> listOfNotNull(t, a.subThoroughfare).joinToString(" # ") },
                            a.subLocality, a.locality).joinToString(", ").ifEmpty { a.getAddressLine(0) }
                    }
            }.getOrNull()
        } ?: "%.5f, %.5f".format(location.latitude, location.longitude)
        return Place(location.latitude, location.longitude, address)
    }
}
