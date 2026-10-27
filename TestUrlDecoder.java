import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class TestUrlDecoder {
    public static void main(String[] args) {
        try {
            String rssUrl = "https://news.google.com/rss/articles/CBMiaEFVX3lxTE1INmc0dVdZdDNGMjY4ZmRkanlFM3JBUU9IVGZpdEwza1BNQVJuQmQ1T0Z3N2dESXlHbjMxbE8zU1NfOUhzWGpoRjg3M2RGYmVtTTBTWTNVVi0wMzZwS1hKdHUxTTRKS2h40gF2QVVfeXFMUHNyMWxxZkZrbXZzU3R2Umd0ejltV2FCRjAyZEtRTm01VVgyYk9JQ2RIdDRrYVJERURYQTRvZE8xam5OTVcwYk1VZ05wWFY0aU5WekU0XzQ3bk8yZmlPLU9sc0pDT09NT2RoNTJXQmk4X2g4TVlyQQ?oc=5";
            
            URL url = new URL(rssUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            conn.setInstanceFollowRedirects(true);
            conn.connect();
            
            System.out.println("Code: " + conn.getResponseCode());
            System.out.println("Header location: " + conn.getHeaderField("Location"));
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            
            String html = sb.toString();
            System.out.println("HTML length: " + html.length());
            
            // Search for links in HTML
            Pattern pattern = Pattern.compile("https?://[a-zA-Z0-9\\.\\-_/]+\\.[a-zA-Z]{2,}[^\\s\"'<>]*");
            Matcher matcher = pattern.matcher(html);
            while (matcher.find()) {
                String found = matcher.group();
                if (!found.contains("google") && !found.contains("gstatic") && !found.contains("schema.org") && !found.contains("w3.org")) {
                    System.out.println("Candidate original URL: " + found);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
