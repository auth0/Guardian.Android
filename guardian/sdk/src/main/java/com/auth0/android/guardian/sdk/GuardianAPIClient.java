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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;

import com.auth0.android.guardian.sdk.networking.RequestFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
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

/**
 * Low level API client for Guardian MFA server
 *
 * Use this API client to manually create enrollments, allow/reject authentication requests, and
 * manage an enrollment's device data
 */
public class GuardianAPIClient {

    private static final int JWT_EXP_SECS = 30;

    private final RequestFactory requestFactory;
    private final HttpUrl baseUrl;

    GuardianAPIClient(RequestFactory requestFactory, HttpUrl baseUrl) {
        this.requestFactory = requestFactory;
        this.baseUrl = baseUrl;
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
     * @param deviceName this device's name
     * @param gcmToken the GCM token required to send push notifications to this device
     * @param publicKey the RSA public key to associate with the enrollment
     * @return a request to execute or start
     */
    @NonNull
    public GuardianAPIRequest<Map<String, Object>> enroll(@NonNull String enrollmentTicket,
                                                          @NonNull String deviceIdentifier,
                                                          @NonNull String deviceName,
                                                          @NonNull String gcmToken,
                                                          @NonNull PublicKey publicKey) {
        Type type = new TypeToken<Map<String, Object>>() {}.getType();
        return requestFactory
                .<Map<String, Object>>newRequest("POST", baseUrl.resolve("api/enroll"), type)
                .setHeader("Authorization", String.format("Ticket id=\"%s\"", enrollmentTicket))
                .setParameter("identifier", deviceIdentifier)
                .setParameter("name", deviceName)
                .setParameter("push_credentials", createPushCredentials(gcmToken))
                .setParameter("public_key", createJWK(publicKey));
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

    /**
     * Allows an authentication request using the enrollment's private key
     *
     * @param txToken the auth transaction token
     * @param deviceIdentifier the local device identifier
     * @param challenge the one time password
     * @param privateKey the private key used to sign the payload
     * @return a request to execute
     * @throws GuardianException when signing with private key fails
     */
    public GuardianAPIRequest<Void> allow(@NonNull String txToken,
                                          @NonNull String deviceIdentifier,
                                          @NonNull String challenge,
                                          @NonNull PrivateKey privateKey) {
        String jwt = createJWT(privateKey, deviceIdentifier, challenge, true, null);
        Type type = new TypeToken<Void>() {}.getType();
        return requestFactory
                .<Void>newRequest("POST", baseUrl.resolve("api/resolve-transaction"), type)
                .setBearer(txToken)
                .setParameter("challenge_response", jwt);
    }

    /**
     * Rejects an authentication request using the enrollment's private key, indicating a reason
     *
     * @param txToken the auth transaction token
     * @param deviceIdentifier the local device identifier
     * @param challenge the one time password
     * @param privateKey the private key used to sign the payload
     * @param reason the reject reason
     * @return a request to execute
     * @throws GuardianException when signing with private key fails
     */
    public GuardianAPIRequest<Void> reject(@NonNull String txToken,
                                           @NonNull String deviceIdentifier,
                                           @NonNull String challenge,
                                           @NonNull PrivateKey privateKey,
                                           @Nullable String reason) {
        String jwt = createJWT(privateKey, deviceIdentifier, challenge, false, reason);
        Type type = new TypeToken<Void>() {}.getType();
        return requestFactory
                .<Void>newRequest("POST", baseUrl.resolve("api/resolve-transaction"), type)
                .setBearer(txToken)
                .setParameter("challenge_response", jwt);
    }

    /**
     * Rejects an authentication request using the enrollment's private key
     *
     * @param txToken the auth transaction token
     * @param deviceIdentifier the local device identifier
     * @param challenge the one time password
     * @param privateKey the private key used to sign the payload
     * @return a request to execute
     * @throws GuardianException when signing with private key fails
     */
    public GuardianAPIRequest<Void> reject(@NonNull String txToken,
                                           @NonNull String deviceIdentifier,
                                           @NonNull String challenge,
                                           @NonNull PrivateKey privateKey) {
        return reject(txToken, deviceIdentifier, challenge, privateKey, null);
    }

    private String createJWT(@NonNull PrivateKey privateKey,
                             @NonNull String deviceIdentifier,
                             @NonNull String challenge,
                             boolean accepted,
                             @Nullable String reason) {
        try {
            Map<String, Object> headers = new HashMap<>();
            headers.put("alg", "RS256");
            headers.put("typ", "JWT");
            long currentTime = new Date().getTime() / 1000L;
            Map<String, Object> claims = new HashMap<>();
            claims.put("iat", currentTime);
            claims.put("exp", currentTime + JWT_EXP_SECS);
            claims.put("aud", getUrl());
            claims.put("iss", deviceIdentifier);
            claims.put("sub", challenge);
            claims.put("auth0_guardian_method", "push");
            claims.put("auth0_guardian_accepted", accepted);
            if (reason != null) {
                claims.put("auth0_guardian_reason", reason);
            }
            Gson gson = new GsonBuilder().create();
            String headerAndPayload = base64UrlSafeEncode(gson.toJson(headers).getBytes())
                    + "." + base64UrlSafeEncode(gson.toJson(claims).getBytes());
            final byte[] messageBytes = headerAndPayload.getBytes();
            final Signature signer = Signature.getInstance("SHA256withRSA", "BC");
            signer.initSign(privateKey);
            signer.update(messageBytes);
            byte[] signature = signer.sign();
            return headerAndPayload + "." + base64UrlSafeEncode(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException | NoSuchProviderException e) {
            throw new GuardianException("Unable to generate the signed JWT", e);
        }
    }

    /**
     * Returns an API client to create, update or delete an enrollment's device data
     *
     * @param id    the device id
     * @param token the device token
     * @return an API client for the device
     */
    public DeviceAPIClient device(@NonNull String id, @NonNull String token) {
        return new DeviceAPIClient(requestFactory, baseUrl, id, token);
    }

    /**
     * A {@link GuardianAPIClient} Builder
     */
    public static class Builder {

        private HttpUrl url;

        /**
         * Set the URL of the Guardian server.
         * For example {@code https://tenant.guardian.auth0.com/}
         *
         * @param url the url
         * @return itself
         * @throws IllegalArgumentException when an url or domain was already set
         */
        public GuardianAPIClient.Builder url(@NonNull Uri url) {
            if (this.url != null) {
                throw new IllegalArgumentException("An url/domain was already set");
            }
            this.url = HttpUrl.parse(url.toString());
            return this;
        }

        /**
         * Set the domain of the Guardian server.
         * For example {@code tenant.guardian.auth0.com}
         *
         * @param domain the domain name
         * @return itself
         * @throws IllegalArgumentException when an url or domain was already set
         */
        public GuardianAPIClient.Builder domain(@NonNull String domain) {
            if (this.url != null) {
                throw new IllegalArgumentException("An url/domain was already set");
            }
            this.url = new HttpUrl.Builder()
                    .scheme("https")
                    .host(domain)
                    .build();
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

            final OkHttpClient.Builder builder = new OkHttpClient.Builder();

            final String clientInfo = Base64.encodeToString(
                    String.format("{\"name\":\"GuardianSDK.Android\",\"version\":\"%s\"}",
                            BuildConfig.VERSION_NAME).getBytes(),
                    Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);

            builder.addInterceptor(new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    okhttp3.Request originalRequest = chain.request();
                    okhttp3.Request requestWithUserAgent = originalRequest.newBuilder()
                            .header("Accept-Language",
                                    Locale.getDefault().toString())
                            .header("User-Agent",
                                    String.format("GuardianSDK/%s(%s) Android %s",
                                            BuildConfig.VERSION_NAME,
                                            BuildConfig.VERSION_CODE,
                                            Build.VERSION.RELEASE))
                            .header("Auth0-Client", clientInfo)
                            .build();
                    return chain.proceed(requestWithUserAgent);
                }
            });

            OkHttpClient client = builder.build();

            Gson gson = new GsonBuilder().create();

            RequestFactory requestFactory = new RequestFactory(gson, client);

            return new GuardianAPIClient(requestFactory, url);
        }
    }
}
