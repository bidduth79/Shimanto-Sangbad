import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class TestUrlResolver {
    public static void main(String[] args) {
        try {
            String rssUrl = "https://news.google.com/rss/articles/CBMiaEFVX3lxTE1INmc0dVdZdDNGMjY4ZmRkanlFM3JBUU9IVGZpdEwza1BNQVJuQmQ1T0Z3N2dESXlHbjMxbE8zU1NfOUhzWGpoRjg3M2RGYmVtTTBTWTNVVi0wMzZwS1hKdHUxTTRKS2h40gF2QVVfeXFMUHNyMWxxZkZrbXZzU3R2Umd0ejltV2FCRjAyZEtRTm01VVgyYk9JQ2RIdDRrYVJERURYQTRvZE8xam5OTVcwYk1VZ05wWFY0aU5WekU0XzQ3bk8yZmlPLU9sc0pDT09NT2RoNTJXQmk4X2g4TVlyQQ?oc=5";
            
            // Try fetching with followRedirects = true or following 302
            URL url = new URL(rssUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            conn.connect();
            
            System.out.println("Status: " + conn.getResponseCode());
            String loc = conn.getHeaderField("Location");
            System.out.println("Location: " + loc);
            
            if (loc != null) {
                // follow to location
                URL url2 = new URL(loc);
                HttpURLConnection conn2 = (HttpURLConnection) url2.openConnection();
                conn2.setInstanceFollowRedirects(false);
                conn2.setRequestMethod("GET");
                conn2.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                conn2.connect();
                System.out.println("Status 2: " + conn2.getResponseCode());
                System.out.println("Location 2: " + conn2.getHeaderField("Location"));
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn2.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                reader.close();
                String html = sb.toString();
                
                // Find <c-wiz ... data-n-a-id="..." ...> or jsdata or data-p or any url
                Pattern p = Pattern.compile("data-p=\"([^\"]+)\"");
                Matcher m = p.matcher(html);
                if (m.find()) {
                    System.out.println("data-p content: " + m.group(1));
                }
                
                // Look for any http/https inside html that is NOT google
                Pattern pUrl = Pattern.compile("https?://[^\",\\s\\\\]+");
                Matcher mUrl = pUrl.matcher(html);
                while (mUrl.find()) {
                    String u = mUrl.group();
                    if (!u.contains("google") && !u.contains("gstatic") && !u.contains("w3.org") && !u.contains("schema.org")) {
                        System.out.println("Found target candidate: " + u);
                    }
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
