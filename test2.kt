import java.text.SimpleDateFormat
import java.util.Locale

fun main() {
    val rawDate = "Wed, 29 Jul 2026 18:15:53 GMT"
    try {
        val inputFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH)
        val date = inputFormat.parse(rawDate)
        println("parsed: " + date?.time)
    } catch (e: Exception) {
        println("error: " + e)
    }
}
