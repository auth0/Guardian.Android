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

import com.auth0.android.guardian.sdk.networking.Callback;
import com.auth0.android.guardian.sdk.utils.MockCallback;
import com.auth0.android.guardian.sdk.utils.MockWebService;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.JWTVerifyException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.RecordedRequest;

import static com.auth0.android.guardian.sdk.utils.CallbackMatcher.hasNoError;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 23, manifest = Config.NONE)
public class GuardianAPIClientTest {

    private static final String ENROLLMENT_TICKET = "ENROLLMENT_TICKET";
    private static final String ENROLLMENT_ID = "ENROLLMENT_ID";
    private static final String DEVICE_ACCOUNT_TOKEN = "DEVICE_ACCOUNT_TOKEN";
    private static final String TX_TOKEN = "TX_TOKEN";
    private static final String ENROLLMENT_URL = "https://example.guardian.auth0.com/";
    private static final String PSAAS_ENROLLMENT_URL = "https://example.guardian.auth0.com/appliance-mfa";
    private static final String ENROLLMENT_ISSUER = "ENROLLMENT_ISSUER";
    private static final String ENROLLMENT_USER = "ENROLLMENT_USER";
    private static final String RECOVERY_CODE = "RECOVERY_CODE";
    private static final String TOTP_SECRET = "TOTP_SECRET";
    private static final String TOTP_ALGORITHM = "TOTP_ALGORITHM";
    private static final Integer TOTP_DIGITS = 6;
    private static final Integer TOTP_PERIOD = 30;
    private static final String DEVICE_IDENTIFIER = "DEVICE_IDENTIFIER";
    private static final String DEVICE_NAME = "DEVICE_NAME";
    private static final String GCM_TOKEN = "GCM_TOKEN";
    private static final byte[] PUBLIC_KEY_EXPONENT = new byte[]{ 0x01, 0x00, 0x01 };
    private static final byte[] PUBLIC_KEY_MODULUS = new byte[]{
            1, 2, 3, 4, 5, 6, 7, 8, 9, 0,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 0,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 0,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 0,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 0,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 0,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 0,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 0,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 0,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 0
    };
    private static final String CHALLENGE = "CHALLENGE";

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    RSAPublicKey publicKey;

    @Mock
    Callback<Map<String,Object>> enrollCallback;

    @Captor
    ArgumentCaptor<Map<String,Object>> enrollCallbackCaptor;

    MockWebService mockAPI;
    GuardianAPIClient apiClient;
    KeyPair keyPair;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        when(publicKey.getModulus())
                .thenReturn(new BigInteger(PUBLIC_KEY_MODULUS));
        when(publicKey.getPublicExponent())
                .thenReturn(new BigInteger(PUBLIC_KEY_EXPONENT));

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        keyPair = keyPairGenerator.generateKeyPair();

        mockAPI = new MockWebService();
        final String domain = mockAPI.getDomain();

