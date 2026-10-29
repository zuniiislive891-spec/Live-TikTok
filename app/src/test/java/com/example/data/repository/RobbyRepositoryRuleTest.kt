package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test

class RobbyRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun testAuthenticatedUserCanSaveAndObserveProfile() {
        runBlocking {
            val uid = signInTestUser("alice_${System.currentTimeMillis()}@example.com")
            val repository = RobbyRepository(firestore)

            val profile = UserProfile(
                userId = uid,
                name = "Alice Commander",
                language = "ur",
                voiceEnabled = true,
                hudTheme = "cyan_arc"
            )
            repository.saveUserProfile(profile)

            val observed = repository.observeUserProfile().first()
            assertNotNull(observed)
            assertEquals("Alice Commander", observed?.name)
            assertEquals("ur", observed?.language)
        }
    }

    @Test
    fun testUnauthenticatedUserFailsFast() {
        runBlocking {
            auth.signOut()
            val repository = RobbyRepository(firestore)

            assertThrows(IllegalStateException::class.java) {
                repository.requireUserId()
            }
        }
    }

    @Test
    fun testChatMessagesAndNotesCreation() {
        runBlocking {
            val uid = signInTestUser("bob_${System.currentTimeMillis()}@example.com")
            val repository = RobbyRepository(firestore)

            val msgId = repository.addChatMessage(
                role = "user",
                content = "Open Chrome",
                language = "en"
            )
            assertNotNull(msgId)

            val noteId = repository.addQuickNote(
                title = "Mission Brief",
                body = "Activate Robby AI protocol"
            )
            assertNotNull(noteId)

            val notes = repository.observeQuickNotes().first()
            assertEquals(1, notes.size)
            assertEquals("Mission Brief", notes[0].title)
        }
    }
}
