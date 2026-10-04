package com.example.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

sealed class AuthResult {
    object SuccessNewUser : AuthResult()
    object SuccessExistingUser : AuthResult()
    object Cancelled : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class GoogleAuthService(private val context: Context) {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val credentialManager by lazy { CredentialManager.create(context) }

    suspend fun signInWithGoogle(): AuthResult {
        try {
            val resourceId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            val webClientId = if (resourceId != 0) {
                context.getString(resourceId)
            } else {
                "1001057390900-dl65vs7tp3n2f4i6oc3u1meg0lebohe6.apps.googleusercontent.com"
            }

            val rawNonce = UUID.randomUUID().toString()
            val bytes = rawNonce.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            val hashedNonce = digest.joinToString("") { "%02x".format(it) }

            val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setNonce(hashedNonce)
                .build()

            val request: GetCredentialRequest = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result: GetCredentialResponse = credentialManager.getCredential(
                request = request,
                context = context,
            )

            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                try {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                    
                    // Firebase Authenticate
                    val authResult = auth.signInWithCredential(authCredential).await()
                    val user = authResult.user
                    
                    if (user != null) {
                        try {
                            val uid = user.uid
                            val db = FirebaseFirestore.getInstance()
                            val userDocRef = db.collection("users").document(uid)
                            val doc = userDocRef.get().await()
                            if (!doc.exists()) {
                                val email = user.email ?: ""
                                val displayName = user.displayName ?: if (email.isNotEmpty()) email.substringBefore("@") else "Usuario"
                                val username = if (email.isNotEmpty()) email.substringBefore("@").lowercase().replace(Regex("[^a-z0-9_]"), "") else "user_${uid.take(5)}"
                                val photoUrl = user.photoUrl?.toString() ?: ""
                                val newProfile = mapOf(
                                    "uid" to uid,
                                    "username" to username,
                                    "displayName" to displayName,
                                    "photoUrl" to photoUrl,
                                    "bio" to "",
                                    "followersCount" to 0,
                                    "followingCount" to 0,
                                    "suspended" to false,
                                    "accountType" to "USER",
                                    "email" to email
                                )
                                userDocRef.set(newProfile).await()
                            }
                        } catch (e: Exception) {
                            Log.w("GoogleAuth", "Failed to ensure Firestore user profile", e)
                        }
                        return AuthResult.SuccessNewUser
                    } else {
                        return AuthResult.Error("Error: Firebase User es nulo.")
                    }
                    
                } catch (e: GoogleIdTokenParsingException) {
                    Log.e("GoogleAuth", "Token inválido", e)
                    return AuthResult.Error("Token de Google inválido.")
                }
            } else {
                return AuthResult.Error("Tipo de credencial no esperado.")
            }
        } catch (e: androidx.credentials.exceptions.NoCredentialException) {
            try {
                val intent = android.content.Intent(android.provider.Settings.ACTION_ADD_ACCOUNT).apply {
                    putExtra(android.provider.Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return AuthResult.Error("Agrega tu cuenta de Google y vuelve a presionar el botón.")
            } catch (intentException: Exception) {
                return AuthResult.Error("No se encontraron cuentas de Google en este dispositivo.")
            }
        } catch (e: GetCredentialCancellationException) {
            return AuthResult.Cancelled
        } catch (e: Exception) {
            Log.e("GoogleAuth", "Sign In Failed", e)
            return AuthResult.Error(e.localizedMessage ?: "Error desconocido.")
        }
    }

    suspend fun signOut() {
        auth.signOut()
        try {
            credentialManager.clearCredentialState(androidx.credentials.ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("GoogleAuth", "Clear credential state failed", e)
        }
    }
}
