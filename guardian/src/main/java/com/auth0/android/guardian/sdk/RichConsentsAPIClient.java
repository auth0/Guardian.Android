package com.auth0.android.guardian.sdk;

import static com.auth0.android.guardian.sdk.oauth2.OAuth2AccessToken.getTokenHash;

import android.util.Base64;

import com.auth0.android.guardian.sdk.model.RichConsent;
import com.auth0.android.guardian.sdk.networking.RequestFactory;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import okhttp3.HttpUrl;

public class RichConsentsAPIClient {
    private final RequestFactory requestFactory;
    private final HttpUrl baseUrl;
    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    RichConsentsAPIClient(RequestFactory requestFactory, HttpUrl baseUrl, PrivateKey privateKey, PublicKey publicKey) {
        this.requestFactory = requestFactory;
        this.baseUrl = baseUrl.newBuilder()
                .addPathSegments("rich-consents")
                .build();
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    public GuardianAPIRequest<RichConsent> fetch(String consentId, String transactionToken) {
        Type type = new TypeToken<RichConsent>() {
        }.getType();

        final HttpUrl url = baseUrl.newBuilder()
                .addPathSegment(consentId)
                .build();

        final String dpopAssertion = createProofOfPossessionAssertion(
                url,
                privateKey,
                publicKey,
                transactionToken
        );

        return requestFactory
                .<RichConsent>newRequest("GET", url, type)
                .setHeader("Authorization", "MFA-DPoP ".concat(transactionToken))
                .setHeader("MFA-DPoP", dpopAssertion);
    }

    private String createProofOfPossessionAssertion(HttpUrl url, PrivateKey privateKey, PublicKey publicKey, String transactionToken) {
        long currentTime = new Date().getTime() / 1000L;
        Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "RS256");
        headers.put("typ", "dpop+jwt");
        headers.put("jwk", exportJWK(publicKey));

        Map<String, Object> claims = new HashMap<>();
        claims.put("htu", url.toString());
        claims.put("htm", "GET");
        claims.put("ath", getTokenHash(transactionToken));
        claims.put("jti", UUID.randomUUID().toString());
        claims.put("iat", currentTime);

        Algorithm alg = Algorithm.RSA256(null, (RSAPrivateKey) privateKey);
        return JWT.create()
                .withHeader(headers)
                .withPayload(claims)
                .sign(alg);
    }

    private static Map<String, String> exportJWK(PublicKey publicKey) {
        if (!(publicKey instanceof RSAPublicKey)) {
            throw new IllegalArgumentException("The key is not an RSA public key.");
        }

        RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;

        // Extract the modulus and exponent
        String modulus = base64UrlSafeEncode(rsaPublicKey.getModulus().toByteArray());
        String exponent = base64UrlSafeEncode(rsaPublicKey.getPublicExponent().toByteArray());

        // Create the JWK structure
        Map<String, String> jwk = new HashMap<>();
        jwk.put("kty", "RSA");
        jwk.put("n", modulus);
        jwk.put("e", exponent);
        jwk.put("alg", "RS256");  // You can change this depending on the algorithm you're using
        jwk.put("use", "sig");    // Use 'sig' for signature verification

        return jwk;
    }

    private static String base64UrlSafeEncode(byte[] bytes) {
        return Base64.encodeToString(bytes, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }
}
