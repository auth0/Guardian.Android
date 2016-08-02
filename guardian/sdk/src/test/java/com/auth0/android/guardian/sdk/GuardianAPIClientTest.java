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

import com.auth0.android.guardian.sdk.utils.CallbackMatcher;
import com.auth0.android.guardian.sdk.utils.MockCallback;
import com.auth0.android.guardian.sdk.utils.MockWebService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;

import static com.auth0.android.guardian.sdk.utils.CallbackMatcher.hasNoError;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class GuardianAPIClientTest {

    private static final String ENROLLMENT_TX_ID = "ENROLLMENT_TX_ID";
    private static final String DEVICE_ID = "DEVICE_ID";
    private static final String DEVICE_ACCOUNT_TOKEN = "DEVICE_ACCOUNT_TOKEN";
    private static final String TX_TOKEN = "TX_TOKEN";

    MockWebService mockAPI;

    GuardianAPIClient apiClient;

    Gson gson;

    @Before
    public void setUp() throws Exception {
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
    public void shouldHaveCustomUserAgentHeader() throws Exception {
        mockAPI.willReturnEnrollmentInfo(DEVICE_ACCOUNT_TOKEN);

        final MockCallback<String> callback = new MockCallback<>();

        apiClient.getDeviceToken(ENROLLMENT_TX_ID)
                .start(callback);

        RecordedRequest request = mockAPI.takeRequest();
        assertThat(request.getHeader("User-Agent"), is(equalTo(
                String.format("GuardianSDK/%s(%s) Android %s",
                        BuildConfig.VERSION_NAME,
                        BuildConfig.VERSION_CODE,
                        Build.VERSION.RELEASE))));
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

        apiClient.device(DEVICE_ID, DEVICE_ACCOUNT_TOKEN)
                .delete()
                .start(callback);

        RecordedRequest request = mockAPI.takeRequest();
        assertThat(request.getPath(), is(equalTo(String.format("/api/device-accounts/%s", DEVICE_ID))));
        assertThat(request.getMethod(), is(equalTo("DELETE")));
        assertThat(request.getHeader("Authorization"), is(equalTo("Bearer " + DEVICE_ACCOUNT_TOKEN)));
    }

    private Map<String, Object> bodyFromRequest(RecordedRequest request) throws IOException {
        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        return gson.fromJson(new InputStreamReader(request.getBody().inputStream()), type);
    }
}