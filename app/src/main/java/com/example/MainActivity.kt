package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.navigation.MainNavigationScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.data.UpdateRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigationScreen()
                }
            }
        }

        // Background update check (Auto-sync automation)
        checkUpdatesInBackground()
    }

    private fun checkUpdatesInBackground() {
        val updateRepo = UpdateRepository()
        val repoPath = "menajohan90-cell/Mi-app-de-tiktok2.0"
        
        CoroutineScope(Dispatchers.Main).launch {
            while(true) {
                try {
                    val result = updateRepo.getGitHubLatestRelease(repoPath)
                    result.onSuccess { url ->
                        // Si encontramos algo en GitHub, avisamos al usuario una sola vez
                        // Nota: En una app real compararíamos versionCode, aquí avisamos para prueba
                        android.util.Log.d("MainActivity", "GitHub Update found: $url")
                        // Podríamos mostrar un banner o notificación interna aquí
                    }
                } catch (e: Exception) {}
                delay(300000) // Cada 5 minutos
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateOnlinePresence(true)
    }

    override fun onPause() {
        super.onPause()
        updateOnlinePresence(false)
    }

    private fun updateOnlinePresence(isOnline: Boolean) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        try {
            val data = mapOf(
                "isOnline" to isOnline,
                "lastActive" to System.currentTimeMillis()
            )
            FirebaseFirestore.getInstance().collection("users").document(uid).set(data, SetOptions.merge())
        } catch (e: Exception) {}
    }
}
