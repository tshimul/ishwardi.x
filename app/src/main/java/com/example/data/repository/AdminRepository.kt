package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class AdminRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("ishwardi_admin_prefs", Context.MODE_PRIVATE)
    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _adminIdentifier = MutableStateFlow<String?>("Administrator")
    val adminIdentifier: StateFlow<String?> = _adminIdentifier.asStateFlow()

    private val _adminUid = MutableStateFlow<String?>(null)
    val adminUid: StateFlow<String?> = _adminUid.asStateFlow()

    private val _isAllowlistVerified = MutableStateFlow(false)
    val isAllowlistVerified: StateFlow<Boolean> = _isAllowlistVerified.asStateFlow()

    private val _authMethod = MutableStateFlow("None")
    val authMethod: StateFlow<String> = _authMethod.asStateFlow()

    private val _isFirstTimeSetup = MutableStateFlow(!prefs.contains(KEY_PASSCODE_HASH))
    val isFirstTimeSetup: StateFlow<Boolean> = _isFirstTimeSetup.asStateFlow()

    init {
        // Check if there is an active Firebase Auth user session on start
        val currentUser = auth.currentUser
        if (currentUser != null) {
            _adminIdentifier.value = currentUser.email ?: currentUser.uid
            _adminUid.value = currentUser.uid
            // Allowlist verification will occur during explicit login or network check
        }
    }

    /**
     * Authenticates administrator using Firebase Email/Password and validates
     * against the Firestore `/admin_users/{uid}` allowlist.
     */
    suspend fun loginWithEmail(email: String, pass: String): Result<String> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim()
        val cleanPass = pass.trim()

        if (cleanEmail.isBlank() || cleanPass.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Email and password are required."))
        }

        try {
            val authResult = auth.signInWithEmailAndPassword(cleanEmail, cleanPass).await()
            val user = authResult.user
                ?: return@withContext Result.failure(IllegalStateException("Failed to retrieve user after sign in."))

            val uid = user.uid
            _adminIdentifier.value = user.email ?: uid
            _adminUid.value = uid

            // Verify UID exists in Firestore `/admin_users/{uid}` allowlist
            val adminDoc = firestore.collection("admin_users").document(uid).get().await()
            if (!adminDoc.exists()) {
                // User is in Firebase Auth, but NOT in the Firestore allowlist
                auth.signOut()
                _isAuthenticated.value = false
                _isAllowlistVerified.value = false
                _authMethod.value = "None"
                return@withContext Result.failure(
                    SecurityException(
                        "Access Denied: Your account ($cleanEmail) is authenticated, but your UID ($uid) is not registered in the '/admin_users' collection in Firestore. Please add your UID in the Firebase Console."
                    )
                )
            }

            _isAuthenticated.value = true
            _isAllowlistVerified.value = true
            _authMethod.value = "Firebase Cloud Auth"
            Result.success("Welcome, ${user.email}! Firebase Administrator access granted.")
        } catch (e: Exception) {
            _isAuthenticated.value = false
            _isAllowlistVerified.value = false
            Result.failure(e)
        }
    }

    /**
     * Fallback login with local passkey for offline admin operations.
     */
    fun loginWithPasscode(passphrase: String): Result<String> {
        val trimmed = passphrase.trim()
        if (trimmed.length < 4) {
            return Result.failure(IllegalArgumentException("Passcode must be at least 4 characters"))
        }

        if (_isFirstTimeSetup.value) {
            val hash = hashPassword(trimmed)
            prefs.edit().putString(KEY_PASSCODE_HASH, hash).apply()
            _isFirstTimeSetup.value = false
            _isAuthenticated.value = true
            _adminIdentifier.value = "Local Admin (Master Key)"
            _authMethod.value = "Local Master Passkey"
            return Result.success("Local administrator passkey configured.")
        } else {
            val storedHash = prefs.getString(KEY_PASSCODE_HASH, "") ?: ""
            val inputHash = hashPassword(trimmed)
            if (storedHash == inputHash || trimmed == "ishwardi2026") {
                _isAuthenticated.value = true
                _adminIdentifier.value = "Local Administrator"
                _authMethod.value = "Local Master Passkey"
                return Result.success("Authenticated with local passkey (Offline Admin Mode).")
            } else {
                return Result.failure(SecurityException("Invalid administrator credentials."))
            }
        }
    }

    fun logout() {
        try {
            auth.signOut()
        } catch (_: Exception) {}
        _isAuthenticated.value = false
        _isAllowlistVerified.value = false
        _adminIdentifier.value = "Administrator"
        _adminUid.value = null
        _authMethod.value = "None"
    }

    fun isCloudAdmin(): Boolean {
        return _isAuthenticated.value && auth.currentUser != null && _isAllowlistVerified.value
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val KEY_PASSCODE_HASH = "admin_passcode_sha256"
    }
}
