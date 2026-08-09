package com.pushprajcore.bfit.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Utility for checking network connectivity before making API calls.
 * Prevents opaque timeout errors on network-dependent features.
 */
object NetworkUtils {

    /**
     * Returns true if the device has an active internet connection.
     */
    fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: SecurityException) {
            // Gracefully handle missing ACCESS_NETWORK_STATE permission
            true // Assume network is available; actual call will fail with a clearer error
        }
    }
}
