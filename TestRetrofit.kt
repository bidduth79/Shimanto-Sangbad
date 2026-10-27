import java.net.URL
fun main() {
    val baseUrl = URL("https://api.telegram.org/")
    val relativeUrl = "/bot8513488470:AAGO9I43FcsNARzx4F6b338jH8eUVTbynCQ/sendMessage"
    println(URL(baseUrl, relativeUrl).toString())
}
