package app.ocnews.data.repo

// Whether the device is currently on a Wi-Fi network, used to gate image downloads.
fun interface NetworkStatus {
    fun isOnWifi(): Boolean
}
