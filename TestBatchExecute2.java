import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class TestBatchExecute2 {
    public static void main(String[] args) {
        try {
            String articleId = "CBMiaEFVX3lxTE1INmc0dVdZdDNGMjY4ZmRkanlFM3JBUU9IVGZpdEwza1BNQVJuQmQ1T0Z3N2dESXlHbjMxbE8zU1NfOUhzWGpoRjg3M2RGYmVtTTBTWTNVVi0wMzZwS1hKdHUxTTRKS2h40gF2QVVfeXFMUHNyMWxxZkZrbXZzU3R2Umd0ejltV2FCRjAyZEtRTm01VVgyYk9JQ2RIdDRrYVJERURYQTRvZE8xam5OTVcwYk1VZ05wWFY0aU5WekU0XzQ3bk8yZmlPLU9sc0pDT09NT2RoNTJXQmk4X2g4TVlyQQ";
            
            // Format RPC payload: [[["FbvS1b","[\"CBMi...\",1]",null,"generic"]]]
            String innerJson = "[\"" + articleId + "\",1]";
            String payload = "[[[\"FbvS1b\",\"" + innerJson.replace("\"", "\\\"") + "\",null,\"generic\"]]]";
            String reqBody = "f.req=" + URLEncoder.encode(payload, "UTF-8");
            
            URL url = new URL("https://news.google.com/_/DotsSplashUi/data/batchexecute?rpcids=FbvS1b");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            conn.setDoOutput(true);
            
            OutputStream os = conn.getOutputStream();
            os.write(reqBody.getBytes("UTF-8"));
            os.flush();
            os.close();
            
            System.out.println("Response code: " + conn.getResponseCode());
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            
            String resp = sb.toString();
            System.out.println("Response preview: " + resp.substring(0, Math.min(300, resp.length())));
            
            Pattern urlPattern = Pattern.compile("https?://[a-zA-Z0-9\\.\\-_/]+\\.[a-zA-Z]{2,}[^\\s\"'\\\\\\]<>]*");
            Matcher m = urlPattern.matcher(resp);
            while (m.find()) {
                String found = m.group();
                if (!found.contains("google.com") && !found.contains("gstatic.com")) {
                    System.out.println("★ SUCCESS - Original News URL: " + found);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
