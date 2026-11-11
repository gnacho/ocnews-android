package app.ocnews.util

import java.net.InetAddress

// True for addresses that only exist inside a local network. A server name that
// resolves to one of these in the LAN will not work from outside, and usually
// means a local DNS entry pointing at an internal machine.
fun isPrivateAddress(address: InetAddress): Boolean {
    if (address.isSiteLocalAddress || address.isLoopbackAddress) return true
    val bytes = address.address
    if (bytes.size != 4) return false
    val first = bytes[0].toInt() and 0xFF
    val second = bytes[1].toInt() and 0xFF
    // 100.64.0.0/10 (carrier grade NAT)
    return first == 100 && second in 64..127
}
