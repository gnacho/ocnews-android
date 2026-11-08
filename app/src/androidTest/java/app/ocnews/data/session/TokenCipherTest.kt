package app.ocnews.data.session

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class TokenCipherTest {
    private val cipher = KeystoreTokenCipher()

    @Test
    fun encryptThenDecryptRoundTrips() {
        val token = "exclaim poem negation abreast paging monopoly"
        val encrypted = cipher.encrypt(token)

        assertNotEquals(token, encrypted)
        assertEquals(token, cipher.decrypt(encrypted))
    }

    @Test
    fun decryptGarbageReturnsNull() {
        assertNull(cipher.decrypt("not-a-valid-ciphertext"))
    }

    @Test
    fun storePersistsTokenEncryptedNotPlaintext() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = DataStoreSessionStore(context, cipher)
        val token = "exclaim poem negation abreast paging monopoly"

        store.save(Session("https://example.com", "alice", token))
        val read = store.session.first()

        assertEquals(token, read?.token)

        // The raw DataStore file must not contain the plaintext token.
        val prefsFile = File(context.filesDir, "datastore/session.preferences_pb")
        val raw = prefsFile.readText(Charsets.ISO_8859_1)
        assertFalse("plaintext token leaked into DataStore", raw.contains(token))
    }
}
