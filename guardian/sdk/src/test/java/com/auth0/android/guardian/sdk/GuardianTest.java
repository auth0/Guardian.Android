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
import android.os.Bundle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 18, manifest = Config.NONE)
public class GuardianTest {

    private static final String TENANT = "TENANT";
    private static final String USER = "USER";
    private static final int PERIOD = 30;
    private static final int DIGITS = 6;
    private static final String ALGORITHM = "SHA1";
    private static final String SECRET_BASE32 = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";
    private static final String DEVICE_ID = "DEVICE_ID";
    private static final String DEVICE_NAME = "DEVICE_NAME";
    private static final String DEVICE_IDENTIFIER = "DEVICE_IDENTIFIER";
    private static final String GCM_TOKEN = "GCM_TOKEN";
    private static final String DEVICE_TOKEN = "DEVICE_TOKEN";
    private static final String GUARDIAN_URL = "http://example.guardian.auth0.com/";
    private static final String ENROLLMENT_TX_ID = "ENROLLMENT_TX_ID";
    private static final String TRANSACTION_TOKEN = "TRANSACTION_TOKEN";

    @Mock
    Device device;

    @Mock
    GuardianAPIRequest<String> stringRequest;

    @Mock
    GuardianAPIRequest<Device> deviceRequest;

    @Mock
    DeviceAPIClient deviceApiClient;

    @Mock
    GuardianAPIClient apiClient;

    @Mock
    Notification notification;

    Enrollment enrollment;
    Guardian guardian;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        when(notification.getTransactionToken())
                .thenReturn(TRANSACTION_TOKEN);

        enrollment = new ParcelableEnrollment(
                GUARDIAN_URL, TENANT, USER, PERIOD, DIGITS, ALGORITHM, SECRET_BASE32, DEVICE_ID,
                DEVICE_IDENTIFIER, DEVICE_NAME, GCM_TOKEN, DEVICE_TOKEN);

        when(apiClient.device(DEVICE_ID, DEVICE_TOKEN))
                .thenReturn(deviceApiClient);

        guardian = new Guardian(apiClient);
    }

    @Test
    public void shouldReturnEnrollRequest() throws Exception {
        Uri enrollmentUri = createEnrollmentUri();
        GuardianAPIRequest<ParcelableEnrollment> request = guardian
                .enroll(enrollmentUri, DEVICE_NAME, GCM_TOKEN);

        assertThat(request, is(instanceOf(EnrollRequest.class)));
        EnrollRequest enrollRequest = (EnrollRequest) request;

        assertThat(enrollRequest.client, is(sameInstance(apiClient)));
        assertThat(enrollRequest.deviceName, is(equalTo(DEVICE_NAME)));
        assertThat(enrollRequest.gcmToken, is(equalTo(GCM_TOKEN)));

        EnrollmentData enrollmentData = EnrollmentData.parse(enrollmentUri);
        EnrollmentData data = enrollRequest.enrollmentData;

        assertThat(data.getUser(), is(equalTo(enrollmentData.getUser())));
        assertThat(data.getSecret(), is(equalTo(enrollmentData.getSecret())));
        assertThat(data.getIssuer(), is(equalTo(enrollmentData.getIssuer())));
        assertThat(data.getEnrollmentTransactionId(), is(equalTo(enrollmentData.getEnrollmentTransactionId())));
        assertThat(data.getDeviceId(), is(equalTo(enrollmentData.getDeviceId())));
        assertThat(data.getAlgorithm(), is(equalTo(enrollmentData.getAlgorithm())));
        assertThat(data.getDigits(), is(equalTo(enrollmentData.getDigits())));
        assertThat(data.getPeriod(), is(equalTo(enrollmentData.getPeriod())));
        assertThat(data.getUrl(), is(equalTo(enrollmentData.getUrl())));
    }

    @Test
    public void shouldCallDelete() throws Exception {
        guardian.delete(enrollment);

        verify(apiClient).device(DEVICE_ID, DEVICE_TOKEN);
        verify(deviceApiClient).delete();
    }

    @Test
    public void shouldCallAllow() throws Exception {
        guardian.allow(notification, enrollment);

        String otpCode = guardian.getOTPCode(enrollment);
        verify(apiClient).allow(TRANSACTION_TOKEN, otpCode);
    }

    @Test
    public void shouldCallReject() throws Exception {
        guardian.reject(notification, enrollment);

        String otpCode = guardian.getOTPCode(enrollment);
        verify(apiClient).reject(TRANSACTION_TOKEN, otpCode, null);
    }

    @Test
    public void shouldCallRejectWithReason() throws Exception {
        guardian.reject(notification, enrollment, "reason");

        String otpCode = guardian.getOTPCode(enrollment);
        verify(apiClient).reject(TRANSACTION_TOKEN, otpCode, "reason");
    }

    @Test
    public void testBuilderWithUrl() throws Exception {
        Guardian guardian = new Guardian.Builder()
                .url(Uri.parse("https://example.guardian.auth0.com"))
                .build();

        assertThat(guardian.getAPIClient().getUrl(),
                is(equalTo("https://example.guardian.auth0.com/")));
    }

    @Test
    public void testBuilderWithDomain() throws Exception {
        Guardian guardian = new Guardian.Builder()
                .domain("example.guardian.auth0.com")
                .build();

        assertThat(guardian.getAPIClient().getUrl(),
                is(equalTo("https://example.guardian.auth0.com/")));
    }

    @Test
    public void testParcelable() throws Exception {
        Guardian originalGuardian = new Guardian.Builder()
                .domain("example.guardian.auth0.com")
                .build();

        Bundle bundle = new Bundle();
        bundle.putParcelable("GUARDIAN", originalGuardian);

        Guardian guardian = bundle.getParcelable("GUARDIAN");

        assertThat(guardian, is(notNullValue()));
        assertThat(guardian.getAPIClient().getUrl(),
                is(equalTo("https://example.guardian.auth0.com/")));
    }

    private Uri createEnrollmentUri() {
        return Uri.parse(
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
    }
}