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

import com.auth0.android.guardian.sdk.networking.RequestFactory;
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

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.RecordedRequest;

import static com.auth0.android.guardian.sdk.utils.CallbackMatcher.hasNoError;
import static com.auth0.android.guardian.sdk.utils.CallbackMatcher.hasPayloadOfType;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class DeviceAPIClientTest {

    private static final String DEVICE_ACCOUNT_TOKEN = "DEVICE_ACCOUNT_TOKEN";
    private static final String DEVICE_ID = "DEVICE_ID";
    private static final String DEVICE_IDENTIFIER = "DEVICE_IDENTIFIER";
    private static final String DEVICE_NAME = "DEVICE_NAME";
    private static final String PUSH_SERVICE = "PUSH_SERVICE";
    private static final String PUSH_TOKEN = "PUSH_TOKEN";

    MockWebService mockAPI;

    DeviceAPIClient apiClient;

    Gson gson;

    @Before
    public void setUp() throws Exception {
        mockAPI = new MockWebService();
        final String domain = mockAPI.getDomain();

        gson = new GsonBuilder()
                .create();

        RequestFactory requestFactory = new RequestFactory(gson, new OkHttpClient());

        apiClient = new DeviceAPIClient(requestFactory, HttpUrl.parse(domain), DEVICE_ID, DEVICE_ACCOUNT_TOKEN);
    }

    @After
    public void tearDown() throws Exception {
        mockAPI.shutdown();
    }

    @Test
    public void shouldDeleteDevice() throws Exception {
        mockAPI.willReturnSuccess(204);

        final MockCallback<Void> callback = new MockCallback<>();

        apiClient
                .delete()
                .start(callback);

        RecordedRequest request = mockAPI.takeRequest();
        assertThat(request.getPath(), is(equalTo(String.format("/api/device-accounts/%s", DEVICE_ID))));
        assertThat(request.getMethod(), is(equalTo("DELETE")));
        assertThat(request.getHeader("Authorization"), is(equalTo("Bearer " + DEVICE_ACCOUNT_TOKEN)));

        assertThat(callback, hasNoError());
    }

    @Test
    public void shouldCreateDevice() throws Exception {
        mockAPI.willReturnDeviceAccount(DEVICE_ID, DEVICE_IDENTIFIER, DEVICE_NAME, PUSH_SERVICE, PUSH_TOKEN);

        final MockCallback<Device> callback = new MockCallback<>();

        apiClient
                .create(DEVICE_NAME, PUSH_TOKEN)
                .start(callback);

        RecordedRequest request = mockAPI.takeRequest();
        assertThat(request.getPath(), is(equalTo(String.format("/api/device-accounts/%s", DEVICE_ID))));
        assertThat(request.getMethod(), is(equalTo("PATCH")));
        assertThat(request.getHeader("Authorization"), is(equalTo("Bearer " + DEVICE_ACCOUNT_TOKEN)));

        Map<String, Object> body = bodyFromRequest(request);
        assertThat(body, hasEntry("identifier", (Object) "android_id"));
        assertThat(body, hasEntry("name", (Object) DEVICE_NAME));
        assertThat(body, hasKey("push_credentials"));

        Map<String, Object> pushCredentials = (Map<String, Object>) body.get("push_credentials");
        assertThat(pushCredentials, hasEntry("service", (Object) "GCM"));
        assertThat(pushCredentials, hasEntry("token", (Object) PUSH_TOKEN));

        assertThat(callback, hasPayloadOfType(Device.class));
    }

    @Test
    public void shouldUpdateDevice() throws Exception {
        mockAPI.willReturnDeviceAccount(DEVICE_ID, DEVICE_IDENTIFIER, DEVICE_NAME, PUSH_SERVICE, PUSH_TOKEN);

        final MockCallback<Device> callback = new MockCallback<>();

        apiClient
                .update(DEVICE_IDENTIFIER, DEVICE_NAME, PUSH_TOKEN)
                .start(callback);

        RecordedRequest request = mockAPI.takeRequest();
        assertThat(request.getPath(), is(equalTo(String.format("/api/device-accounts/%s", DEVICE_ID))));
        assertThat(request.getMethod(), is(equalTo("PATCH")));
        assertThat(request.getHeader("Authorization"), is(equalTo("Bearer " + DEVICE_ACCOUNT_TOKEN)));

        Map<String, Object> body = bodyFromRequest(request);
        assertThat(body, hasEntry("name", (Object) DEVICE_NAME));
        assertThat(body, hasEntry("identifier", (Object) DEVICE_IDENTIFIER));
        assertThat(body, hasKey("push_credentials"));
        assertThat(body.size(), is(equalTo(3)));

        Map<String, Object> pushCredentials = (Map<String, Object>) body.get("push_credentials");
        assertThat(pushCredentials, hasEntry("service", (Object) "GCM"));
        assertThat(pushCredentials, hasEntry("token", (Object) PUSH_TOKEN));
        assertThat(pushCredentials.size(), is(equalTo(2)));

        assertThat(callback, hasPayloadOfType(Device.class));
    }

    @Test
    public void shouldUpdateIdentifier() throws Exception {
        mockAPI.willReturnDeviceAccount(DEVICE_ID, DEVICE_IDENTIFIER, DEVICE_NAME, PUSH_SERVICE, PUSH_TOKEN);

        final MockCallback<Device> callback = new MockCallback<>();

        apiClient
                .update()
                .localIdentifier(DEVICE_IDENTIFIER)
                .start(callback);

        RecordedRequest request = mockAPI.takeRequest();
        assertThat(request.getPath(), is(equalTo(String.format("/api/device-accounts/%s", DEVICE_ID))));
        assertThat(request.getMethod(), is(equalTo("PATCH")));
        assertThat(request.getHeader("Authorization"), is(equalTo("Bearer " + DEVICE_ACCOUNT_TOKEN)));

        Map<String, Object> body = bodyFromRequest(request);
        assertThat(body, hasEntry("identifier", (Object) DEVICE_IDENTIFIER));
        assertThat(body.size(), is(equalTo(1)));

        assertThat(callback, hasPayloadOfType(Device.class));
    }

    @Test
    public void shouldUpdateName() throws Exception {
        mockAPI.willReturnDeviceAccount(DEVICE_ID, DEVICE_IDENTIFIER, DEVICE_NAME, PUSH_SERVICE, PUSH_TOKEN);

        final MockCallback<Device> callback = new MockCallback<>();

        apiClient
                .update()
                .name(DEVICE_NAME)
                .start(callback);

        RecordedRequest request = mockAPI.takeRequest();
        assertThat(request.getPath(), is(equalTo(String.format("/api/device-accounts/%s", DEVICE_ID))));
        assertThat(request.getMethod(), is(equalTo("PATCH")));
        assertThat(request.getHeader("Authorization"), is(equalTo("Bearer " + DEVICE_ACCOUNT_TOKEN)));

        Map<String, Object> body = bodyFromRequest(request);
        assertThat(body, hasEntry("name", (Object) DEVICE_NAME));
        assertThat(body.size(), is(equalTo(1)));

        assertThat(callback, hasPayloadOfType(Device.class));
    }

    @Test
    public void shouldUpdateGCMToken() throws Exception {
        mockAPI.willReturnDeviceAccount(DEVICE_ID, DEVICE_IDENTIFIER, DEVICE_NAME, PUSH_SERVICE, PUSH_TOKEN);

        final MockCallback<Device> callback = new MockCallback<>();

        apiClient
                .update()
                .GCMToken(PUSH_TOKEN)
                .start(callback);

        RecordedRequest request = mockAPI.takeRequest();
        assertThat(request.getPath(), is(equalTo(String.format("/api/device-accounts/%s", DEVICE_ID))));
        assertThat(request.getMethod(), is(equalTo("PATCH")));
        assertThat(request.getHeader("Authorization"), is(equalTo("Bearer " + DEVICE_ACCOUNT_TOKEN)));

        Map<String, Object> body = bodyFromRequest(request);
        assertThat(body, hasKey("push_credentials"));
        assertThat(body.size(), is(equalTo(1)));

        Map<String, Object> pushCredentials = (Map<String, Object>) body.get("push_credentials");
        assertThat(pushCredentials, hasEntry("service", (Object) "GCM"));
        assertThat(pushCredentials, hasEntry("token", (Object) PUSH_TOKEN));
        assertThat(pushCredentials.size(), is(equalTo(2)));

        assertThat(callback, hasPayloadOfType(Device.class));
    }

    @Test
    public void shouldUpdateNameAndGCMTokenAndIdentifier() throws Exception {
        mockAPI.willReturnDeviceAccount(DEVICE_ID, DEVICE_IDENTIFIER, DEVICE_NAME, PUSH_SERVICE, PUSH_TOKEN);

        final MockCallback<Device> callback = new MockCallback<>();

        apiClient
                .update()
                .localIdentifier(DEVICE_IDENTIFIER)
                .name(DEVICE_NAME)
                .GCMToken(PUSH_TOKEN)
                .start(callback);

        RecordedRequest request = mockAPI.takeRequest();
        assertThat(request.getPath(), is(equalTo(String.format("/api/device-accounts/%s", DEVICE_ID))));
        assertThat(request.getMethod(), is(equalTo("PATCH")));
        assertThat(request.getHeader("Authorization"), is(equalTo("Bearer " + DEVICE_ACCOUNT_TOKEN)));

        Map<String, Object> body = bodyFromRequest(request);
        assertThat(body, hasEntry("identifier", (Object) DEVICE_IDENTIFIER));
        assertThat(body, hasEntry("name", (Object) DEVICE_NAME));
        assertThat(body, hasKey("push_credentials"));
        assertThat(body.size(), is(equalTo(3)));

        Map<String, Object> pushCredentials = (Map<String, Object>) body.get("push_credentials");
        assertThat(pushCredentials, hasEntry("service", (Object) "GCM"));
        assertThat(pushCredentials, hasEntry("token", (Object) PUSH_TOKEN));
        assertThat(pushCredentials.size(), is(equalTo(2)));

        assertThat(callback, hasPayloadOfType(Device.class));
    }

    private Map<String, Object> bodyFromRequest(RecordedRequest request) throws IOException {
        Type type = new TypeToken<Map<String, Object>>() {}.getType();
        return gson.fromJson(new InputStreamReader(request.getBody().inputStream()), type);
    }
}