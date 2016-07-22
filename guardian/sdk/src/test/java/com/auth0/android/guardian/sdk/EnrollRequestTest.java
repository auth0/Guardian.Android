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

import com.auth0.android.guardian.sdk.networking.Callback;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 18, manifest = Config.NONE)
public class EnrollRequestTest {

    private static final String TENANT = "TENANT";
    private static final String USER = "USER";
    private static final int PERIOD = 30;
    private static final int DIGITS = 6;
    private static final String ALGORITHM = "ALGORITHM";
    private static final String SECRET_BASE32 = "SECRET_BASE32";
    private static final String DEVICE_ID = "DEVICE_ID";
    private static final String DEVICE_NAME = "DEVICE_NAME";
    private static final String DEVICE_IDENTIFIER = "DEVICE_IDENTIFIER";
    private static final String GCM_TOKEN = "GCM_TOKEN";
    private static final String DEVICE_TOKEN = "DEVICE_TOKEN";
    private static final String GUARDIAN_URL = "http://example.guardian.auth0.com/";
    private static final String ENROLLMENT_TX_ID = "ENROLLMENT_TX_ID";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    Device device;

    @Mock
    GuardianAPIRequest<String> deviceTokenRequest;

    @Mock
    GuardianAPIRequest<Device> createDeviceRequest;

    @Mock
    DeviceAPIClient deviceApiClient;

    @Mock
    GuardianAPIClient apiClient;

    @Mock
    Callback<Enrollment> enrollmentCallback;

    @Captor
    ArgumentCaptor<Callback<String>> deviceTokenCallbackCaptor;

    @Captor
    ArgumentCaptor<Callback<Device>> deviceCallbackCaptor;

    @Captor
    ArgumentCaptor<Enrollment> enrollmentCaptor;

    @Captor
    ArgumentCaptor<Throwable> throwableCaptor;

    EnrollRequest enrollRequest;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        when(device.getEnrollmentId())
                .thenReturn(DEVICE_ID);
        when(device.getDeviceName())
                .thenReturn(DEVICE_NAME);
        when(device.getGCMToken())
                .thenReturn(GCM_TOKEN);
        when(device.getDeviceIdentifier())
                .thenReturn(DEVICE_IDENTIFIER);

        when(apiClient.getUrl())
                .thenReturn(GUARDIAN_URL);

        when(deviceTokenRequest.execute())
                .thenReturn(DEVICE_TOKEN);
        when(apiClient.getDeviceToken(ENROLLMENT_TX_ID))
                .thenReturn(deviceTokenRequest);

        when(createDeviceRequest.execute())
                .thenReturn(device);
        when(deviceApiClient.create(DEVICE_NAME, GCM_TOKEN))
                .thenReturn(createDeviceRequest);
        when(apiClient.device(DEVICE_ID, DEVICE_TOKEN))
                .thenReturn(deviceApiClient);

        Uri enrollmentUri = Uri.parse(
                "otpauth://totp/" +
                        TENANT + ":" + USER +
                        "?secret=" + SECRET_BASE32 +
                        "&issuer=" + TENANT +
                        "&enrollment_tx_id=" + ENROLLMENT_TX_ID +
                        "&id=" + DEVICE_ID +
                        "&algorithm=" + ALGORITHM +
                        "&digits=" + DIGITS +
                        "&period=" + PERIOD +
                        "&base_url=" + GUARDIAN_URL
        );

        EnrollmentData enrollmentData = EnrollmentData.parse(enrollmentUri);

