package com.example.myapplication;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;

public class NetworkUtil {
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            Network network = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                network = connectivityManager.getActiveNetwork();
            }
            if (network != null) {
                NetworkCapabilities capabilities = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    capabilities = connectivityManager.getNetworkCapabilities(network);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    return capabilities != null &&
                            (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
                }
            }
        }
        return false; // No active network
    }
}