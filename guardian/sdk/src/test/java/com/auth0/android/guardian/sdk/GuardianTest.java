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
import android.os.Parcel;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 23, manifest = Config.NONE)
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
    private static final String RECOVERY_CODE = "RECOVERY_CODE";
    private static final String ENROLLMENT_TX_ID = "ENROLLMENT_TX_ID";
    private static final String TRANSACTION_TOKEN = "TRANSACTION_TOKEN";
    private static final String CHALLENGE = "CHALLENGE";

    @Mock
    GuardianAPIRequest<Map<String, Object>> mockEnrollRequest;

    @Mock
    DeviceAPIClient deviceApiClient;

    @Mock
    GuardianAPIClient apiClient;

    @Mock
    Notification notification;

    @Mock
    PrivateKey privateKey;

    @Mock
    PublicKey publicKey;

    Enrollment enrollment;
    Guardian guardian;
    KeyPair keyPair;
    CurrentDevice currentDevice;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        when(notification.getTransactionToken())
                .thenReturn(TRANSACTION_TOKEN);

        keyPair = new KeyPair(publicKey, privateKey);

        currentDevice = new CurrentDevice(GCM_TOKEN, DEVICE_NAME, DEVICE_IDENTIFIER);

        enrollment = new GuardianEnrollment(GUARDIAN_URL, TENANT, USER, PERIOD,
                DIGITS, ALGORITHM, SECRET_BASE32, DEVICE_ID, currentDevice, DEVICE_TOKEN,
                RECOVERY_CODE, privateKey);

        when(apiClient.device(DEVICE_ID, DEVICE_TOKEN))
                .thenReturn(deviceApiClient);

        guardian = new Guardian(apiClient);
    }

    @Test
    public void shouldReturnEnrollRequestUsingFullEnrollmentUri() throws Exception {
        String enrollmentUri = createEnrollmentUri();

        when(apiClient.enroll(eq(ENROLLMENT_TX_ID), eq(DEVICE_IDENTIFIER), eq(DEVICE_NAME), eq(GCM_TOKEN), eq(publicKey)))
                .thenReturn(mockEnrollRequest);

        GuardianAPIRequest<Enrollment> request = guardian
                .enroll(enrollmentUri, currentDevice, keyPair);

        assertThat(request, is(instanceOf(EnrollRequest.class)));
        EnrollRequest enrollRequest = (EnrollRequest) request;

        assertThat(enrollRequest.request, is(sameInstance(mockEnrollRequest)));
        assertThat(enrollRequest.device, is(sameInstance(currentDevice)));
        assertThat(enrollRequest.deviceKeyPair, is(sameInstance(keyPair)));
    }

    @Test
    public void shouldReturnEnrollRequestUsingOnlyEnrollmentTicket() throws Exception {
        when(apiClient.enroll(eq(ENROLLMENT_TX_ID), eq(DEVICE_IDENTIFIER), eq(DEVICE_NAME), eq(GCM_TOKEN), eq(publicKey)))
                .thenReturn(mockEnrollRequest);

        GuardianAPIRequest<Enrollment> request = guardian
                .enroll(ENROLLMENT_TX_ID, currentDevice, keyPair);

        assertThat(request, is(instanceOf(EnrollRequest.class)));
        EnrollRequest enrollRequest = (EnrollRequest) request;

        assertThat(enrollRequest.request, is(sameInstance(mockEnrollRequest)));
        assertThat(enrollRequest.device, is(sameInstance(currentDevice)));
        assertThat(enrollRequest.deviceKeyPair, is(sameInstance(keyPair)));
    }

    @Test
    public void shouldCallDelete() throws Exception {
        @SuppressWarnings("unchecked")
        GuardianAPIRequest<Void> mockRequest = mock(GuardianAPIRequest.class);
        when(deviceApiClient.delete())
                .thenReturn(mockRequest);

        GuardianAPIRequest<Void> request = guardian.delete(enrollment);

        verify(apiClient).device(DEVICE_ID, DEVICE_TOKEN);
        verify(deviceApiClient).delete();

        assertThat(request, is(sameInstance(mockRequest)));
    }

    @Test
    public void shouldCallAllowWithPrivateKey() throws Exception {
        when(notification.getChallenge())
                .thenReturn(CHALLENGE);

        @SuppressWarnings("unchecked")
        GuardianAPIRequest<Void> mockRequest = mock(GuardianAPIRequest.class);
        when(apiClient.allow(TRANSACTION_TOKEN, DEVICE_IDENTIFIER, CHALLENGE, privateKey))
                .thenReturn(mockRequest);

        GuardianAPIRequest<Void> request = guardian.allow(notification, enrollment);

        verify(apiClient)
                .allow(TRANSACTION_TOKEN, DEVICE_IDENTIFIER, CHALLENGE, privateKey);

        assertThat(request, is(sameInstance(mockRequest)));
    }

    @Test
    public void shouldCallRejectWithPrivateKey() throws Exception {
        when(notification.getChallenge())
                .thenReturn(CHALLENGE);

        @SuppressWarnings("unchecked")
        GuardianAPIRequest<Void> mockRequest = mock(GuardianAPIRequest.class);
        when(apiClient.reject(TRANSACTION_TOKEN, DEVICE_IDENTIFIER, CHALLENGE, privateKey, null))
                .thenReturn(mockRequest);

        GuardianAPIRequest<Void> request = guardian.reject(notification, enrollment);

        verify(apiClient)
                .reject(TRANSACTION_TOKEN, DEVICE_IDENTIFIER, CHALLENGE, privateKey,  null);

        assertThat(request, is(sameInstance(mockRequest)));
    }

    @Test
    public void shouldCallRejectWithReasonAndPrivateKey() throws Exception {
        when(notification.getChallenge())
                .thenReturn(CHALLENGE);

        @SuppressWarnings("unchecked")
        GuardianAPIRequest<Void> mockRequest = mock(GuardianAPIRequest.class);
        when(apiClient.reject(TRANSACTION_TOKEN, DEVICE_IDENTIFIER, CHALLENGE, privateKey, "reason"))
                .thenReturn(mockRequest);

        GuardianAPIRequest<Void> request = guardian.reject(notification, enrollment, "reason");

        verify(apiClient)
                .reject(TRANSACTION_TOKEN, DEVICE_IDENTIFIER, CHALLENGE, privateKey, "reason");

        assertThat(request, is(sameInstance(mockRequest)));
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

        Parcel parcel = Parcel.obtain();
        originalGuardian.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        Guardian guardian = Guardian.CREATOR.createFromParcel(parcel);

        assertThat(guardian, is(notNullValue()));
        assertThat(guardian.getAPIClient().getUrl(),
                is(equalTo("https://example.guardian.auth0.com/")));
    }

    private String createEnrollmentUri() {
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
        ).toString();
    }
}