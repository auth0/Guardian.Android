/*
 * Copyright (c) 2016 Auth0 (http://auth0.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.auth0.android.guardian.sdk;

import android.net.Uri;
import android.os.Build;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.auth0.android.guardian.sdk.networking.RequestFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Low level API client for Guardian MFA server
 * <p>
 * Use this API client to manually create enrollments, allow/reject authentication requests, and
 * manage an enrollment's device data
 */
public class GuardianAPIClient {

    private static final int ACCESS_APPROVAL_JWT_EXP_SECS = 30;
    private static final int BASIC_JWT_EXP_SECS = 60 * 60 * 2; // 2 hours
    private static final String PATH = "appliance-mfa";

    private final RequestFactory requestFactory;
    private final HttpUrl baseUrl;
    private final ClientInfo clientInfo;

    GuardianAPIClient(RequestFactory requestFactory, HttpUrl baseUrl, ClientInfo clientInfo) {
        this.requestFactory = requestFactory;
        this.baseUrl = appendingPathComponentIfNeeded(baseUrl, PATH);
        this.clientInfo = clientInfo;
    }

    private static Map<String, String> createJWK(@NonNull PublicKey publicKey) {
        if (!(publicKey instanceof RSAPublicKey)) {
            throw new IllegalArgumentException("Only RSA keys are supported");
        }
        RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;
        Map<String, String> jwk = new HashMap<>(5);
        jwk.put("kty", "RSA");
        jwk.put("alg", "RS256");
        jwk.put("use", "sig");
        jwk.put("e", base64UrlSafeEncode(rsaPublicKey.getPublicExponent().toByteArray()));
        jwk.put("n", base64UrlSafeEncode(rsaPublicKey.getModulus().toByteArray()));
        return jwk;
    }

