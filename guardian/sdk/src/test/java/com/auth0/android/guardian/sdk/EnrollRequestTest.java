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

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 23, manifest = Config.NONE)
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
    private static final String RECOVERY_CODE = "RECOVERY_CODE";
    private static final String GCM_TOKEN = "GCM_TOKEN";
    private static final String DEVICE_TOKEN = "DEVICE_TOKEN";
    private static final String GUARDIAN_URL = "http://example.guardian.auth0.com/";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    GuardianAPIRequest<Map<String, Object>> request;

    @Mock
    Callback<Enrollment> callback;

    @Mock
    PrivateKey privateKey;

    @Mock
    RSAPublicKey publicKey;

    @Captor
    ArgumentCaptor<Callback<Map<String, Object>>> callbackCaptor;

    @Captor
    ArgumentCaptor<Enrollment> enrollmentCaptor;

    EnrollRequest enrollRequest;
    KeyPair keyPair;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        keyPair = new KeyPair(publicKey, privateKey);

        enrollRequest = new EnrollRequest(request, DEVICE_IDENTIFIER, DEVICE_NAME, GCM_TOKEN, keyPair);
    }

    @Test
    public void shouldEnrollSucessfullySync() throws Exception {
        Map<String, Object> enrollmentResponse = createEnrollmentResponse(DEVICE_ID, GUARDIAN_URL,
                TENANT, USER, DEVICE_TOKEN, RECOVERY_CODE, SECRET_BASE32, ALGORITHM, PERIOD, DIGITS);
        when(request.execute())
                .thenReturn(enrollmentResponse);

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
        assertThat(enrollment.getRecoveryCode(), is(equalTo(RECOVERY_CODE)));
        assertThat(enrollment.getSigningKey(), is(sameInstance(privateKey)));
    }

    @Test
    public void shouldEnrollSuccessfullyAsync() throws Exception {
        Map<String, Object> enrollmentResponse = createEnrollmentResponse(DEVICE_ID, GUARDIAN_URL,
                TENANT, USER, DEVICE_TOKEN, RECOVERY_CODE, SECRET_BASE32, ALGORITHM, PERIOD, DIGITS);

        enrollRequest
                .start(callback);

        verify(request).start(callbackCaptor.capture());
        callbackCaptor.getValue()
                .onSuccess(enrollmentResponse);

        verify(callback).onSuccess(enrollmentCaptor.capture());

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
        assertThat(enrollment.getRecoveryCode(), is(equalTo(RECOVERY_CODE)));
        assertThat(enrollment.getSigningKey(), is(sameInstance(privateKey)));
    }

    @Test
    public void shouldEnrollSucessfullySyncWithNullRecovey() throws Exception {
        Map<String, Object> enrollmentResponse = createEnrollmentResponse(DEVICE_ID, GUARDIAN_URL,
                TENANT, USER, DEVICE_TOKEN, null, SECRET_BASE32, ALGORITHM, PERIOD, DIGITS);
        when(request.execute())
                .thenReturn(enrollmentResponse);

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
        assertThat(enrollment.getRecoveryCode(), is(nullValue()));
        assertThat(enrollment.getSigningKey(), is(sameInstance(privateKey)));
    }

    @Test
    public void shouldEnrollSucessfullySyncWithNullTOTP() throws Exception {
        Map<String, Object> enrollmentResponse = createEnrollmentResponse(DEVICE_ID, GUARDIAN_URL,
                TENANT, USER, DEVICE_TOKEN, RECOVERY_CODE, null, null, null, null);
        when(request.execute())
                .thenReturn(enrollmentResponse);

        Enrollment enrollment = enrollRequest
                .execute();

        assertThat(enrollment.getUrl(), is(equalTo(GUARDIAN_URL)));
        assertThat(enrollment.getLabel(), is(equalTo(TENANT)));
        assertThat(enrollment.getUser(), is(equalTo(USER)));
        assertThat(enrollment.getPeriod(), is(nullValue()));
        assertThat(enrollment.getDigits(), is(nullValue()));
        assertThat(enrollment.getAlgorithm(), is(nullValue()));
        assertThat(enrollment.getSecret(), is(nullValue()));
        assertThat(enrollment.getId(), is(equalTo(DEVICE_ID)));
        assertThat(enrollment.getDeviceIdentifier(), is(equalTo(DEVICE_IDENTIFIER)));
        assertThat(enrollment.getDeviceName(), is(equalTo(DEVICE_NAME)));
        assertThat(enrollment.getGCMToken(), is(equalTo(GCM_TOKEN)));
        assertThat(enrollment.getDeviceToken(), is(equalTo(DEVICE_TOKEN)));
        assertThat(enrollment.getRecoveryCode(), is(RECOVERY_CODE));
        assertThat(enrollment.getSigningKey(), is(sameInstance(privateKey)));
    }

    @Test
    public void shouldFailEnrollWhenRequestFailsSync() throws Exception {
        thrown.expect(Throwable.class);

        when(request.execute())
                .thenThrow(new RuntimeException("Error"));

        enrollRequest
                .execute();
    }

    @Test
    public void shouldFailEnrollWhenRequestFailsAsync() throws Exception {
        enrollRequest
                .start(callback);

        verify(request).start(callbackCaptor.capture());
        callbackCaptor.getValue()
                .onFailure(new RuntimeException("Error"));

        verify(callback).onFailure(any(Throwable.class));
    }

    private Map<String, Object> createEnrollmentResponse(String id, String url, String issuer,
                                                         String user, String token,
                                                         String recoveryCode,
                                                         String totpSecret, String totpAlgorithm,
                                                         Integer totpPeriod, Integer totpDigits) {
        Map<String, Object> enrollmentResponse = new HashMap<>();
        enrollmentResponse.put("id", id);
        enrollmentResponse.put("url", url);
        enrollmentResponse.put("issuer", issuer);
        enrollmentResponse.put("user", user);
        enrollmentResponse.put("token", token);
        if (recoveryCode != null) {
            enrollmentResponse.put("recovery_code", recoveryCode);
        }
        if (totpSecret != null || totpAlgorithm != null || totpPeriod != null || totpDigits != null) {
            Map<String, Object> totp = new HashMap<>();
            totp.put("secret", totpSecret);
            totp.put("algorithm", totpAlgorithm);
            totp.put("digits", totpDigits.doubleValue());
            totp.put("period", totpPeriod.doubleValue());
            enrollmentResponse.put("totp", totp);
        }
        return enrollmentResponse;
    }
}