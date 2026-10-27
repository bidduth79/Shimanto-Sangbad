import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

public class TestHttp {
    public static void main(String[] args) {
        try {
            URL url = new URL("https://news.google.com/rss/search?q=%E0%A6%AC%E0%A6%BF%E0%A6%9C%E0%A6%BF%E0%A6%AC%E0%A6%BF+when:1d&hl=bn&gl=BD&ceid=BD:bn");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setReadTimeout(5000);
            connection.setConnectTimeout(5000);
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.connect();
            System.out.println("Response Code: " + connection.getResponseCode());
            if (connection.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                System.out.println(reader.readLine());
                reader.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
