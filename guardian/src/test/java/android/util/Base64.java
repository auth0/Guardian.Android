package android.util;

import android.os.Build;

import java.nio.charset.StandardCharsets;

public class Base64 {
    public static final int NO_PADDING = 1;
    public static final int NO_WRAP = 2;
    public static final int URL_SAFE = 8;
    public static String encodeToString(byte[] input, int flags) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return java.util.Base64.getEncoder().encodeToString(input);
        }
        return "eh?";
    }

    public static byte[] decode(String str, int flags) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return java.util.Base64.getDecoder().decode(str);
        }

        return "eh?".getBytes(StandardCharsets.UTF_8);
    }
}