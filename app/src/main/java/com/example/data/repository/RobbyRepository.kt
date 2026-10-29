package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.ChatMessage
import com.example.data.model.QuickNote
import com.example.data.model.UserProfile
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class RobbyRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing Robby AI storage.")
    }

    fun observeUserProfile(): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserProfile::class.java)
                    } else {
                        null
                    }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, docRef.path)
                    throw error
                }
        )
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            val payload = mapOf(
                "userId" to uid,
                "name" to profile.name,
                "language" to profile.language,
                "voiceEnabled" to profile.voiceEnabled,
                "hudTheme" to profile.hudTheme,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    fun observeChatMessages(): Flow<List<ChatMessage>> = flow {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
        emitAll(
            colRef.snapshots()
                .map { snapshot -> snapshot.toObjects(ChatMessage::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, "users/$uid/messages")
                    throw error
                }
        )
    }

    suspend fun addChatMessage(role: String, content: String, language: String, actionExecuted: String = ""): String {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("messages")
        return try {
            val payload = hashMapOf<String, Any>(
                "userId" to uid,
                "role" to role,
                "content" to content,
                "language" to language,
                "actionExecuted" to actionExecuted,
                "createdAt" to FieldValue.serverTimestamp()
            )
            val doc = colRef.add(payload).await()
            doc.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            throw e
        }
    }

    fun observeQuickNotes(): Flow<List<QuickNote>> = flow {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("notes")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
        emitAll(
            colRef.snapshots()
                .map { snapshot -> snapshot.toObjects(QuickNote::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, "users/$uid/notes")
                    throw error
                }
        )
    }

    suspend fun addQuickNote(title: String, body: String, category: String = "Voice Command"): String {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("notes")
        return try {
            val payload = hashMapOf<String, Any>(
                "userId" to uid,
                "title" to title,
                "body" to body,
                "category" to category,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val doc = colRef.add(payload).await()
            doc.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            throw e
        }
    }

    suspend fun deleteQuickNote(noteId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("notes").document(noteId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }
}
