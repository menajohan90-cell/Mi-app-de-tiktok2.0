package com.example.auth

import android.util.Log
import android.util.Patterns
import com.example.data.ProfileResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private fun formatFirebasePassword(pass: String): String {
        return if (pass.length < 6) {
            pass.padEnd(6, '_')
        } else {
            pass
        }
    }

    suspend fun register(email: String, username: String, pass: String): AuthResult {
        val cleanEmail = email.trim()
        val cleanUsername = username.trim().lowercase().replace("@", "")
        val cleanPass = pass.trim()

        if (cleanEmail.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            return AuthResult.Error("Por favor ingresa un correo electrónico válido.")
        }
        if (cleanUsername.isEmpty() || cleanUsername.length < 3) {
            return AuthResult.Error("El nombre de usuario debe tener al menos 3 caracteres.")
        }
        if (!cleanUsername.matches(Regex("^[a-z0-9._]+$"))) {
            return AuthResult.Error("El nombre de usuario solo puede contener letras, números, puntos o guiones bajos.")
        }
        if (cleanPass.isEmpty()) {
            return AuthResult.Error("Por favor ingresa una contraseña.")
        }

        return try {
            val firebasePass = formatFirebasePassword(cleanPass)
            val authResult = auth.createUserWithEmailAndPassword(cleanEmail, firebasePass).await()
            val user = authResult.user ?: return AuthResult.Error("No se pudo crear la cuenta de usuario.")
            val uid = user.uid

            // Update Firebase Auth user display name
            try {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(cleanUsername)
                    .build()
                user.updateProfile(profileUpdates).await()
            } catch (e: Exception) {
                Log.w("AuthRepository", "Failed to update auth display name: ${e.message}")
            }

            // Check if username is already taken by another user (now authenticated)
            try {
                val existingUsername = db.collection("users")
                    .whereEqualTo("username", cleanUsername)
                    .limit(2)
                    .get()
                    .await()

                val isTaken = existingUsername.documents.any { it.id != uid }
                if (isTaken) {
                    try {
                        user.delete().await()
                    } catch (delEx: Exception) {
                        Log.w("AuthRepository", "Failed to delete user after duplicate username", delEx)
                    }
                    return AuthResult.Error("El nombre de usuario '$cleanUsername' ya está en uso. Por favor elige otro.")
                }
            } catch (e: Exception) {
                Log.w("AuthRepository", "Could not check username uniqueness: ${e.message}")
            }

            // Save user profile in Firestore
            try {
                val profileData = hashMapOf<String, Any>(
                    "uid" to uid,
                    "username" to cleanUsername,
                    "displayName" to cleanUsername,
                    "photoUrl" to "",
                    "bio" to "",
                    "followersCount" to 0,
                    "followingCount" to 0,
                    "suspended" to false,
                    "accountType" to "USER",
                    "email" to cleanEmail
                )
                db.collection("users").document(uid).set(profileData, SetOptions.merge()).await()
            } catch (e: Exception) {
                Log.e("AuthRepository", "Error saving profile document in Firestore: ${e.message}", e)
            }

            AuthResult.SuccessNewUser
        } catch (e: FirebaseAuthUserCollisionException) {
            AuthResult.Error("Ya existe una cuenta con este correo electrónico.")
        } catch (e: FirebaseAuthWeakPasswordException) {
            AuthResult.Error("La contraseña es muy débil. Debe tener al menos 6 caracteres.")
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            AuthResult.Error("El formato del correo electrónico no es válido.")
        } catch (e: com.google.firebase.FirebaseNetworkException) {
            AuthResult.Error("Error de red. Verifica tu conexión a internet.")
        } catch (e: Exception) {
            Log.e("AuthRepository", "Registration error", e)
            AuthResult.Error(e.localizedMessage ?: "Error al registrar la cuenta.")
        }
    }

    suspend fun login(identifier: String, pass: String): AuthResult {
        val cleanId = identifier.trim()
        val cleanPass = pass

        if (cleanId.isEmpty() || cleanPass.isEmpty()) {
            return AuthResult.Error("Por favor completa todos los campos.")
        }

        return try {
            val targetEmail = if (cleanId.contains("@")) {
                cleanId.lowercase()
            } else {
                val cleanUsername = cleanId.lowercase().replace("@", "")
                try {
                    val query = db.collection("users")
                        .whereEqualTo("username", cleanUsername)
                        .limit(1)
                        .get()
                        .await()

                    if (query.isEmpty) {
                        return AuthResult.Error("No se encontró ningún usuario con el nombre de usuario '$cleanId'.")
                    }
                    val userDoc = query.documents.first()
                    val emailFromDoc = userDoc.getString("email")
                    if (emailFromDoc.isNullOrBlank()) {
                        return AuthResult.Error("Por favor inicia sesión con tu correo electrónico.")
                    }
                    emailFromDoc.lowercase()
                } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
                    Log.w("AuthRepository", "Firestore unauthenticated query restricted: ${e.message}")
                    return AuthResult.Error("Por seguridad, por favor inicia sesión utilizando tu correo electrónico.")
                } catch (e: Exception) {
                    Log.w("AuthRepository", "Error resolving username to email: ${e.message}")
                    return AuthResult.Error("Por favor inicia sesión con tu correo electrónico.")
                }
            }

            val firebasePass = formatFirebasePassword(cleanPass)
            val authResult = auth.signInWithEmailAndPassword(targetEmail, firebasePass).await()
            val user = authResult.user ?: return AuthResult.Error("No se pudo iniciar sesión.")

            // Ensure profile exists in Firestore
            try {
                val userDocRef = db.collection("users").document(user.uid)
                val doc = userDocRef.get().await()
                if (!doc.exists()) {
                    val email = user.email ?: ""
                    val defaultUsername = if (email.isNotEmpty()) email.substringBefore("@").lowercase().replace(Regex("[^a-z0-9_]"), "") else "user_${user.uid.take(5)}"
                    val newProfile = hashMapOf<String, Any>(
                        "uid" to user.uid,
                        "username" to defaultUsername,
                        "displayName" to (user.displayName ?: defaultUsername),
                        "photoUrl" to (user.photoUrl?.toString() ?: ""),
                        "bio" to "",
                        "followersCount" to 0,
                        "followingCount" to 0,
                        "suspended" to false,
                        "accountType" to "USER",
                        "email" to email
                    )
                    userDocRef.set(newProfile, SetOptions.merge()).await()
                }
            } catch (e: Exception) {
                Log.w("AuthRepository", "Could not check/create profile document on login: ${e.message}")
            }

            AuthResult.SuccessExistingUser
        } catch (e: FirebaseAuthInvalidUserException) {
            AuthResult.Error("No existe ninguna cuenta registrada con este correo.")
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            AuthResult.Error("Contraseña o correo incorrectos. Si no recuerdas tu contraseña, puedes restablecerla abajo.")
        } catch (e: com.google.firebase.FirebaseNetworkException) {
            AuthResult.Error("Error de red. Verifica tu conexión a internet.")
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: e.message ?: ""
            if (msg.contains("credential", ignoreCase = true) || msg.contains("password", ignoreCase = true)) {
                AuthResult.Error("Contraseña o correo incorrectos. Si no recuerdas tu contraseña, puedes restablecerla abajo.")
            } else {
                Log.w("AuthRepository", "Aviso en inicio de sesión: $msg")
                AuthResult.Error(if (msg.isNotEmpty()) msg else "Error al iniciar sesión.")
            }
        }
    }

    suspend fun forgotPassword(email: String): AuthResult {
        val cleanEmail = email.trim()
        if (cleanEmail.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            return AuthResult.Error("Por favor ingresa un correo electrónico válido.")
        }

        return try {
            auth.sendPasswordResetEmail(cleanEmail).await()
            AuthResult.SuccessExistingUser
        } catch (e: FirebaseAuthInvalidUserException) {
            AuthResult.Error("No existe ninguna cuenta registrada con este correo.")
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            AuthResult.Error("El formato del correo electrónico es inválido.")
        } catch (e: com.google.firebase.FirebaseNetworkException) {
            AuthResult.Error("Error de red. Verifica tu conexión a internet.")
        } catch (e: Exception) {
            Log.e("AuthRepository", "Forgot password error", e)
            AuthResult.Error(e.localizedMessage ?: "No se pudo enviar el correo de recuperación.")
        }
    }

    suspend fun googleSignIn(idToken: String): Result<ProfileResponse> {
        return try {
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user ?: return Result.failure(Exception("Fallo al iniciar sesión"))
            
            val uid = user.uid
            val displayName = user.displayName ?: "Usuario"
            val photoUrl = user.photoUrl?.toString() ?: ""
            val email = user.email ?: ""
            val username = email.substringBefore("@").lowercase().replace(Regex("[^a-z0-9]"), "")

            val userDocRef = db.collection("users").document(uid)
            val doc = userDocRef.get().await()

            val profile = if (!doc.exists()) {
                val newProfile = ProfileResponse(
                    uid = uid,
                    username = username,
                    displayName = displayName,
                    photoUrl = photoUrl,
                    email = email
                )
                userDocRef.set(newProfile).await()
                newProfile
            } else {
                doc.toObject(ProfileResponse::class.java)!!
            }
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
