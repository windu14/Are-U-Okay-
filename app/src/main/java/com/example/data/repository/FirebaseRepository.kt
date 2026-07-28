package com.example.data.repository

import com.example.data.local.JournalNote
import com.example.data.remote.ITunesTrack
import com.example.data.remote.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseRepository {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    val currentUserFlow: Flow<FirebaseUser?> = callbackFlow {
        try {
            val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                trySend(try { firebaseAuth.currentUser } catch (e: Exception) { null })
            }
            auth.addAuthStateListener(listener)
            awaitClose {
                try {
                    auth.removeAuthStateListener(listener)
                } catch (ignored: Exception) {}
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Error in currentUserFlow", e)
            trySend(null)
            close()
        }
    }

    fun getCurrentUser(): FirebaseUser? = try { auth.currentUser } catch (e: Exception) { null }

    suspend fun signUp(username: String, email: String, pass: String): Result<FirebaseUser> {
        return try {
            // 1. Create account in Firebase Auth first (to obtain valid request.auth token for Firestore)
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val user = authResult.user ?: throw Exception("Gagal mendaftar akun.")

            // 2. Set user display name in Firebase Auth
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(username.trim())
                .build()
            try {
                user.updateProfile(profileUpdates).await()
            } catch (ignored: Exception) {}

            // 3. Create user profile document in Firestore (now authenticated)
            val userMap = hashMapOf(
                "uid" to user.uid,
                "username" to username.trim(),
                "email" to email.trim(),
                "totalCurhat" to 0,
                "createdAt" to System.currentTimeMillis()
            )

            try {
                firestore.collection("users").document(user.uid)
                    .set(userMap, com.google.firebase.firestore.SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                // If Firestore write fails, Auth sign up still succeeds
            }

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(emailOrUsername: String, pass: String): Result<FirebaseUser> {
        return try {
            val input = emailOrUsername.trim()
            val targetEmail = if (input.contains("@")) {
                input
            } else {
                try {
                    val query = firestore.collection("users")
                        .whereEqualTo("username", input)
                        .get()
                        .await()
                    if (query.isEmpty) {
                        return Result.failure(Exception("Username '$input' tidak ditemukan!"))
                    }
                    query.documents.first().getString("email") ?: throw Exception("Email tidak ditemukan.")
                } catch (e: Exception) {
                    return Result.failure(Exception("Akses username dibatasi. Silakan gunakan Alamat Email untuk masuk."))
                }
            }

            val authResult = auth.signInWithEmailAndPassword(targetEmail, pass).await()
            val user = authResult.user ?: throw Exception("Gagal masuk akun.")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
    }

    fun getUserProfileFlow(uid: String): Flow<UserProfile?> = callbackFlow {
        if (uid.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listenerRegistration = firestore.collection("users")
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    val user = auth.currentUser
                    if (user != null && user.uid == uid) {
                        trySend(
                            UserProfile(
                                uid = uid,
                                username = user.displayName?.takeIf { it.isNotBlank() } ?: "Remaja Ceria",
                                email = user.email ?: "",
                                totalCurhat = 0
                            )
                        )
                    } else {
                        trySend(null)
                    }
                    return@addSnapshotListener
                }
                val profile = UserProfile(
                    uid = snapshot.getString("uid") ?: uid,
                    username = snapshot.getString("username") ?: auth.currentUser?.displayName ?: "User",
                    email = snapshot.getString("email") ?: auth.currentUser?.email ?: "",
                    totalCurhat = (snapshot.getLong("totalCurhat") ?: 0L).toInt(),
                    createdAt = snapshot.getLong("createdAt") ?: System.currentTimeMillis()
                )
                trySend(profile)
            }

        awaitClose { listenerRegistration.remove() }
    }

    fun getAllNotesFlow(): Flow<List<JournalNote>> = callbackFlow {
        val listenerRegistration = firestore.collection("curhat")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    android.util.Log.e("FirebaseRepository", "Error reading curhat collection", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val notes = snapshot.documents.mapNotNull { doc ->
                    try {
                        val rawTrackId = doc.get("trackId")
                        val trackIdLong = when (rawTrackId) {
                            is Number -> rawTrackId.toLong()
                            is String -> rawTrackId.toLongOrNull()
                            else -> null
                        }
                        val rawTimestamp = doc.get("timestamp")
                        val timestampLong: Long = when (rawTimestamp) {
                            is Number -> rawTimestamp.toLong()
                            is String -> rawTimestamp.toLongOrNull() ?: System.currentTimeMillis()
                            else -> System.currentTimeMillis()
                        }
                        JournalNote(
                            id = doc.id.hashCode(),
                            docId = doc.id,
                            userId = doc.getString("userId") ?: "",
                            username = doc.getString("username") ?: "Anonim",
                            content = doc.getString("content") ?: "",
                            category = doc.getString("category") ?: "Semuanya",
                            moodEmoji = doc.getString("moodEmoji") ?: "✨",
                            timestamp = timestampLong,
                            trackId = trackIdLong,
                            trackName = doc.getString("trackName"),
                            artistName = doc.getString("artistName"),
                            artworkUrl = doc.getString("artworkUrl"),
                            previewUrl = doc.getString("previewUrl")
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("FirebaseRepository", "Error parsing doc ${doc.id}", e)
                        null
                    }
                }.sortedByDescending { it.timestamp }

                trySend(notes)
            }

        awaitClose { listenerRegistration.remove() }
    }

    suspend fun addNote(
        content: String,
        category: String,
        moodEmoji: String,
        selectedTrack: ITunesTrack?,
        uid: String,
        username: String
    ): Result<Unit> {
        return try {
            val authUser = auth.currentUser
            val effectiveUid = authUser?.uid?.takeIf { it.isNotBlank() }
                ?: uid.takeIf { it.isNotBlank() }
                ?: "anon_${System.currentTimeMillis()}"

            val effectiveUsername = if (username.isNotBlank() && username != "Remaja Ceria" && username != "Anonim") username
                else authUser?.displayName?.takeIf { it.isNotBlank() } ?: username.ifBlank { "Remaja Ceria" }

            val docRef = firestore.collection("curhat").document()
            val noteMap = mutableMapOf<String, Any>(
                "docId" to docRef.id,
                "userId" to effectiveUid,
                "username" to effectiveUsername,
                "content" to content,
                "category" to category,
                "moodEmoji" to moodEmoji,
                "timestamp" to System.currentTimeMillis()
            )

            if (selectedTrack != null) {
                noteMap["trackId"] = selectedTrack.trackId
                selectedTrack.trackName?.let { noteMap["trackName"] = it }
                selectedTrack.artistName?.let { noteMap["artistName"] = it }
                (selectedTrack.highResArtworkUrl ?: selectedTrack.artworkUrl100)?.let { noteMap["artworkUrl"] = it }
                selectedTrack.previewUrl?.let { noteMap["previewUrl"] = it }
            }

            android.util.Log.d("FirebaseRepository", "Saving curhat note: ${docRef.id} for user $effectiveUid")
            docRef.set(noteMap).await()
            android.util.Log.d("FirebaseRepository", "Curhat note saved successfully to Firestore: ${docRef.id}")

            // Increment user totalCurhat in Firestore users collection
            if (effectiveUid.isNotBlank() && !effectiveUid.startsWith("anon_")) {
                try {
                    val userDocRef = firestore.collection("users").document(effectiveUid)
                    userDocRef.set(
                        mapOf(
                            "uid" to effectiveUid,
                            "username" to effectiveUsername,
                            "email" to (authUser?.email ?: ""),
                            "totalCurhat" to FieldValue.increment(1)
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    ).await()
                    android.util.Log.d("FirebaseRepository", "User totalCurhat incremented successfully")
                } catch (e: Exception) {
                    android.util.Log.e("FirebaseRepository", "Error updating user totalCurhat in Firestore", e)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Failed to add note to Firestore", e)
            Result.failure(e)
        }
    }

    suspend fun deleteNote(docId: String, uid: String): Result<Unit> {
        return try {
            if (docId.isNotBlank()) {
                firestore.collection("curhat").document(docId).delete().await()

                if (uid.isNotBlank()) {
                    try {
                        firestore.collection("users").document(uid)
                            .set(
                                mapOf("totalCurhat" to FieldValue.increment(-1)),
                                com.google.firebase.firestore.SetOptions.merge()
                            ).await()
                    } catch (ignored: Exception) {}
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
