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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.security.PrivateKey;
import java.security.PublicKey;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.MockitoAnnotations.initMocks;

public class GuardianEnrollmentTest {

    private static final String USER = "USER";
    private static final int PERIOD = 30;
    private static final int DIGITS = 6;
    private static final String ALGORITHM = "ALGORITHM";
    private static final String SECRET_BASE32 = "SECRET_BASE32";
    private static final String DEVICE_ID = "DEVICE_ID";
    private static final String DEVICE_LOCAL_IDENTIFIER = "DEVICE_LOCAL_IDENTIFIER";
    private static final String DEVICE_NAME = "DEVICE_NAME";
    private static final String DEVICE_GCM_TOKEN = "DEVICE_GCM_TOKEN";
    private static final String DEVICE_TOKEN = "DEVICE_TOKEN";

    @Mock
    PrivateKey privateKey;

    @Mock
    PublicKey publicKey;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void shouldHaveCorrectData() throws Exception {
        CurrentDevice device = new CurrentDevice(DEVICE_GCM_TOKEN, DEVICE_NAME, DEVICE_LOCAL_IDENTIFIER);

        Enrollment enrollment = new GuardianEnrollment(USER, PERIOD, DIGITS, ALGORITHM,
                SECRET_BASE32, DEVICE_ID, device, DEVICE_TOKEN, privateKey, publicKey);

        assertThat(enrollment.getUserId(), is(equalTo(USER)));
        assertThat(enrollment.getPeriod(), is(equalTo(PERIOD)));
        assertThat(enrollment.getDigits(), is(equalTo(DIGITS)));
        assertThat(enrollment.getAlgorithm(), is(equalTo(ALGORITHM)));
        assertThat(enrollment.getSecret(), is(equalTo(SECRET_BASE32)));
        assertThat(enrollment.getId(), is(equalTo(DEVICE_ID)));
        assertThat(enrollment.getDeviceIdentifier(), is(equalTo(DEVICE_LOCAL_IDENTIFIER)));
        assertThat(enrollment.getDeviceName(), is(equalTo(DEVICE_NAME)));
        assertThat(enrollment.getNotificationToken(), is(equalTo(DEVICE_GCM_TOKEN)));
        assertThat(enrollment.getDeviceToken(), is(equalTo(DEVICE_TOKEN)));
        assertThat(enrollment.getSigningKey(), is(sameInstance(privateKey)));
        assertThat(enrollment.getPublicKey(), is(sameInstance(publicKey)));
    }
}