        apiClient = new GuardianAPIClient.Builder()
                .url(Uri.parse(domain))
                .build();
    }

    @After
    public void tearDown() throws Exception {
        mockAPI.shutdown();
    }

    @Test
    public void shouldBuildWithUrl() throws Exception {
        GuardianAPIClient apiClient = new GuardianAPIClient.Builder()
                .url(Uri.parse("https://example.guardian.auth0.com"))
                .build();

        assertThat(apiClient.getUrl(),
                is(equalTo("https://example.guardian.auth0.com/")));
    }

    @Test
    public void shouldBuildWithDomain() throws Exception {
        GuardianAPIClient apiClient = new GuardianAPIClient.Builder()
                .domain("example.guardian.auth0.com")
                .build();

        assertThat(apiClient.getUrl(),
                is(equalTo("https://example.guardian.auth0.com/")));
    }

    @Test
    public void shouldFailIfDomainWasAlreadySet() throws Exception {
        exception.expect(IllegalArgumentException.class);

        new GuardianAPIClient.Builder()
                .domain("example.guardian.auth0.com")
                .url(Uri.parse("https://example.guardian.auth0.com"))
                .build();
    }

    @Test
    public void shouldFailIfUrlWasAlreadySet() throws Exception {
        exception.expect(IllegalArgumentException.class);

        new GuardianAPIClient.Builder()
                .url(Uri.parse("https://example.guardian.auth0.com"))
                .domain("example.guardian.auth0.com")
                .build();
    }

    @Test
    public void shouldFailIfNoUrlOrDomainConfigured() throws Exception {
        exception.expect(IllegalStateException.class);

        new GuardianAPIClient.Builder()
                .build();
    }

    @Test
    public void shouldHaveCustomUserAgentAndLanguageHeader() throws Exception {
        mockAPI.willReturnEnrollment(ENROLLMENT_ID, ENROLLMENT_URL, ENROLLMENT_ISSUER, ENROLLMENT_USER,
                DEVICE_ACCOUNT_TOKEN, RECOVERY_CODE, TOTP_SECRET, TOTP_ALGORITHM, TOTP_DIGITS, TOTP_PERIOD);

        final MockCallback<Map<String,Object>> callback = new MockCallback<>();

        apiClient.enroll(ENROLLMENT_TICKET, DEVICE_IDENTIFIER, DEVICE_NAME, GCM_TOKEN, publicKey)
                .start(callback);

        RecordedRequest request = mockAPI.takeRequest();
        assertThat(request.getHeader("User-Agent"),
                is(equalTo(
                        String.format("GuardianSDK/%s(%s) Android %s",
                                BuildConfig.VERSION_NAME,
                                BuildConfig.VERSION_CODE,
                                Build.VERSION.RELEASE))));

        assertThat(request.getHeader("Accept-Language"),
                is(equalTo(Locale.getDefault().toString())));

        String auth0ClientEncoded = request.getHeader("Auth0-Client");
        assertThat(auth0ClientEncoded, is(notNullValue()));

        byte[] auth0ClientDecoded = Base64.decode(
                auth0ClientEncoded, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);

        Type type = new TypeToken<Map<String, String>>() {}.getType();
        Gson gson = new GsonBuilder().create();
        Map<String, String> auth0Client = gson.fromJson(
                new InputStreamReader(
                        new ByteArrayInputStream(auth0ClientDecoded)), type);
        assertThat(auth0Client, is(notNullValue()));
        assertThat(auth0Client, hasEntry("name", "Guardian.Android"));
        assertThat(auth0Client, hasEntry("version", BuildConfig.VERSION_NAME));
    }

    @Test
    public void shouldEnroll() throws Exception {
        mockAPI.willReturnEnrollment(ENROLLMENT_ID, ENROLLMENT_URL, ENROLLMENT_ISSUER, ENROLLMENT_USER,
                DEVICE_ACCOUNT_TOKEN, RECOVERY_CODE, TOTP_SECRET, TOTP_ALGORITHM, TOTP_DIGITS, TOTP_PERIOD);

        apiClient.enroll(ENROLLMENT_TICKET, DEVICE_IDENTIFIER, DEVICE_NAME, GCM_TOKEN, publicKey)
                .start(enrollCallback);

        RecordedRequest request = mockAPI.takeRequest();
        assertThat(request.getPath(), is(equalTo("/api/enroll")));
        assertThat(request.getMethod(), is(equalTo("POST")));
        assertThat(request.getHeader("Authorization"), is(equalTo("Ticket id=\"" + ENROLLMENT_TICKET + "\"")));

        Map<String, Object> body = bodyFromRequest(request);
        assertThat(body, hasEntry("identifier", (Object) DEVICE_IDENTIFIER));
        assertThat(body, hasEntry("name", (Object) DEVICE_NAME));
        assertThat(body, hasKey("public_key"));
        assertThat(body, hasKey("push_credentials"));
        assertThat(body.size(), is(equalTo(4)));

        @SuppressWarnings("unchecked")
        Map<String, Object> pushCredentials = (Map<String, Object>) body.get("push_credentials");
        assertThat(pushCredentials, hasEntry("service", (Object) "GCM"));
        assertThat(pushCredentials, hasEntry("token", (Object) GCM_TOKEN));
        assertThat(pushCredentials.size(), is(equalTo(2)));

        @SuppressWarnings("unchecked")
        Map<String, Object> publicKey = (Map<String, Object>) body.get("public_key");
        assertThat(publicKey, hasEntry("kty", (Object) "RSA"));
        assertThat(publicKey, hasEntry("alg", (Object) "RS256"));
        assertThat(publicKey, hasEntry("use", (Object) "sig"));
        assertThat(publicKey, hasEntry("e", (Object) "AQAB"));
        assertThat(publicKey, hasEntry("n", (Object) "AQIDBAUGBwgJAAECAwQFBgcICQABAgMEBQYHCAkAAQIDBAUGBwgJAAECAwQFBgcICQABAgMEBQYHCAkAAQIDBAUGBwgJAAECAwQFBgcICQABAgMEBQYHCAkAAQIDBAUGBwgJAA"));
        assertThat(publicKey.size(), is(equalTo(5)));

        verify(enrollCallback, timeout(100)).onSuccess(anyMapOf(String.class, Object.class));

        verifyNoMoreInteractions(enrollCallback);
    }

    @Test
    public void shouldCallTheRightUrlWhenUsingPathSegmentsWithoutTrailingSlash() throws Exception {
        MockWebService newMockWebService = new MockWebService();
        final String domain = newMockWebService.getDomain();
        GuardianAPIClient apiClientPSaaS = new GuardianAPIClient.Builder()
                .url(Uri.parse(domain + "appliance-mfa"))
                .build();

        newMockWebService.willReturnEnrollment(ENROLLMENT_ID, PSAAS_ENROLLMENT_URL, ENROLLMENT_ISSUER, ENROLLMENT_USER,
                DEVICE_ACCOUNT_TOKEN, RECOVERY_CODE, TOTP_SECRET, TOTP_ALGORITHM, TOTP_DIGITS, TOTP_PERIOD);

        apiClientPSaaS.enroll(ENROLLMENT_TICKET, DEVICE_IDENTIFIER, DEVICE_NAME, GCM_TOKEN, publicKey)
                .start(enrollCallback);

        RecordedRequest request = newMockWebService.takeRequest();

        assertThat(request.getPath(), is(equalTo("/appliance-mfa/api/enroll")));
        assertThat(request.getMethod(), is(equalTo("POST")));
        assertThat(request.getHeader("Authorization"), is(equalTo("Ticket id=\"" + ENROLLMENT_TICKET + "\"")));
    }

    @Test
    public void shouldFailEnrollIfNotRSA() throws Exception {
        exception.expect(IllegalArgumentException.class);

        final MockCallback<Map<String,Object>> callback = new MockCallback<>();

        apiClient.enroll(ENROLLMENT_TICKET, DEVICE_IDENTIFIER, DEVICE_NAME, GCM_TOKEN, mock(PublicKey.class))
                .start(callback);
    }

    @Test
    public void shouldAllowLoginWithPrivateKey() throws Exception {
        mockAPI.willReturnSuccess(204);

        final MockCallback<Void> callback = new MockCallback<>();

        apiClient.allow(TX_TOKEN, DEVICE_IDENTIFIER, CHALLENGE, keyPair.getPrivate())
                .start(callback);

        RecordedRequest request = mockAPI.takeRequest();
        assertThat(request.getPath(), is(equalTo("/api/resolve-transaction")));
        assertThat(request.getMethod(), is(equalTo("POST")));
        assertThat(request.getHeader("Authorization"), is(equalTo("Bearer " + TX_TOKEN)));

        Map<String, Object> body = bodyFromRequest(request);
        assertThat(body, hasKey("challenge_response"));

        String jwt = (String) body.get("challenge_response");
        verifyJWT(jwt, true, null);

        assertThat(callback, hasNoError());
    }

    @Test
    public void shouldRejectLoginWithPrivateKey() throws Exception {
        mockAPI.willReturnSuccess(204);

        final MockCallback<Void> callback = new MockCallback<>();

        apiClient.reject(TX_TOKEN, DEVICE_IDENTIFIER, CHALLENGE, keyPair.getPrivate(), "hack")
                .start(callback);

        RecordedRequest request = mockAPI.takeRequest();
        assertThat(request.getPath(), is(equalTo("/api/resolve-transaction")));
        assertThat(request.getMethod(), is(equalTo("POST")));
        assertThat(request.getHeader("Authorization"), is(equalTo("Bearer " + TX_TOKEN)));

        Map<String, Object> body = bodyFromRequest(request);
        assertThat(body, hasKey("challenge_response"));

        String jwt = (String) body.get("challenge_response");
        verifyJWT(jwt, false, "hack");

        assertThat(callback, hasNoError());
    }

    @Test
    public void shouldRejectLoginWithoutReasonWithPrivateKey() throws Exception {
        mockAPI.willReturnSuccess(204);

        final MockCallback<Void> callback = new MockCallback<>();

        apiClient.reject(TX_TOKEN, DEVICE_IDENTIFIER, CHALLENGE, keyPair.getPrivate())
                .start(callback);

        RecordedRequest request = mockAPI.takeRequest();
        assertThat(request.getPath(), is(equalTo("/api/resolve-transaction")));
        assertThat(request.getMethod(), is(equalTo("POST")));
        assertThat(request.getHeader("Authorization"), is(equalTo("Bearer " + TX_TOKEN)));

        Map<String, Object> body = bodyFromRequest(request);
        assertThat(body, hasKey("challenge_response"));

        String jwt = (String) body.get("challenge_response");
        verifyJWT(jwt, false, null);

        assertThat(callback, hasNoError());
    }

    @Test
    public void shouldCreateValidDeviceAPI() throws Exception {
        mockAPI.willReturnSuccess(200);

        final MockCallback<Void> callback = new MockCallback<>();

        apiClient.device(ENROLLMENT_ID, DEVICE_ACCOUNT_TOKEN)
                .delete()
                .start(callback);

        RecordedRequest request = mockAPI.takeRequest();
        assertThat(request.getPath(), is(equalTo(String.format("/api/device-accounts/%s", ENROLLMENT_ID))));
        assertThat(request.getMethod(), is(equalTo("DELETE")));
        assertThat(request.getHeader("Authorization"), is(equalTo("Bearer " + DEVICE_ACCOUNT_TOKEN)));
    }

    private Map<String, Object> bodyFromRequest(RecordedRequest request) throws IOException {
        Gson gson = new GsonBuilder().create();
        Type type = new TypeToken<Map<String, Object>>() {}.getType();
        return gson.fromJson(new InputStreamReader(request.getBody().inputStream()), type);
    }

    private void verifyJWT(String jwt, boolean accepted, String rejectReason)
            throws SignatureException, NoSuchAlgorithmException, JWTVerifyException, InvalidKeyException, IOException {
        final String audience = HttpUrl.parse(apiClient.getUrl()).resolve("api/resolve-transaction").toString();
        final JWTVerifier jwtVerifier = new JWTVerifier(keyPair.getPublic(), audience, DEVICE_IDENTIFIER);
        final Map<String, Object> payload = jwtVerifier.verify(jwt);
        assertThat(payload, hasEntry("aud", (Object) audience));
        assertThat(payload, hasEntry("sub", (Object) CHALLENGE));
        assertThat(payload, hasEntry("iss", (Object) DEVICE_IDENTIFIER));
        assertThat(payload, hasEntry("auth0_guardian_accepted", (Object) accepted));
        assertThat(payload, hasEntry("auth0_guardian_method", (Object) "push"));
        if (!accepted && rejectReason != null) {
            assertThat(payload, hasEntry("auth0_guardian_reason", (Object) rejectReason));
        } else {
            assertThat(payload.containsKey("auth0_guardian_reason"), is(equalTo(false)));
        }
        assertThat(payload, hasKey("iat"));
        assertThat(payload, hasKey("exp"));

        Integer iat = (Integer) payload.get("iat");
        Integer exp = (Integer) payload.get("exp");

        int currentTime = (int)(new Date().getTime() / 1000L);
        assertThat(iat, is(lessThanOrEqualTo(currentTime)));
        assertThat(exp, is(greaterThan(currentTime)));
        assertThat(exp - iat, is(equalTo(30)));
    }
}