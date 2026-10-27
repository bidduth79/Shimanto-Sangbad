import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class TestUrlDecoder2 {
    public static void main(String[] args) {
        try {
            String rssUrl = "https://news.google.com/rss/articles/CBMiaEFVX3lxTE1INmc0dVdZdDNGMjY4ZmRkanlFM3JBUU9IVGZpdEwza1BNQVJuQmQ1T0Z3N2dESXlHbjMxbE8zU1NfOUhzWGpoRjg3M2RGYmVtTTBTWTNVVi0wMzZwS1hKdHUxTTRKS2h40gF2QVVfeXFMUHNyMWxxZkZrbXZzU3R2Umd0ejltV2FCRjAyZEtRTm01VVgyYk9JQ2RIdDRrYVJERURYQTRvZE8xam5OTVcwYk1VZ05wWFY0aU5WekU0XzQ3bk8yZmlPLU9sc0pDT09NT2RoNTJXQmk4X2g4TVlyQQ?oc=5";
            
            URL url = new URL(rssUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");
            conn.setInstanceFollowRedirects(true);
            conn.connect();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            
            String html = sb.toString();
            
            // Search for "data-n-a-id" or "c-wiz" or "a href" or "http"
            Pattern p = Pattern.compile("c-wiz[^>]*");
            Matcher m = p.matcher(html);
            while (m.find()) {
                String tag = m.group();
                if (tag.contains("http")) {
                    System.out.println("c-wiz tag: " + tag);
                }
            }

            // Search for data-n-a-id or data-n-a-ts
            Pattern p2 = Pattern.compile("data-n-a-id=\"([^\"]+)\"");
            Matcher m2 = p2.matcher(html);
            if (m2.find()) {
                System.out.println("data-n-a-id: " + m2.group(1));
            }

            // Search for <a href="...">
            Pattern p3 = Pattern.compile("<a[^>]+href=\"([^\"]+)\"[^>]*>");
            Matcher m3 = p3.matcher(html);
            while (m3.find()) {
                String href = m3.group(1);
                if (!href.contains("google.com") && !href.contains("google.bd")) {
                    System.out.println("Found external href: " + href);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
