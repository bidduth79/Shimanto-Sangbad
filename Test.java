import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Date;

public class Test {
    public static void main(String[] args) {
        String rawDate = "Wed, 29 Jul 2026 19:10:00 GMT";
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
            Date date = inputFormat.parse(rawDate);
            System.out.println(date.getTime());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
