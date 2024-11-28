package com.auth0.android.guardian.sdk.oauth2;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class OAuth2AccessToken {
    public static String getTokenHash(String token) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        byte[] hash = digest.digest(token.getBytes());
        return android.util.Base64.encodeToString(hash,
                android.util.Base64.URL_SAFE |
                        android.util.Base64.NO_WRAP |
                        android.util.Base64.NO_PADDING);
    }
}
