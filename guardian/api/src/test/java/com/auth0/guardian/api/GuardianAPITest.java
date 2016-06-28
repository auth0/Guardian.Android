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

package com.auth0.guardian.api;

import com.auth0.guardian.api.data.DeviceAccount;
import com.auth0.guardian.api.data.EnrollmentInfo;
import com.auth0.guardian.api.data.RejectReason;
import com.auth0.guardian.api.data.TenantInfo;
import com.auth0.guardian.api.exceptions.DeviceAccountNotFoundException;
import com.auth0.guardian.api.exceptions.EnrollmentTransactionNotFoundException;
import com.auth0.guardian.api.exceptions.InvalidOTPCodeException;
import com.auth0.guardian.api.exceptions.InvalidTokenException;
import com.auth0.guardian.api.exceptions.LoginTransactionNotFoundException;
import com.auth0.guardian.api.exceptions.UnparseableServerErrorException;
import com.auth0.guardian.api.util.CallbackMatcher;
import com.auth0.guardian.api.util.MockCallback;
import com.auth0.guardian.api.util.MockWebService;
import com.auth0.requests.gson.JsonRequiredTypeAdapterFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;

import static com.auth0.guardian.api.util.CallbackMatcher.hasNoError;
import static com.jayway.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Nicolas Ulrich (nikolaseu@gmail.com)
 */
public class GuardianAPITest {

    private static final String ENROLLMENT_TX_ID = "ENROLLMENT_TX_ID";
    private static final String DEVICE_ID = "DEVICE_ID";
    private static final String DEVICE_ACCOUNT_TOKEN = "DEVICE_ACCOUNT_TOKEN";
    private static final String DEVICE_IDENTIFIER = "DEVICE_IDENTIFIER";
    private static final String DEVICE_NAME = "DEVICE_NAME";
    private static final String PUSH_SERVICE = "PUSH_SERVICE";
    private static final String PUSH_TOKEN = "PUSH_TOKEN";
    private static final String TENANT_NAME = "TENANT_NAME";
    private static final String TENANT_FRIENDLY_NAME = "TENANT_FRIENDLY_NAME";
    private static final String TENANT_PICTURE_URL = "TENANT_PICTURE_URL";
    private static final String TX_TOKEN = "TX_TOKEN";

    MockWebService mockAPI;

    GuardianAPI webService;

    Gson gson;

    @Before
    public void setUp() throws Exception {
        mockAPI = new MockWebService();
        final String domain = mockAPI.getDomain();

        gson = new GsonBuilder()
                .registerTypeAdapterFactory(new JsonRequiredTypeAdapterFactory())
                .create();

        webService = new GuardianAPI.Builder()
                .baseUrl(domain)
                .gson(gson)
                .build();
    }

    @After
    public void tearDown() throws Exception {
        mockAPI.shutdown();
    }

    @Test
    public void shouldGetEnrollmentInfo() throws Exception {
        mockAPI.willReturnEnrollmentInfo(DEVICE_ACCOUNT_TOKEN);

        final MockCallback<EnrollmentInfo> callback = new MockCallback<>();

        webService.getEnrollmentInfo(ENROLLMENT_TX_ID)
                .start(callback);

        RecordedRequest request = mockAPI.takeRequest();
        assertThat(request.getPath(), is(equalTo("/api/enrollment-info")));
        assertThat(request.getMethod(), is(equalTo("POST")));

        Map<String, Object> body = bodyFromRequest(request);
        assertThat(body, hasEntry("enrollment_tx_id", (Object) ENROLLMENT_TX_ID));

        assertThat(callback, CallbackMatcher.hasPayloadOfType(EnrollmentInfo.class));
        EnrollmentInfo enrollmentInfo = callback.payload().call();
        assertThat(enrollmentInfo.getDeviceAccountToken(), is(equalTo(DEVICE_ACCOUNT_TOKEN)));
    }

