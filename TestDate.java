import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Date;
import java.util.TimeZone;

public class TestDate {
    public static void main(String[] args) {
        String rawDate = "Wed, 29 Jul 2026 18:15:53 GMT";
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
            Date date = inputFormat.parse(rawDate);
            
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", new Locale("bn", "BD"));
            outputFormat.setTimeZone(TimeZone.getTimeZone("Asia/Dhaka"));
            
            System.out.println("Formatted: " + outputFormat.format(date));
            
        } catch (Exception e) {
            System.out.println("Error: " + e);
        }
    }
}
