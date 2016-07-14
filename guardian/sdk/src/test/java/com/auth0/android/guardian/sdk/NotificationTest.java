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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 18, manifest = Config.NONE)
public class NotificationTest {

    private static final String HOSTNAME = "example.com";
    private static final String HOSTNAME_HTTPS = "https://example.com";
    private static final String HOSTNAME_HTTPS_WITH_FINAL_DASH = "https://example.com/";
    private static final String HOSTNAME_HTTP = "http://example.com";
    private static final String HOSTNAME_HTTP_WITH_FINAL_DASH = "http://example.com/";
    private static final String DEVICE_ID = "DEVICE_ID";
    private static final String TRANSACTION_TOKEN = "TRANSACTION_TOKEN";
    private static final String BROWSER_NAME = "BROWSER_NAME";
    private static final String BROWSER_VERSION = "BROWSER_VERSION";
    private static final String OS_NAME = "OS_NAME";
    private static final String OS_VERSION = "OS_VERSION";
    private static final String LOCATION = "LOCATION";
    private static final Double LATITUDE = 56.87;
    private static final Double LONGITUDE = 34.34;

    @Test
    public void shouldHaveCorrectDataAfterParse() throws Exception {
        Date currentDate = new Date();
        Bundle data = createPushNotificationPayload(HOSTNAME, currentDate);

        Notification notification = Notification.parse(data);

        assertThat(notification.getDate(), is(equalTo(currentDate)));
        assertThat(notification.getBrowserName(), is(equalTo(BROWSER_NAME)));
        assertThat(notification.getBrowserVersion(), is(equalTo(BROWSER_VERSION)));
        assertThat(notification.getOsName(), is(equalTo(OS_NAME)));
        assertThat(notification.getOsVersion(), is(equalTo(OS_VERSION)));
        assertThat(notification.getEnrollmentId(), is(equalTo("guardian://example.com/DEVICE_ID")));
        assertThat(notification.getTransactionToken(), is(equalTo(TRANSACTION_TOKEN)));
        assertThat(notification.getLocation(), is(equalTo(LOCATION)));
        assertThat(notification.getLatitude(), is(equalTo(LATITUDE)));
        assertThat(notification.getLongitude(), is(equalTo(LONGITUDE)));
    }

    @Test
    public void shouldHaveCorrectEnrollmentIdWithHostname() throws Exception {
        Bundle data = createPushNotificationPayload(HOSTNAME, new Date());

        Notification notification = Notification.parse(data);

        assertThat(notification.getEnrollmentId(), is(equalTo("guardian://example.com/DEVICE_ID")));
    }

    @Test
    public void shouldHaveCorrectEnrollmentIdWithHttpsUrl() throws Exception {
        Bundle data = createPushNotificationPayload(HOSTNAME_HTTPS, new Date());

        Notification notification = Notification.parse(data);

        assertThat(notification.getEnrollmentId(), is(equalTo("guardian://example.com/DEVICE_ID")));
    }

    @Test
    public void shouldHaveCorrectEnrollmentIdWithHttpsAndFinalDashUrl() throws Exception {
        Bundle data = createPushNotificationPayload(HOSTNAME_HTTPS_WITH_FINAL_DASH, new Date());

        Notification notification = Notification.parse(data);

        assertThat(notification.getEnrollmentId(), is(equalTo("guardian://example.com/DEVICE_ID")));
    }

    @Test
    public void shouldHaveCorrectEnrollmentIdWithHttpUrl() throws Exception {
        Bundle data = createPushNotificationPayload(HOSTNAME_HTTP, new Date());

        Notification notification = Notification.parse(data);

        assertThat(notification.getEnrollmentId(), is(equalTo("guardian://example.com/DEVICE_ID")));
    }

    @Test
    public void shouldHaveCorrectEnrollmentIdWithHttpAndFinalDashUrl() throws Exception {
        Bundle data = createPushNotificationPayload(HOSTNAME_HTTP_WITH_FINAL_DASH, new Date());

        Notification notification = Notification.parse(data);

        assertThat(notification.getEnrollmentId(), is(equalTo("guardian://example.com/DEVICE_ID")));
    }

    @Test
    public void shouldHaveCorrectDataAfterParcel() throws Exception {
        Date currentDate = new Date();
        Bundle data = createPushNotificationPayload(HOSTNAME, currentDate);

        Notification originalNotification = Notification.parse(data);

        Bundle bundle = new Bundle();
        bundle.putParcelable("NOTIFICATION", originalNotification);
        Notification notification = bundle.getParcelable("NOTIFICATION");

        assertThat(notification, is(notNullValue()));

        assertThat(notification.getDate(), is(equalTo(currentDate)));
        assertThat(notification.getBrowserName(), is(equalTo(BROWSER_NAME)));
        assertThat(notification.getBrowserVersion(), is(equalTo(BROWSER_VERSION)));
        assertThat(notification.getOsName(), is(equalTo(OS_NAME)));
        assertThat(notification.getOsVersion(), is(equalTo(OS_VERSION)));
        assertThat(notification.getEnrollmentId(), is(equalTo("guardian://example.com/DEVICE_ID")));
        assertThat(notification.getTransactionToken(), is(equalTo(TRANSACTION_TOKEN)));
        assertThat(notification.getLocation(), is(equalTo(LOCATION)));
        assertThat(notification.getLatitude(), is(equalTo(LATITUDE)));
        assertThat(notification.getLongitude(), is(equalTo(LONGITUDE)));
    }

    @Test
    public void shouldMatchEnrollmentId() throws Exception {
        Date currentDate = new Date();
        Bundle data = createPushNotificationPayload(HOSTNAME_HTTPS, currentDate);

        Notification notification = Notification.parse(data);

        Enrollment enrollment = new Enrollment(
                HOSTNAME_HTTPS, null, null, 6, 30, null, null,
                DEVICE_ID, null, null, null, null);

        assertThat(notification.getEnrollmentId(), is(equalTo(enrollment.getId())));
    }

    private Bundle createPushNotificationPayload(String hostname, Date date) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        Bundle data = new Bundle();
        data.putString("d", simpleDateFormat.format(date));
        data.putString("s", "{\"b\":{\"v\":\""+BROWSER_VERSION+"\",\"n\":\""+BROWSER_NAME+"\"}," +
                "\"os\":{\"v\":\""+OS_VERSION+"\",\"n\":\""+OS_NAME+"\"}}");
        data.putString("l", "{\"n\":\""+LOCATION+"\",\"lat\":\""+LATITUDE+"\"," +
                "\"long\":\""+LONGITUDE+"\"}");
        data.putString("sh", hostname);
        data.putString("txtkn", TRANSACTION_TOKEN);
        data.putString("dai", DEVICE_ID);

        return data;
    }
}