    @Test
    public void shouldGetTenantInfo() throws Exception {
        mockAPI.willReturnTenantInfo(TENANT_NAME, TENANT_FRIENDLY_NAME, TENANT_PICTURE_URL);

        final MockCallback<TenantInfo> callback = new MockCallback<>();

        webService.getTenantInfo()
                .start(callback);

        RecordedRequest request = mockAPI.takeRequest();
        assertThat(request.getPath(), is(equalTo("/api/tenant-info")));
        assertThat(request.getMethod(), is(equalTo("GET")));

        assertThat(callback, CallbackMatcher.hasPayloadOfType(TenantInfo.class));
        TenantInfo tenantInfo = callback.payload().call();
        assertThat(tenantInfo.getName(), is(equalTo(TENANT_NAME)));
        assertThat(tenantInfo.getFriendlyName(), is(equalTo(TENANT_FRIENDLY_NAME)));
        assertThat(tenantInfo.getPictureUrl(), is(equalTo(TENANT_PICTURE_URL)));
    }

    @Test
    public void shouldGetRejectReasons() throws Exception {
        mockAPI.willReturnRejectReasons();

        final MockCallback<List<RejectReason>> callback = new MockCallback<>();

        webService.getRejectReasons()
                .start(callback);

        RecordedRequest request = mockAPI.takeRequest();
        assertThat(request.getPath(), is(equalTo("/api/reject-reasons")));
        assertThat(request.getMethod(), is(equalTo("GET")));

        await().until(callback.payload(), is(notNullValue()));
        List<RejectReason> rejectReasons = callback.payload().call();
        assertThat(rejectReasons.get(0).getId(), is(equalTo("hack")));
        assertThat(rejectReasons.get(0).getDescription(), is(equalTo("I've been hacked")));
        assertThat(rejectReasons.get(1).getId(), is(equalTo("mistake")));
        assertThat(rejectReasons.get(1).getDescription(), is(equalTo("It was just a mistake")));
    }

    @Test
    public void shouldUpdateDeviceAccount() throws Exception {
        mockAPI.willReturnDeviceAccount("deviceId", "deviceIdentifier", "deviceName", "pushService", "pushToken");

        final MockCallback<DeviceAccount> callback = new MockCallback<>();

        webService.updateDeviceAccount(DEVICE_ID, DEVICE_ACCOUNT_TOKEN, DEVICE_IDENTIFIER, DEVICE_NAME, PUSH_SERVICE, PUSH_TOKEN)
                .start(callback);

        RecordedRequest request = mockAPI.takeRequest();
        assertThat(request.getPath(), is(equalTo("/api/device-accounts/" + DEVICE_ID)));
        assertThat(request.getMethod(), is(equalTo("PATCH")));
        assertThat(request.getHeader("Authorization"), is(equalTo("Bearer " + DEVICE_ACCOUNT_TOKEN)));

        Map<String, Object> push_credentials = new HashMap<>();
        push_credentials.put("service", PUSH_SERVICE);
        push_credentials.put("token", PUSH_TOKEN);

        Map<String, Object> body = bodyFromRequest(request);
        assertThat(body, hasEntry("identifier", (Object) DEVICE_IDENTIFIER));
        assertThat(body, hasEntry("name", (Object) DEVICE_NAME));
        assertThat(body, hasEntry("push_credentials", (Object) push_credentials));

        assertThat(callback, CallbackMatcher.hasPayloadOfType(DeviceAccount.class));
        DeviceAccount deviceAccount = callback.payload().call();
        assertThat(deviceAccount.getId(), is(equalTo("deviceId")));
        assertThat(deviceAccount.getIdentifier(), is(equalTo("deviceIdentifier")));
        assertThat(deviceAccount.getName(), is(equalTo("deviceName")));
        assertThat(deviceAccount.getPushCredentials().getService(), is(equalTo("pushService")));
        assertThat(deviceAccount.getPushCredentials().getToken(), is(equalTo("pushToken")));
    }

