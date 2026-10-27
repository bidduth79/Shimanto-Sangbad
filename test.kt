import java.text.SimpleDateFormat
import java.util.Locale

fun main() {
    val rawDate = "Wed, 29 Jul 2026 19:10:00 GMT"
    val inputFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH)
    val date = inputFormat.parse(rawDate)
    println(date?.time ?: 0L)
}
