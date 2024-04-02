package app.getxray.xray.junit.common;

public final class Utils {

    private Utils() {
      }
    
    public static boolean isStringEmpty(String string) {
        return (string == null || string.isEmpty() || string.trim().isEmpty());
    }

}
