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

import android.os.Build;
import android.util.Base64;

import com.auth0.android.guardian.sdk.utils.CallbackMatcher;
import com.auth0.android.guardian.sdk.utils.MockCallback;
import com.auth0.android.guardian.sdk.utils.MockWebService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Locale;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;

import static com.auth0.android.guardian.sdk.utils.CallbackMatcher.hasNoError;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 23, manifest = Config.NONE)
public class GuardianAPIClientTest {

    private static final String ENROLLMENT_TICKET = "ENROLLMENT_TICKET";
    private static final String ENROLLMENT_TX_ID = "ENROLLMENT_TX_ID";
    private static final String ENROLLMENT_ID = "ENROLLMENT_ID";
    private static final String DEVICE_ACCOUNT_TOKEN = "DEVICE_ACCOUNT_TOKEN";
    private static final String TX_TOKEN = "TX_TOKEN";
    private static final String ENROLLMENT_URL = "https://example.guardian.auth0.com/";
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
    private static final byte[] PUBLIC_KEY = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    MockWebService mockAPI;
    GuardianAPIClient apiClient;
    Gson gson;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        mockAPI = new MockWebService();
        final String domain = mockAPI.getDomain();

        gson = new GsonBuilder()
                .create();

        apiClient = new GuardianAPIClient.Builder()
                .baseUrl(domain)
                .gson(gson)
                .build();
    }

    @After
    public void tearDown() throws Exception {
        mockAPI.shutdown();
    }

    @Test
    public void shouldHaveCustomUserAgentAndLanguageHeader() throws Exception {
        mockAPI.willReturnEnrollmentInfo(DEVICE_ACCOUNT_TOKEN);

        final MockCallback<String> callback = new MockCallback<>();

        apiClient.getDeviceToken(ENROLLMENT_TX_ID)
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
    }

    @Test
    public void shouldEnroll() throws Exception {
        mockAPI.willReturnEnrollment(ENROLLMENT_ID, ENROLLMENT_URL, ENROLLMENT_ISSUER, ENROLLMENT_USER,
                DEVICE_ACCOUNT_TOKEN, RECOVERY_CODE, TOTP_SECRET, TOTP_ALGORITHM, TOTP_DIGITS, TOTP_PERIOD);

        final MockCallback<Map<String,Object>> callback = new MockCallback<>();

        apiClient.enroll(ENROLLMENT_TICKET, DEVICE_IDENTIFIER, DEVICE_NAME, GCM_TOKEN, PUBLIC_KEY)
                .start(callback);

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
        assertThat(publicKey, hasEntry("e", (Object) "AQAB"));
        assertThat(publicKey, hasEntry("n", (Object) Base64.encodeToString(PUBLIC_KEY, Base64.DEFAULT)));
        assertThat(publicKey.size(), is(equalTo(3)));
    }

    @Test
    public void shouldGetDeviceToken() throws Exception {
        mockAPI.willReturnEnrollmentInfo(DEVICE_ACCOUNT_TOKEN);

        final MockCallback<String> callback = new MockCallback<>();

        apiClient.getDeviceToken(ENROLLMENT_TX_ID)
                .start(callback);

        RecordedRequest request = mockAPI.takeRequest();
        assertThat(request.getPath(), is(equalTo("/api/enrollment-info")));
        assertThat(request.getMethod(), is(equalTo("POST")));

        Map<String, Object> body = bodyFromRequest(request);
        assertThat(body, hasEntry("enrollment_tx_id", (Object) ENROLLMENT_TX_ID));

        assertThat(callback, CallbackMatcher.hasPayloadOfType(String.class));
        String deviceToken = callback.payload().call();
        assertThat(deviceToken, is(equalTo(DEVICE_ACCOUNT_TOKEN)));
    }

    @Test
    public void shouldAllowLogin() throws Exception {
        mockAPI.willReturnSuccess(204);

        final MockCallback<Void> callback = new MockCallback<>();

        apiClient.allow(TX_TOKEN, "123456")
                .start(callback);

        RecordedRequest request = mockAPI.takeRequest();
        assertThat(request.getPath(), is(equalTo("/api/verify-otp")));
        assertThat(request.getMethod(), is(equalTo("POST")));
        assertThat(request.getHeader("Authorization"), is(equalTo("Bearer " + TX_TOKEN)));

        Map<String, Object> body = bodyFromRequest(request);
        assertThat(body, hasEntry("type", (Object) "push_notification"));
        assertThat(body, hasEntry("code", (Object) "123456"));

        assertThat(callback, hasNoError());
    }

    @Test
    public void shouldRejectLogin() throws Exception {
        mockAPI.willReturnSuccess(204);

        final MockCallback<Void> callback = new MockCallback<>();

        apiClient.reject(TX_TOKEN, "123456", "hack")
                .start(callback);

        RecordedRequest request = mockAPI.takeRequest();
        assertThat(request.getPath(), is(equalTo("/api/reject-login")));
        assertThat(request.getMethod(), is(equalTo("POST")));
        assertThat(request.getHeader("Authorization"), is(equalTo("Bearer " + TX_TOKEN)));

        Map<String, Object> body = bodyFromRequest(request);
        assertThat(body, hasEntry("code", (Object) "123456"));
        assertThat(body, hasEntry("reason", (Object) "hack"));

        assertThat(callback, hasNoError());
    }

    @Test
    public void shouldRejectLoginWithoutReason() throws Exception {
        mockAPI.willReturnSuccess(204);

        final MockCallback<Void> callback = new MockCallback<>();

        apiClient.reject(TX_TOKEN, "123456")
                .start(callback);

        RecordedRequest request = mockAPI.takeRequest();
        assertThat(request.getPath(), is(equalTo("/api/reject-login")));
        assertThat(request.getMethod(), is(equalTo("POST")));
        assertThat(request.getHeader("Authorization"), is(equalTo("Bearer " + TX_TOKEN)));

        Map<String, Object> body = bodyFromRequest(request);
        assertThat(body, hasEntry("code", (Object) "123456"));

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
        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        return gson.fromJson(new InputStreamReader(request.getBody().inputStream()), type);
    }
}