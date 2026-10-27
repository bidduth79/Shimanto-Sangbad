public class TestToday {
    public static void main(String[] args) {
        long timestamp = 1785348953000L;
        long currentMillis = System.currentTimeMillis();
        long threeDaysMillis = 3L * 24 * 60 * 60 * 1000;
        System.out.println("Diff: " + (currentMillis - timestamp));
        System.out.println("Diff <= 3 days? " + ((currentMillis - timestamp) <= threeDaysMillis));
    }
}