    @Test
    public void shouldDeleteDeviceAccount() throws Exception {
        mockAPI.willReturnSuccess(204);

        final MockCallback<Void> callback = new MockCallback<>();

        webService.deleteDeviceAccount(DEVICE_ID, DEVICE_ACCOUNT_TOKEN)
                .start(callback);

        RecordedRequest request = mockAPI.takeRequest();
        assertThat(request.getPath(), is(equalTo("/api/device-accounts/" + DEVICE_ID)));
        assertThat(request.getMethod(), is(equalTo("DELETE")));
        assertThat(request.getHeader("Authorization"), is(equalTo("Bearer " + DEVICE_ACCOUNT_TOKEN)));

        assertThat(callback, hasNoError());
    }

    @Test
    public void shouldAllowLogin() throws Exception {
        mockAPI.willReturnSuccess(204);

        final MockCallback<Void> callback = new MockCallback<>();

        webService.allowLogin(TX_TOKEN, "123456")
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

        webService.rejectLogin(TX_TOKEN, "123456", "hack")
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
    public void shouldParseDeviceAccountNotFoundException() throws Exception {
        mockAPI.willReturnServerError(401, "device_account_not_found", "Error", "Message");

        final MockCallback<Void> callback = new MockCallback<>();

        webService.deleteDeviceAccount(DEVICE_ID, DEVICE_ACCOUNT_TOKEN)
                .start(callback);

        assertThat(callback, CallbackMatcher.hasError());

        Throwable throwable = callback.error().call();
        assertThat(throwable, is(instanceOf(DeviceAccountNotFoundException.class)));
    }

    @Test
    public void shouldParseEnrollmentTransactionNotFoundException() throws Exception {
        mockAPI.willReturnServerError(401, "enrollment_transaction_not_found", "Error", "Message");

        final MockCallback<EnrollmentInfo> callback = new MockCallback<>();

        webService.getEnrollmentInfo(ENROLLMENT_TX_ID)
                .start(callback);

        assertThat(callback, CallbackMatcher.hasError(EnrollmentInfo.class));

        Throwable throwable = callback.error().call();
        assertThat(throwable, is(instanceOf(EnrollmentTransactionNotFoundException.class)));
    }

    @Test
    public void shouldParseInvalidOTPCodeException() throws Exception {
        mockAPI.willReturnServerError(401, "invalid_otp", "Error", "Message");

        final MockCallback<Void> callback = new MockCallback<>();

        webService.allowLogin(TX_TOKEN, "123456")
                .start(callback);

        assertThat(callback, CallbackMatcher.hasError());

        Throwable throwable = callback.error().call();
        assertThat(throwable, is(instanceOf(InvalidOTPCodeException.class)));
    }

    @Test
    public void shouldParseInvalidTokenException() throws Exception {
        mockAPI.willReturnServerError(401, "invalid_token", "Error", "Message");

        final MockCallback<Void> callback = new MockCallback<>();

        webService.rejectLogin(TX_TOKEN, "123456", "hack")
                .start(callback);

        assertThat(callback, CallbackMatcher.hasError());

        Throwable throwable = callback.error().call();
        assertThat(throwable, is(instanceOf(InvalidTokenException.class)));
    }

    @Test
    public void shouldParseLoginTransactionNotFoundException() throws Exception {
        mockAPI.willReturnServerError(401, "login_transaction_not_found", "Error", "Message");

        final MockCallback<Void> callback = new MockCallback<>();

        webService.rejectLogin(TX_TOKEN, "123456", "hack")
                .start(callback);

        assertThat(callback, CallbackMatcher.hasError());

        Throwable throwable = callback.error().call();
        assertThat(throwable, is(instanceOf(LoginTransactionNotFoundException.class)));
    }

    @Test
    public void shouldParseInternalServerError() throws Exception {
        mockAPI.willReturnInternalServerError();

        final MockCallback<Void> callback = new MockCallback<>();

        webService.rejectLogin(TX_TOKEN, "123456", "hack")
                .start(callback);

        assertThat(callback, CallbackMatcher.hasError());

        Throwable throwable = callback.error().call();
        assertThat(throwable, is(instanceOf(UnparseableServerErrorException.class)));
    }

    private Map<String, Object> bodyFromRequest(RecordedRequest request) throws IOException {
        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        return gson.fromJson(new InputStreamReader(request.getBody().inputStream()), type);
    }
}