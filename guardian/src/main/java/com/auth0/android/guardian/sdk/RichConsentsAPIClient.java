package com.auth0.android.guardian.sdk;

import static com.auth0.android.guardian.sdk.oauth2.OAuth2AccessToken.getTokenHash;

import android.net.Uri;
import android.util.Base64;

import androidx.annotation.NonNull;

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

/**
 * Rich Consents API client.
 * <p>
 * Use this client to fetch the consent details when the transaction has a linking id associated.
 */
public class RichConsentsAPIClient {
    private final RequestFactory requestFactory;
    private final HttpUrl baseUrl;
    private final ClientInfo clientInfo;

    private static final String CONSENT_PATH = "rich-consents";


    RichConsentsAPIClient(RequestFactory requestFactory, Uri url, ClientInfo clientInfo) {
        this.requestFactory = requestFactory;
        this.baseUrl = buildBaseUrl(url);
        this.clientInfo = clientInfo;
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

    private static String createProofOfPossessionAssertion(HttpUrl url, PrivateKey privateKey, PublicKey publicKey, String transactionToken) {
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

    /**
     * Fetches the consent details.
     *
     * @param consentId Consent ID, a.k.a. transactionLinkingId.
     * @param transactionToken Transaction token received in the push notification
     * @param privateKey RSA private key, which can be obtained from enrollment object using enrollment.getSigningKey() used to sign the requests to allow/reject an authentication request
     * @param publicKey The public key used for enrollment, which can be obtained from enrollment object using enrollment.getPublicKey()
     * @return A GuardianAPIRequest that should be started/executed.
     */

    public GuardianAPIRequest<RichConsent> fetch(@NonNull String consentId, @NonNull String transactionToken, PrivateKey privateKey, PublicKey publicKey) {
        final Type type = new TypeToken<GuardianRichConsent>() {
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
                .setHeader("Auth0-Client", this.clientInfo.toBase64())
                .setHeader("Authorization", "MFA-DPoP ".concat(transactionToken))
                .setHeader("MFA-DPoP", dpopAssertion);
    }

    public static HttpUrl buildBaseUrl(Uri url) {
        HttpUrl httpUrl = HttpUrl.parse(url.toString());
        if (httpUrl == null) {
            throw new NullPointerException("Base uri cannot be null");
        }

        String host = httpUrl.host();

        if (host.endsWith("auth0.com")) {
            host = host.replace(".guardian", "");
        }

        return httpUrl.newBuilder()
                .host(host)
                .addPathSegments(CONSENT_PATH)
                .build();
    }
}