    private static String base64UrlSafeEncode(byte[] bytes) {
        return Base64.encodeToString(bytes, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }

    static Map<String, String> createPushCredentials(@Nullable String gcmToken) {
        if (gcmToken == null) {
            return null;
        }

        Map<String, String> pushCredentials = new HashMap<>(2);
        pushCredentials.put("service", "GCM");
        pushCredentials.put("token", gcmToken);
        return pushCredentials;
    }

    String getUrl() {
        return baseUrl.toString();
    }

    /**
     * Creates an enroll. When successful, returns extra data about the new Enrollment, including
     * the token that can be used to update the push notification settings and to un-enroll this
     * device.
     * This device will now be available as a Guardian second factor.
     *
     * @param enrollmentTicket the enrollment ticket obtained from a Guardian QR code or enrollment
     *                         email
     * @param deviceIdentifier the local identifier that uniquely identifies the android device
     * @param deviceName       this device's name
     * @param gcmToken         the GCM token required to send push notifications to this device
     * @param publicKey        the RSA public key to associate with the enrollment
     * @return a request to execute or start
     */
    @NonNull
    public GuardianAPIRequest<Map<String, Object>> enroll(@NonNull String enrollmentTicket,
                                                          @NonNull String deviceIdentifier,
                                                          @NonNull String deviceName,
                                                          @NonNull String gcmToken,
                                                          @NonNull PublicKey publicKey) {
        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();

        HttpUrl url = baseUrl.newBuilder()
                .addPathSegments("api/enroll")
                .build();

        return requestFactory
                .<Map<String, Object>>newRequest("POST", url, type)
                .setHeader("Authorization", String.format("Ticket id=\"%s\"", enrollmentTicket))
                .setHeader("Auth0-Client", this.clientInfo.toBase64())
                .setParameter("identifier", deviceIdentifier)
                .setParameter("name", deviceName)
                .setParameter("push_credentials", createPushCredentials(gcmToken))
                .setParameter("public_key", createJWK(publicKey));
    }

    /**
     * Allows an authentication request using the enrollment's private key
     *
     * @param txToken          the auth transaction token
     * @param deviceIdentifier the local device identifier
     * @param challenge        the one time password
     * @param privateKey       the private key used to sign the payload
     * @return a request to execute
     * @throws GuardianException when signing with private key fails
     */
    public GuardianAPIRequest<Void> allow(@NonNull String txToken,
                                          @NonNull String deviceIdentifier,
                                          @NonNull String challenge,
                                          @NonNull PrivateKey privateKey) {
        final HttpUrl url = baseUrl.newBuilder()
                .addPathSegments("api/resolve-transaction")
                .build();

        final String jwt = createAccessApprovalJWT(privateKey, url.toString(), deviceIdentifier, challenge, true, null);
        return requestFactory
                .<Void>newRequest("POST", url, Void.class)
                .setHeader("Auth0-Client", this.clientInfo.toBase64())
                .setBearer(txToken)
                .setParameter("challenge_response", jwt);
    }

    /**
     * Rejects an authentication request using the enrollment's private key, indicating a reason
     *
     * @param txToken          the auth transaction token
     * @param deviceIdentifier the local device identifier
     * @param challenge        the one time password
     * @param privateKey       the private key used to sign the payload
     * @param reason           the reject reason
     * @return a request to execute
     * @throws GuardianException when signing with private key fails
     */
    public GuardianAPIRequest<Void> reject(@NonNull String txToken,
                                           @NonNull String deviceIdentifier,
                                           @NonNull String challenge,
                                           @NonNull PrivateKey privateKey,
                                           @Nullable String reason) {
        final HttpUrl url = baseUrl.newBuilder()
                .addPathSegments("api/resolve-transaction")
                .build();

        final String jwt = createAccessApprovalJWT(privateKey, url.toString(), deviceIdentifier, challenge, false, reason);
        return requestFactory
                .<Void>newRequest("POST", url, Void.class)
                .setHeader("Auth0-Client", this.clientInfo.toBase64())
                .setBearer(txToken)
                .setParameter("challenge_response", jwt);
    }

    /**
     * Rejects an authentication request using the enrollment's private key
     *
     * @param txToken          the auth transaction token
     * @param deviceIdentifier the local device identifier
     * @param challenge        the one time password
     * @param privateKey       the private key used to sign the payload
     * @return a request to execute
     * @throws GuardianException when signing with private key fails
     */
    public GuardianAPIRequest<Void> reject(@NonNull String txToken,
                                           @NonNull String deviceIdentifier,
                                           @NonNull String challenge,
                                           @NonNull PrivateKey privateKey) {
        return reject(txToken, deviceIdentifier, challenge, privateKey, null);
    }

    /**
     * Returns an API client to create, update or delete an enrollment's device data
     *
     * @param deviceIdentifier the device id
     * @param subject          the enrollment's user id
     * @param privateKey       the private key used to sign the payload
     * @return an API client for the device
     */
    @NonNull
    public DeviceAPIClient device(@NonNull String deviceIdentifier, @NonNull String subject, @NonNull PrivateKey privateKey) {
        final HttpUrl url = baseUrl.newBuilder()
                .addPathSegments("api/device-accounts")
                .build();

        final String token = createBasicJWT(privateKey, url.toString(), deviceIdentifier, subject);

        return new DeviceAPIClient(requestFactory, baseUrl, deviceIdentifier, token);
    }

    private String createBasicJWT(@NonNull PrivateKey privateKey,
                                  @NonNull String audience,
                                  @NonNull String deviceIdentifier,
                                  @NonNull String subject) {
        long currentTime = new Date().getTime() / 1000L;
        Map<String, Object> claims = new HashMap<>();
        claims.put("iat", currentTime);
        claims.put("exp", currentTime + BASIC_JWT_EXP_SECS);
        claims.put("aud", audience);
        claims.put("iss", deviceIdentifier);
        claims.put("sub", subject);
        return signJWT(privateKey, claims);
    }

    private String createAccessApprovalJWT(@NonNull PrivateKey privateKey,
                                           @NonNull String audience,
                                           @NonNull String deviceIdentifier,
                                           @NonNull String subject,
                                           boolean accepted,
                                           @Nullable String reason) {
        long currentTime = new Date().getTime() / 1000L;
        Map<String, Object> claims = new HashMap<>();
        claims.put("iat", currentTime);
        claims.put("exp", currentTime + ACCESS_APPROVAL_JWT_EXP_SECS);
        claims.put("aud", audience);
        claims.put("iss", deviceIdentifier);
        claims.put("sub", subject);
        claims.put("auth0_guardian_method", "push");
        claims.put("auth0_guardian_accepted", accepted);
        if (reason != null) {
            claims.put("auth0_guardian_reason", reason);
        }
        return signJWT(privateKey, claims);
    }

    private String signJWT(@NonNull PrivateKey privateKey, @NonNull Map<String, Object> claims) {
        try {
            Map<String, Object> headers = new HashMap<>();
            headers.put("alg", "RS256");
            headers.put("typ", "JWT");
            Gson gson = new GsonBuilder().create();
            String headerAndPayload = base64UrlSafeEncode(gson.toJson(headers).getBytes())
                    + "." + base64UrlSafeEncode(gson.toJson(claims).getBytes());
            final byte[] messageBytes = headerAndPayload.getBytes();
            final Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initSign(privateKey);
            signer.update(messageBytes);
            byte[] signature = signer.sign();
            return headerAndPayload + "." + base64UrlSafeEncode(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new GuardianException("Unable to generate the signed JWT", e);
        }
    }

    private HttpUrl appendingPathComponentIfNeeded(HttpUrl url, String pathComponent){
        String lastPathSegment = url.pathSegments().get(url.pathSize() -1);
        if(lastPathSegment.equals(pathComponent)){
            return url;
        }

        if(url.encodedPath().contains("/" + pathComponent)){
            return url;
        }

        if(url.host().endsWith("guardian.auth0.com")){
            return url;
        }

        if(url.host().matches(".*guardian\\.[^.]*\\.auth0\\.com.*")){
            return url;
        }

        return url.newBuilder()
                .addPathSegment(pathComponent)
                .build();
    }

    /**
     * Returns an API client to create, update or delete an enrollment's device data
     *
     * @param id    the device id
     * @param token the device token
     * @return an API client for the device
     * @deprecated Use {@link #device(String, String, PrivateKey)} to prefer JWT authentication.
     */
    @Deprecated
    public DeviceAPIClient device(@NonNull String id, @NonNull String token) {
        return new DeviceAPIClient(requestFactory, baseUrl, id, token);
    }

    /**
     * A {@link GuardianAPIClient} Builder
     */
    public static class Builder {

        private HttpUrl url;
        private ClientInfo clientInfo;
        private RequestFactory requestFactory;

        /**
         * Set the URL of the Guardian server.
         * For example {@code https://tenant.region.auth0.com/}
         *
         * @param url the url
         * @return itself
         * @throws IllegalArgumentException when an url or domain was already set
         */
        public Builder url(@NonNull Uri url) {
            if (this.url != null) {
                throw new IllegalArgumentException("An url/domain was already set");
            }
            this.url = HttpUrl.parse(url.toString());
            return this;
        }

        /**
         * Set the domain of the Guardian server.
         * For example {@code tenant.region.auth0.com}
         *
         * @param domain the domain name
         * @return itself
         * @throws IllegalArgumentException when an url or domain was already set
         */
        public Builder domain(@NonNull String domain) {
            if (this.url != null) {
                throw new IllegalArgumentException("An url/domain was already set");
            }
            this.url = new HttpUrl.Builder()
                    .scheme("https")
                    .host(domain)
                    .build();
            return this;
        }

        public Builder setClientInfo(ClientInfo clientInfo) {
            this.clientInfo = clientInfo;
            return this;
        }

        public Builder setRequestFactory(RequestFactory requestFactory) {
            this.requestFactory = requestFactory;
            return this;
        }

        /**
         * Builds and returns the GuardianAPIClient instance
         *
         * @return the created instance
         * @throws IllegalStateException when the builder was not configured correctly
         */
        public GuardianAPIClient build() {
            if (url == null) {
                throw new IllegalStateException("You must set either a domain or an url");
            }

            if (requestFactory == null) {
                throw new IllegalStateException("RequestFactory cannot be null");
            }

            return new GuardianAPIClient(requestFactory, url, clientInfo);
        }
    }
}
