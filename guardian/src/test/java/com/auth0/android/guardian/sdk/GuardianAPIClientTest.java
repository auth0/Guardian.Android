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

import static com.auth0.android.guardian.sdk.utils.CallbackMatcher.hasNoError;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import android.net.Uri;
import android.os.Build;
import android.util.Base64;

import com.auth0.android.guardian.sdk.networking.Callback;
import com.auth0.android.guardian.sdk.networking.RequestFactory;
import com.auth0.android.guardian.sdk.utils.MockCallback;
import com.auth0.android.guardian.sdk.utils.MockWebService;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
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
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.mockwebserver.RecordedRequest;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23, manifest = Config.NONE)
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
    private static final byte[] PUBLIC_KEY_EXPONENT = new byte[]{0x01, 0x00, 0x01};
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
    private static final Integer THIRTY_SECONDS = 30;
    private static final Integer TWO_HOURS_IN_SECONDS = 2 * 60 * 60;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    RSAPublicKey publicKey;

    @Mock
    Callback<Map<String, Object>> enrollCallback;

    MockWebService mockAPI;
    GuardianAPIClient apiClient;
    KeyPair keyPair;
    RequestFactory requestFactory;
    RichConsentsAPIClient richConsentsAPIClient;

    @Before
    public void setUp() throws Exception {
        openMocks(this);

        when(publicKey.getModulus())
                .thenReturn(new BigInteger(PUBLIC_KEY_MODULUS));
        when(publicKey.getPublicExponent())
                .thenReturn(new BigInteger(PUBLIC_KEY_EXPONENT));

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        keyPair = keyPairGenerator.generateKeyPair();

        mockAPI = new MockWebService();
        final String domain = mockAPI.getDomain();

        requestFactory = provideRequestFactory(provideOkHttpClient());

        apiClient = new GuardianAPIClient.Builder()
                .url(Uri.parse(domain))
                .setRequestFactory(requestFactory)
                .setClientInfo(new ClientInfo())
                .build();

        richConsentsAPIClient = new RichConsentsAPIClient(requestFactory, Uri.parse(domain), new ClientInfo());
    }

    private OkHttpClient provideOkHttpClient() {
        final OkHttpClient.Builder builder = new OkHttpClient.Builder();

        builder.addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                okhttp3.Request originalRequest = chain.request();
                okhttp3.Request requestWithUserAgent = originalRequest.newBuilder()
                        .header("Accept-Language",
                                Locale.getDefault().toString())
                        .header("User-Agent",
                                String.format("GuardianSDK/%s Android %s",
                                        BuildConfig.VERSION_NAME,
                                        Build.VERSION.RELEASE))
                        .build();
                return chain.proceed(requestWithUserAgent);
            }
        });

        return builder.build();
    }

    private RequestFactory provideRequestFactory(OkHttpClient okHttpClient) {
        Gson gson = new GsonBuilder().create();
        return new RequestFactory(gson, okHttpClient);
    }

    @After
    public void tearDown() throws Exception {
        mockAPI.shutdown();
    }

    @Test
    public void shouldBuildWithDomain() throws Exception {
        GuardianAPIClient apiClient = new GuardianAPIClient.Builder()
                .domain("example.guardian.auth0.com")
                .setRequestFactory(requestFactory)
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
                .setRequestFactory(requestFactory)
                .build();
    }

    @Test
    public void shouldFailIfUrlWasAlreadySet() throws Exception {
        exception.expect(IllegalArgumentException.class);

        new GuardianAPIClient.Builder()
                .url(Uri.parse("https://example.guardian.auth0.com"))
                .domain("example.guardian.auth0.com")
                .setRequestFactory(requestFactory)
                .build();
    }

    @Test
    public void shouldFailIfNoUrlOrDomainConfigured() throws Exception {
        exception.expect(IllegalStateException.class);

        new GuardianAPIClient.Builder()
                .setRequestFactory(requestFactory)
                .build();
    }

    @Test
    public void shouldNotAddPathComponentToUrlWithGuardianAuth0Suffix() {
        Uri inputUrl = Uri.parse("https://samples.guardian.auth0.com");
        String expectedUrl = "https://samples.guardian.auth0.com/";

        GuardianAPIClient apiClient = new GuardianAPIClient.Builder()
                .url(inputUrl)
                .setRequestFactory(requestFactory)
                .build();

        assertEquals(expectedUrl, apiClient.getUrl());
    }


    @Test
    public void shouldNotAddPathComponentToUrlWithGuardianRegionAuth0Com() {
        Uri inputUrl = Uri.parse("https://samples.guardian.en.auth0.com");
        String expectedUrl = "https://samples.guardian.en.auth0.com/";

        GuardianAPIClient apiClient = new GuardianAPIClient.Builder()
                .url(inputUrl)
                .setRequestFactory(requestFactory)
                .build();

        assertEquals(expectedUrl, apiClient.getUrl());
    }

    @Test
    public void shouldNotAddPathComponentToCustomUrlWithoutGuardianWithAlreadyAddedPathComponent() {
        Uri inputUrl = Uri.parse("https://samples.auth0.com/appliance-mfa");
        String expectedUrl = "https://samples.auth0.com/appliance-mfa";

        GuardianAPIClient apiClient = new GuardianAPIClient.Builder()
                .url(inputUrl)
                .setRequestFactory(requestFactory)
                .build();

        assertEquals(expectedUrl, apiClient.getUrl());
    }

    @Test
    public void shouldAddPathComponentToCustomUrlWithoutGuardianWithoutAlreadyAddedPathComponent() {
        Uri inputUrl = Uri.parse("https://samples.auth0.com");
        String expectedUrl = "https://samples.auth0.com/appliance-mfa";

        GuardianAPIClient apiClient = new GuardianAPIClient.Builder()
                .url(inputUrl)
                .setRequestFactory(requestFactory)
                .build();

        assertEquals(expectedUrl, apiClient.getUrl());
    }

    @Test
    public void shouldAddPathComponentToCustomUrlWithGuardianWithoutAlreadyAddedPathComponent() {
        Uri inputUrl = Uri.parse("https://samples.guardian.some.thing.auth0.com");
        String expectedUrl = "https://samples.guardian.some.thing.auth0.com/appliance-mfa";

        GuardianAPIClient apiClient = new GuardianAPIClient.Builder()
                .url(inputUrl)
                .setRequestFactory(requestFactory)
                .build();

        assertEquals(expectedUrl, apiClient.getUrl());
    }

    @Test
    public void shouldHaveCustomUserAgentAndLanguageHeader() throws Exception {
        mockAPI.willReturnEnrollment(ENROLLMENT_ID, ENROLLMENT_URL, ENROLLMENT_ISSUER, ENROLLMENT_USER,
                DEVICE_ACCOUNT_TOKEN, RECOVERY_CODE, TOTP_SECRET, TOTP_ALGORITHM, TOTP_DIGITS, TOTP_PERIOD);

        final MockCallback<Map<String, Object>> callback = new MockCallback<>();

        apiClient.enroll(ENROLLMENT_TICKET, DEVICE_IDENTIFIER, DEVICE_NAME, GCM_TOKEN, publicKey)
                .start(callback);

        RecordedRequest request = mockAPI.takeRequest();
        assertThat(request.getHeader("User-Agent"),
                is(equalTo(
                        String.format("GuardianSDK/%s Android %s",
                                BuildConfig.VERSION_NAME,
                                Build.VERSION.RELEASE))));

        assertThat(request.getHeader("Accept-Language"),
                is(equalTo(Locale.getDefault().toString())));
    }

    @Test
    public void shouldCorrectlySetClientHeader() throws Exception {
        mockAPI.willReturnEnrollment(ENROLLMENT_ID, ENROLLMENT_URL, ENROLLMENT_ISSUER, ENROLLMENT_USER,
                DEVICE_ACCOUNT_TOKEN, RECOVERY_CODE, TOTP_SECRET, TOTP_ALGORITHM, TOTP_DIGITS, TOTP_PERIOD);

        final MockCallback<Map<String, Object>> callback = new MockCallback<>();

        final String STUB_APP_NAME = "SomeCoolApp";
        final String STUB_APP_VERSION = "1.2.3.4";
        final ClientInfo.TelemetryInfo telemetryInfo = new ClientInfo.TelemetryInfo(STUB_APP_NAME, STUB_APP_VERSION);
        final ClientInfo clientInfo = new ClientInfo(telemetryInfo);


        GuardianAPIClient testApiClient = new GuardianAPIClient.Builder()
                .url(Uri.parse(this.mockAPI.getDomain()))
                .setClientInfo(clientInfo)
                .setRequestFactory(requestFactory)
                .build();

        testApiClient.enroll(ENROLLMENT_TICKET, DEVICE_IDENTIFIER, DEVICE_NAME, GCM_TOKEN, publicKey)
                .start(callback);

        RecordedRequest request = mockAPI.takeRequest();

        String auth0ClientEncoded = request.getHeader("Auth0-Client");
        assertThat(auth0ClientEncoded, is(notNullValue()));

        byte[] auth0ClientDecoded = Base64.decode(
                auth0ClientEncoded, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);

        Gson gson = new GsonBuilder().create();

        ClientInfo auth0Client = gson.fromJson(
                new InputStreamReader(
                        new ByteArrayInputStream(auth0ClientDecoded)), ClientInfo.class);

        assertThat(auth0Client, is(notNullValue()));

        assertThat(auth0Client.telemetryInfo, is(notNullValue()));
        assertThat(auth0Client.telemetryInfo.appName, equalTo(STUB_APP_NAME));
        assertThat(auth0Client.telemetryInfo.appVersion, equalTo(STUB_APP_VERSION));
    }

    @Test
    public void shouldNotSetTelemetryInfoIfNoneIsProvided() throws Exception {
        mockAPI.willReturnEnrollment(ENROLLMENT_ID, ENROLLMENT_URL, ENROLLMENT_ISSUER, ENROLLMENT_USER,
                DEVICE_ACCOUNT_TOKEN, RECOVERY_CODE, TOTP_SECRET, TOTP_ALGORITHM, TOTP_DIGITS, TOTP_PERIOD);

        final MockCallback<Map<String, Object>> callback = new MockCallback<>();

        GuardianAPIClient testApiClient = new GuardianAPIClient.Builder()
                .url(Uri.parse(this.mockAPI.getDomain()))
                .setRequestFactory(requestFactory)
                .setClientInfo(new ClientInfo())
                .build();

        testApiClient.enroll(ENROLLMENT_TICKET, DEVICE_IDENTIFIER, DEVICE_NAME, GCM_TOKEN, publicKey)
                .start(callback);

        RecordedRequest request = mockAPI.takeRequest();

        String auth0ClientEncoded = request.getHeader("Auth0-Client");
        assertThat(auth0ClientEncoded, is(notNullValue()));

        byte[] auth0ClientDecoded = Base64.decode(
                auth0ClientEncoded, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);

        Gson gson = new GsonBuilder().create();

        ClientInfo auth0Client = gson.fromJson(
                new InputStreamReader(
                        new ByteArrayInputStream(auth0ClientDecoded)), ClientInfo.class);

        assertThat(auth0Client, is(notNullValue()));
        assertThat(auth0Client.telemetryInfo, is(nullValue()));
    }

    @Test
    public void shouldEnroll() throws Exception {
        mockAPI.willReturnEnrollment(ENROLLMENT_ID, ENROLLMENT_URL, ENROLLMENT_ISSUER, ENROLLMENT_USER,
                DEVICE_ACCOUNT_TOKEN, RECOVERY_CODE, TOTP_SECRET, TOTP_ALGORITHM, TOTP_DIGITS, TOTP_PERIOD);

        apiClient.enroll(ENROLLMENT_TICKET, DEVICE_IDENTIFIER, DEVICE_NAME, GCM_TOKEN, publicKey)
                .start(enrollCallback);

        RecordedRequest request = mockAPI.takeRequest();
        assertThat(request.getPath(), is(equalTo("/appliance-mfa/api/enroll")));
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

        verify(enrollCallback, timeout(100)).onSuccess(anyMap());

        verifyNoMoreInteractions(enrollCallback);
    }

    @Test
    public void shouldCallTheRightUrlWhenUsingPathSegmentsWithoutTrailingSlash() throws Exception {
        MockWebService newMockWebService = new MockWebService();
        final String domain = newMockWebService.getDomain();
        GuardianAPIClient apiClientPSaaS = new GuardianAPIClient.Builder()
                .url(Uri.parse(domain + "appliance-mfa"))
                .setRequestFactory(requestFactory)
                .setClientInfo(new ClientInfo())
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
    public void shouldCallTheRightUrlWhenUsingPathSegmentsWithTrailingSlash() throws Exception {
        MockWebService newMockWebService = new MockWebService();
        final String domain = newMockWebService.getDomain();
        GuardianAPIClient apiClientPSaaS = new GuardianAPIClient.Builder()
                .url(Uri.parse(domain + "appliance-mfa/"))
                .setRequestFactory(requestFactory)
                .setClientInfo(new ClientInfo())
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

        final MockCallback<Map<String, Object>> callback = new MockCallback<>();

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
        assertThat(request.getPath(), is(equalTo("/appliance-mfa/api/resolve-transaction")));
        assertThat(request.getMethod(), is(equalTo("POST")));
        assertThat(request.getHeader("Authorization"), is(equalTo("Bearer " + TX_TOKEN)));

        Map<String, Object> body = bodyFromRequest(request);
        assertThat(body, hasKey("challenge_response"));

        String jwt = (String) body.get("challenge_response");
        verifyAccessApprovalJWT(jwt, true, null);

        assertThat(callback, hasNoError());
    }

    @Test
    public void shouldRejectLoginWithPrivateKey() throws Exception {
        mockAPI.willReturnSuccess(204);

        final MockCallback<Void> callback = new MockCallback<>();

        apiClient.reject(TX_TOKEN, DEVICE_IDENTIFIER, CHALLENGE, keyPair.getPrivate(), "hack")
                .start(callback);

        RecordedRequest request = mockAPI.takeRequest();
        assertThat(request.getPath(), is(equalTo("/appliance-mfa/api/resolve-transaction")));
        assertThat(request.getMethod(), is(equalTo("POST")));
        assertThat(request.getHeader("Authorization"), is(equalTo("Bearer " + TX_TOKEN)));

        Map<String, Object> body = bodyFromRequest(request);
        assertThat(body, hasKey("challenge_response"));

        String jwt = (String) body.get("challenge_response");
        verifyAccessApprovalJWT(jwt, false, "hack");

        assertThat(callback, hasNoError());
    }

    @Test
    public void shouldRejectLoginWithoutReasonWithPrivateKey() throws Exception {
        mockAPI.willReturnSuccess(204);

        final MockCallback<Void> callback = new MockCallback<>();

        apiClient.reject(TX_TOKEN, DEVICE_IDENTIFIER, CHALLENGE, keyPair.getPrivate())
                .start(callback);

        RecordedRequest request = mockAPI.takeRequest();
        assertThat(request.getPath(), is(equalTo("/appliance-mfa/api/resolve-transaction")));
        assertThat(request.getMethod(), is(equalTo("POST")));
        assertThat(request.getHeader("Authorization"), is(equalTo("Bearer " + TX_TOKEN)));

        Map<String, Object> body = bodyFromRequest(request);
        assertThat(body, hasKey("challenge_response"));

        String jwt = (String) body.get("challenge_response");
        verifyAccessApprovalJWT(jwt, false, null);

        assertThat(callback, hasNoError());
    }

    @Test
    public void shouldCreateValidDeviceAPIWithOpaqueToken() throws Exception {
        mockAPI.willReturnSuccess(200);

        final MockCallback<Void> callback = new MockCallback<>();

        apiClient.device(ENROLLMENT_ID, DEVICE_ACCOUNT_TOKEN)
                .delete()
                .start(callback);

        RecordedRequest request = mockAPI.takeRequest();
        assertThat(request.getPath(), is(equalTo(String.format("/appliance-mfa/api/device-accounts/%s", ENROLLMENT_ID))));
        assertThat(request.getMethod(), is(equalTo("DELETE")));
        assertThat(request.getHeader("Authorization"), is(equalTo("Bearer " + DEVICE_ACCOUNT_TOKEN)));
    }

    @Test
    public void shouldCreateValidDeviceAPIWithJWT() throws Exception {
        mockAPI.willReturnSuccess(200);

        final MockCallback<Void> callback = new MockCallback<>();

        apiClient.device(ENROLLMENT_ID, ENROLLMENT_USER, keyPair.getPrivate())
                .delete()
                .start(callback);

        RecordedRequest request = mockAPI.takeRequest();
        assertThat(request.getPath(), is(equalTo(String.format("/appliance-mfa/api/device-accounts/%s", ENROLLMENT_ID))));
        assertThat(request.getMethod(), is(equalTo("DELETE")));
        String authorization = request.getHeader("Authorization");
        assertThat(authorization, Matchers.startsWith("Bearer "));
        String jwt = authorization.split("Bearer ")[1];
        verifyBasicJWT(jwt);
    }

    @Test
    public void shouldCreateValidRichConsentsAPI() throws Exception {
        String consentId = "cns_00000001";
        mockAPI.willReturnRichConsent(consentId, "https://api", "openid", "test");

        final MockCallback<RichConsent> callback = new MockCallback<>();

        richConsentsAPIClient.fetch(consentId, "token", keyPair.getPrivate(), keyPair.getPublic())
                .start(callback);

        RecordedRequest request = mockAPI.takeRequest();
        assertThat(request.getPath(), is(equalTo(String.format("/rich-consents/%s", consentId))));
    }

    private Map<String, Object> bodyFromRequest(RecordedRequest request) throws IOException {
        Gson gson = new GsonBuilder().create();
        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        return gson.fromJson(new InputStreamReader(request.getBody().inputStream()), type);
    }

    private void verifyBasicJWT(String jwt) throws SignatureException, NoSuchAlgorithmException, JWTVerificationException, InvalidKeyException, IOException {
        final String audience = HttpUrl.parse(apiClient.getUrl()).resolve("/appliance-mfa/api/device-accounts").toString();
        DecodedJWT payload = verifyJWT(jwt, ENROLLMENT_ID, audience, TWO_HOURS_IN_SECONDS * 1000L);
        assertThat(payload.getSubject(), is(equalTo(ENROLLMENT_USER)));
        assertThat(payload.getClaim("auth0_guardian_accepted").isMissing(), is(equalTo(true)));
        assertThat(payload.getClaim("auth0_guardian_method").isMissing(), is(equalTo(true)));
        assertThat(payload.getClaim("auth0_guardian_reason").isMissing(), is(equalTo(true)));
    }

    private void verifyAccessApprovalJWT(String jwt, boolean accepted, String rejectReason) throws NoSuchAlgorithmException, SignatureException, JWTVerificationException, InvalidKeyException, IOException {
        final String audience = HttpUrl.parse(apiClient.getUrl()).resolve("/appliance-mfa/api/resolve-transaction").toString();
        DecodedJWT payload = verifyJWT(jwt, DEVICE_IDENTIFIER, audience, THIRTY_SECONDS * 1000L);
        assertThat(payload.getSubject(), is(equalTo(CHALLENGE)));
        assertThat(payload.getClaim("auth0_guardian_accepted").asBoolean(), is(equalTo(accepted)));
        assertThat(payload.getClaim("auth0_guardian_method").asString(), is(equalTo("push")));
        if (!accepted && rejectReason != null) {
            assertThat(payload.getClaim("auth0_guardian_reason").asString(), is(equalTo(rejectReason)));
        } else {
            assertThat(payload.getClaim("auth0_guardian_reason").isMissing(), is(equalTo(true)));
        }
    }

    private DecodedJWT verifyJWT(String jwt, String issuer, String audience, long expiresIn) throws SignatureException, NoSuchAlgorithmException, JWTVerificationException, InvalidKeyException, IOException {
        RSAPublicKey pub = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey priv = (RSAPrivateKey) keyPair.getPrivate();
        Algorithm algorithm = Algorithm.RSA256(pub, priv);
        JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer(issuer)
                .withAudience(audience)
                .build();
        DecodedJWT payload = verifier.verify(jwt);

        List<String> aud = payload.getAudience();
        String iss = payload.getIssuer();
        assertThat(aud, is(not(empty())));
        assertThat(iss, is(not(blankOrNullString())));

        long iat = payload.getIssuedAt().getTime();
        long exp = payload.getExpiresAt().getTime();

        long currentTime = new Date().getTime();
        assertThat(iat, is(lessThanOrEqualTo(currentTime)));
        assertThat(exp, is(greaterThan(currentTime)));
        assertThat(exp - iat, is(equalTo(expiresIn)));
        return payload;
    }
}