        enrollRequest = new EnrollRequest(apiClient, enrollmentData, DEVICE_NAME, GCM_TOKEN);
    }

    @Test
    public void shouldEnrollSucessfullySync() throws Exception {
        Enrollment enrollment = enrollRequest
                .execute();

        assertThat(enrollment.getUrl(), is(equalTo(GUARDIAN_URL)));
        assertThat(enrollment.getLabel(), is(equalTo(TENANT)));
        assertThat(enrollment.getUser(), is(equalTo(USER)));
        assertThat(enrollment.getPeriod(), is(equalTo(PERIOD)));
        assertThat(enrollment.getDigits(), is(equalTo(DIGITS)));
        assertThat(enrollment.getAlgorithm(), is(equalTo(ALGORITHM)));
        assertThat(enrollment.getSecret(), is(equalTo(SECRET_BASE32)));
        assertThat(enrollment.getId(), is(equalTo(DEVICE_ID)));
        assertThat(enrollment.getDeviceIdentifier(), is(equalTo(DEVICE_IDENTIFIER)));
        assertThat(enrollment.getDeviceName(), is(equalTo(DEVICE_NAME)));
        assertThat(enrollment.getGCMToken(), is(equalTo(GCM_TOKEN)));
        assertThat(enrollment.getDeviceToken(), is(equalTo(DEVICE_TOKEN)));
    }

    @Test
    public void shouldEnrollSuccessfullyAsync() throws Exception {
        enrollRequest
                .start(enrollmentCallback);

        verify(deviceTokenRequest).start(deviceTokenCallbackCaptor.capture());
        deviceTokenCallbackCaptor.getValue()
                .onSuccess(DEVICE_TOKEN);

        verify(createDeviceRequest).start(deviceCallbackCaptor.capture());
        deviceCallbackCaptor.getValue()
                .onSuccess(device);

        verify(enrollmentCallback).onSuccess(enrollmentCaptor.capture());

        Enrollment enrollment = enrollmentCaptor.getValue();

        assertThat(enrollment.getUrl(), is(equalTo(GUARDIAN_URL)));
        assertThat(enrollment.getLabel(), is(equalTo(TENANT)));
        assertThat(enrollment.getUser(), is(equalTo(USER)));
        assertThat(enrollment.getPeriod(), is(equalTo(PERIOD)));
        assertThat(enrollment.getDigits(), is(equalTo(DIGITS)));
        assertThat(enrollment.getAlgorithm(), is(equalTo(ALGORITHM)));
        assertThat(enrollment.getSecret(), is(equalTo(SECRET_BASE32)));
        assertThat(enrollment.getId(), is(equalTo(DEVICE_ID)));
        assertThat(enrollment.getDeviceIdentifier(), is(equalTo(DEVICE_IDENTIFIER)));
        assertThat(enrollment.getDeviceName(), is(equalTo(DEVICE_NAME)));
        assertThat(enrollment.getGCMToken(), is(equalTo(GCM_TOKEN)));
        assertThat(enrollment.getDeviceToken(), is(equalTo(DEVICE_TOKEN)));
    }

    @Test
    public void shouldFailEnrollWhenDeviceTokenFailsSync() throws Exception {
        thrown.expect(Throwable.class);

        when(deviceTokenRequest.execute())
                .thenThrow(new RuntimeException("Error"));

        enrollRequest
                .execute();
    }

    @Test
    public void shouldFailEnrollUpdateDeviceFailsSync() throws Exception {
        thrown.expect(Throwable.class);

        when(createDeviceRequest.execute())
                .thenThrow(new RuntimeException("Error"));

        enrollRequest
                .execute();
    }

    @Test
    public void shouldFailEnrollWhenDeviceTokenFailsAsync() throws Exception {
        enrollRequest
                .start(enrollmentCallback);

        verify(deviceTokenRequest).start(deviceTokenCallbackCaptor.capture());
        deviceTokenCallbackCaptor.getValue()
                .onFailure(new RuntimeException("Error"));

        verify(enrollmentCallback).onFailure(any(Throwable.class));
    }

    @Test
    public void shouldFailEnrollWhenUpdateDeviceFailsAsync() throws Exception {
        enrollRequest
                .start(enrollmentCallback);

        verify(deviceTokenRequest).start(deviceTokenCallbackCaptor.capture());
        deviceTokenCallbackCaptor.getValue()
                .onSuccess(DEVICE_TOKEN);

        verify(createDeviceRequest).start(deviceCallbackCaptor.capture());
        deviceCallbackCaptor.getValue()
                .onFailure(new RuntimeException("Error"));

        verify(enrollmentCallback).onFailure(any(Throwable.class));
    }
}