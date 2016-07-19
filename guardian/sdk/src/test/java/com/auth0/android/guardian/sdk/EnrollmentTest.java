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

import android.os.Bundle;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import okhttp3.HttpUrl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 18, manifest = Config.NONE)
public class EnrollmentTest {

    private static final HttpUrl URL_HTTPS = HttpUrl.parse("https://example.com");
    private static final HttpUrl URL_HTTPS_WITH_FINAL_DASH = HttpUrl.parse("https://example.com/");
    private static final HttpUrl URL_HTTP = HttpUrl.parse("http://example.com");
    private static final HttpUrl URL_HTTP_WITH_FINAL_DASH = HttpUrl.parse("http://example.com/");
    private static final String TENANT = "TENANT";
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

    @Test
    public void shouldHaveCorrectIdWithHttpsUrl() throws Exception {
        Enrollment enrollment = new Enrollment(URL_HTTPS, TENANT, USER, PERIOD,
                DIGITS, ALGORITHM, SECRET_BASE32, DEVICE_ID, DEVICE_LOCAL_IDENTIFIER, DEVICE_NAME,
                DEVICE_GCM_TOKEN, DEVICE_TOKEN);
        assertThat(enrollment.getId(), is(equalTo("guardian://example.com/DEVICE_ID")));
    }

    @Test
    public void shouldHaveCorrectIdWithHttpsAndFinalDashUrl() throws Exception {
        Enrollment enrollment = new Enrollment(URL_HTTPS_WITH_FINAL_DASH, TENANT, USER, PERIOD,
                DIGITS, ALGORITHM, SECRET_BASE32, DEVICE_ID, DEVICE_LOCAL_IDENTIFIER, DEVICE_NAME,
                DEVICE_GCM_TOKEN, DEVICE_TOKEN);
        assertThat(enrollment.getId(), is(equalTo("guardian://example.com/DEVICE_ID")));
    }

    @Test
    public void shouldHaveCorrectIdWithHttpUrl() throws Exception {
        Enrollment enrollment = new Enrollment(URL_HTTP, TENANT, USER, PERIOD,
                DIGITS, ALGORITHM, SECRET_BASE32, DEVICE_ID, DEVICE_LOCAL_IDENTIFIER, DEVICE_NAME,
                DEVICE_GCM_TOKEN, DEVICE_TOKEN);
        assertThat(enrollment.getId(), is(equalTo("guardian://example.com/DEVICE_ID")));
    }

    @Test
    public void shouldHaveCorrectIdWithHttpAndFinalDashUrl() throws Exception {
        Enrollment enrollment = new Enrollment(URL_HTTP_WITH_FINAL_DASH, TENANT, USER, PERIOD,
                DIGITS, ALGORITHM, SECRET_BASE32, DEVICE_ID, DEVICE_LOCAL_IDENTIFIER, DEVICE_NAME,
                DEVICE_GCM_TOKEN, DEVICE_TOKEN);
        assertThat(enrollment.getId(), is(equalTo("guardian://example.com/DEVICE_ID")));
    }

    @Test
    public void shouldHaveCorrectData() throws Exception {
        Enrollment enrollment = new Enrollment(URL_HTTP_WITH_FINAL_DASH, TENANT, USER, PERIOD,
                DIGITS, ALGORITHM, SECRET_BASE32, DEVICE_ID, DEVICE_LOCAL_IDENTIFIER, DEVICE_NAME,
                DEVICE_GCM_TOKEN, DEVICE_TOKEN);
        assertThat(enrollment.getUrl(), is(equalTo(URL_HTTP_WITH_FINAL_DASH.toString())));
        assertThat(enrollment.getTenant(), is(equalTo(TENANT)));
        assertThat(enrollment.getUser(), is(equalTo(USER)));
        assertThat(enrollment.getPeriod(), is(equalTo(PERIOD)));
        assertThat(enrollment.getDigits(), is(equalTo(DIGITS)));
        assertThat(enrollment.getAlgorithm(), is(equalTo(ALGORITHM)));
        assertThat(enrollment.getSecret(), is(equalTo(SECRET_BASE32)));
        assertThat(enrollment.getDeviceId(), is(equalTo(DEVICE_ID)));
        assertThat(enrollment.getDeviceLocalIdentifier(), is(equalTo(DEVICE_LOCAL_IDENTIFIER)));
        assertThat(enrollment.getDeviceName(), is(equalTo(DEVICE_NAME)));
        assertThat(enrollment.getGCMToken(), is(equalTo(DEVICE_GCM_TOKEN)));
        assertThat(enrollment.getDeviceToken(), is(equalTo(DEVICE_TOKEN)));
    }

    @Test
    public void shouldHaveCorrectDataAfterParcel() throws Exception {
        Enrollment originalEnrollment = new Enrollment(URL_HTTP_WITH_FINAL_DASH, TENANT, USER, PERIOD,
                DIGITS, ALGORITHM, SECRET_BASE32, DEVICE_ID, DEVICE_LOCAL_IDENTIFIER, DEVICE_NAME,
                DEVICE_GCM_TOKEN, DEVICE_TOKEN);

        Bundle bundle = new Bundle();
        bundle.putParcelable("ENROLLMENT", originalEnrollment);
        Enrollment enrollment = bundle.getParcelable("ENROLLMENT");

        assertThat(enrollment, is(notNullValue()));

        assertThat(enrollment.getUrl(), is(equalTo(URL_HTTP_WITH_FINAL_DASH.toString())));
        assertThat(enrollment.getTenant(), is(equalTo(TENANT)));
        assertThat(enrollment.getUser(), is(equalTo(USER)));
        assertThat(enrollment.getPeriod(), is(equalTo(PERIOD)));
        assertThat(enrollment.getDigits(), is(equalTo(DIGITS)));
        assertThat(enrollment.getAlgorithm(), is(equalTo(ALGORITHM)));
        assertThat(enrollment.getSecret(), is(equalTo(SECRET_BASE32)));
        assertThat(enrollment.getDeviceId(), is(equalTo(DEVICE_ID)));
        assertThat(enrollment.getDeviceLocalIdentifier(), is(equalTo(DEVICE_LOCAL_IDENTIFIER)));
        assertThat(enrollment.getDeviceName(), is(equalTo(DEVICE_NAME)));
        assertThat(enrollment.getGCMToken(), is(equalTo(DEVICE_GCM_TOKEN)));
        assertThat(enrollment.getDeviceToken(), is(equalTo(DEVICE_TOKEN)));
    }
}