package app.ocnews.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress

class NetworkTest {
    @Test
    fun `private ranges are detected`() {
        assertTrue(isPrivateAddress(InetAddress.getByName("192.168.1.201")))
        assertTrue(isPrivateAddress(InetAddress.getByName("10.0.0.5")))
        assertTrue(isPrivateAddress(InetAddress.getByName("172.16.3.4")))
        assertTrue(isPrivateAddress(InetAddress.getByName("127.0.0.1")))
        assertTrue(isPrivateAddress(InetAddress.getByName("100.64.1.1")))
        assertFalse(isPrivateAddress(InetAddress.getByName("217.182.125.143")))
        assertFalse(isPrivateAddress(InetAddress.getByName("8.8.8.8")))
    }
}
