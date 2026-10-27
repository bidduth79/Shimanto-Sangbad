import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class TestUserAgents {
    public static void main(String[] args) {
        String rssUrl = "https://news.google.com/rss/articles/CBMiaEFVX3lxTE1INmc0dVdZdDNGMjY4ZmRkanlFM3JBUU9IVGZpdEwza1BNQVJuQmQ1T0Z3N2dESXlHbjMxbE8zU1NfOUhzWGpoRjg3M2RGYmVtTTBTWTNVVi0wMzZwS1hKdHUxTTRKS2h40gF2QVVfeXFMUHNyMWxxZkZrbXZzU3R2Umd0ejltV2FCRjAyZEtRTm01VVgyYk9JQ2RIdDRrYVJERURYQTRvZE8xam5OTVcwYk1VZ05wWFY0aU5WekU0XzQ3bk8yZmlPLU9sc0pDT09NT2RoNTJXQmk4X2g4TVlyQQ?oc=5";
        
        String[] userAgents = {
            "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)",
            "facebookexternalhit/1.1 (+http://www.facebook.com/externalhit_uatext.php)",
            "Mozilla/5.0 (compatible; Bingbot/2.0; +http://www.bing.com/bingbot.htm)",
            "Twitterbot/1.0",
            "WhatsApp/2.21.12.21 A",
            "curl/7.68.0"
        };
        
        for (String ua : userAgents) {
            try {
                URL url = new URL(rssUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setInstanceFollowRedirects(false);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", ua);
                conn.connect();
                
                System.out.println("UA: " + ua.substring(0, Math.min(30, ua.length())));
                System.out.println("  Status: " + conn.getResponseCode());
                System.out.println("  Location: " + conn.getHeaderField("Location"));
                
                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("canonical") || line.contains("http") || line.contains("refresh")) {
                            System.out.println("  Line: " + line.substring(0, Math.min(100, line.length())));
                        }
                    }
                    reader.close();
                }
            } catch (Exception e) {
                System.out.println("Error for " + ua + ": " + e.getMessage());
            }
        }
    }
}
