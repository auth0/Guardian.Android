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

import com.auth0.android.guardian.sdk.networking.Callback;
import com.auth0.android.guardian.sdk.networking.Request;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class DeviceTokenRequestTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    Request<Map<String, String>> networkRequest;

    @Mock
    Map<String, String> successResponse;

    @Mock
    Map<String, String> invalidResponse;

    @Mock
    GuardianException exception;

    @Mock
    Callback<String> callback;

    @Captor
    ArgumentCaptor<Callback<Map<String, String>>> networkCallbackCaptor;

    DeviceTokenRequest request;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        when(successResponse.containsKey("device_account_token"))
                .thenReturn(true);
        when(successResponse.get("device_account_token"))
                .thenReturn("theDeviceAccountToken");

        when(invalidResponse.containsKey("device_account_token"))
                .thenReturn(false);

        request = new DeviceTokenRequest(networkRequest);
    }

    @Test
    public void shouldExecuteAndReturnToken() throws Exception {
        when(networkRequest.execute())
                .thenReturn(successResponse);

        String token = request.execute();

        assertThat(token, is(equalTo("theDeviceAccountToken")));
    }

    @Test
    public void shouldFailExecuteInvalidResponse() throws Exception {
        thrown.expect(GuardianException.class);

        when(networkRequest.execute())
                .thenReturn(invalidResponse);

        request.execute();
    }

    @Test
    public void shouldFailExecuteIfRequestThrows() throws Exception {
        thrown.expect(GuardianException.class);

        when(networkRequest.execute())
                .thenThrow(exception);

        request.execute();
    }

    @Test
    public void shouldStartAndReturnToken() throws Exception {
        request.start(callback);

        verify(networkRequest).start(networkCallbackCaptor.capture());
        networkCallbackCaptor.getValue().onSuccess(successResponse);

        verify(callback).onSuccess("theDeviceAccountToken");
    }

    @Test
    public void shouldFailStartInvalidResponse() throws Exception {
        request.start(callback);

        verify(networkRequest).start(networkCallbackCaptor.capture());
        networkCallbackCaptor.getValue().onSuccess(invalidResponse);

        verify(callback).onFailure(any(GuardianException.class));
    }

    @Test
    public void shouldFailStartIfRequestThrows() throws Exception {
        request.start(callback);

        verify(networkRequest).start(networkCallbackCaptor.capture());
        networkCallbackCaptor.getValue().onFailure(exception);

        verify(callback).onFailure(exception);
    }
}