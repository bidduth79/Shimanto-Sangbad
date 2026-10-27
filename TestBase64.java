import java.util.Base64;

public class TestBase64 {
    public static void main(String[] args) {
        try {
            String b64 = "CBMiaEFVX3lxTE1INmc0dVdZdDNGMjY4ZmRkanlFM3JBUU9IVGZpdEwza1BNQVJuQmQ1T0Z3N2dESXlHbjMxbE8zU1NfOUhzWGpoRjg3M2RGYmVtTTBTWTNVVi0wMzZwS1hKdHUxTTRKS2h40gF2QVVfeXFMUHNyMWxxZkZrbXZzU3R2Umd0ejltV2FCRjAyZEtRTm01VVgyYk9JQ2RIdDRrYVJERURYQTRvZE8xam5OTVcwYk1VZ05wWFY0aU5WekU0XzQ3bk8yZmlPLU9sc0pDT09NT2RoNTJXQmk4X2g4TVlyQQ";
            // Replace URL-safe chars
            String cleanB64 = b64.replace("-", "+").replace("_", "/");
            while (cleanB64.length() % 4 != 0) {
                cleanB64 += "=";
            }
            byte[] decoded = Base64.getDecoder().decode(cleanB64);
            System.out.println("Decoded length: " + decoded.length);
            
            // Print printable ASCII strings from decoded bytes
            StringBuilder sb = new StringBuilder();
            for (byte b : decoded) {
                char c = (char) (b & 0xFF);
                if (c >= 32 && c <= 126) {
                    sb.append(c);
                } else {
                    if (sb.length() > 0) {
                        System.out.println("String found: " + sb.toString());
                        sb.setLength(0);
                    }
                }
            }
            if (sb.length() > 0) {
                System.out.println("String found: " + sb.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
