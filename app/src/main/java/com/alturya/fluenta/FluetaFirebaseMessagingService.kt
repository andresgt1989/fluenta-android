package com.alturya.fluenta

import android.util.Log
import com.alturya.fluenta.data.TokenStore
import com.alturya.fluenta.network.ApiClient
import com.alturya.fluenta.network.FcmRegisterBody
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class FluetaFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Called when a new FCM token is generated (first install or after token rotation).
     * We upload it to the backend so the server can push lesson reminders and streak alerts.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        serviceScope.launch {
            try {
                val authToken = TokenStore.getToken(applicationContext).firstOrNull()
                if (authToken != null) {
                    ApiClient.setToken(authToken)
                    ApiClient.api.registerFcmToken(FcmRegisterBody(fcmToken = token, platform = "android"))
                    Log.d("FCM", "Token registered with backend")
                } else {
                    // No auth token yet — token will be registered on next login via LoginViewModel.
                    Log.d("FCM", "No auth token yet; FCM token saved locally for next login")
                }
                // Persist locally so LoginViewModel can register it after auth if needed.
                TokenStore.saveFcmToken(applicationContext, token)
            } catch (e: Exception) {
                Log.e("FCM", "Failed to register token", e)
            }
        }
    }

    /**
     * Called when the app is in the foreground and receives a push message.
     * The backend currently sends lesson reminders, streak alerts, and weekly recaps.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        // Foreground notifications: log and let the system notification channel handle them.
        // The notification payload sent by the backend already includes title + body.
        Log.d("FCM", "Push received: ${message.notification?.title} — ${message.notification?.body}")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
