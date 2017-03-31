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
import android.os.Parcel;
import android.os.Parcelable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.security.PrivateKey;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import okhttp3.HttpUrl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 23, manifest = Config.NONE)
public class ParcelableNotificationTest {

    private static final String HOSTNAME = "example.com";
    private static final String HOSTNAME_HTTP = "http://example.com/";
    private static final String HOSTNAME_HTTPS = "https://example.com/";
    private static final String DEVICE_ID = "DEVICE_ID";
    private static final String TRANSACTION_TOKEN = "TRANSACTION_TOKEN";
    private static final String CHALLENGE = "CHALLENGE";
    private static final String BROWSER_NAME = "BROWSER_NAME";
    private static final String BROWSER_VERSION = "BROWSER_VERSION";
    private static final String OS_NAME = "OS_NAME";
    private static final String OS_VERSION = "OS_VERSION";
    private static final String LOCATION = "LOCATION";
    private static final Double LATITUDE = 56.87;
    private static final Double LONGITUDE = 34.34;

    @Mock
    CurrentDevice currentDevice;

    @Mock
    PrivateKey privateKey;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void shouldHaveCorrectDataWithHttpHostname() throws Exception {
        Date currentDate = new Date();
        Bundle data = createPushNotificationPayload(
                HOSTNAME_HTTP, DEVICE_ID, TRANSACTION_TOKEN, currentDate, CHALLENGE);

        ParcelableNotification notification = ParcelableNotification.parse(data);

        assertThat(notification, is(notNullValue()));

        assertThat(notification.getUrl(), is(equalTo(HOSTNAME_HTTP)));
        assertThat(notification.getDate(), is(equalTo(currentDate)));
        assertThat(notification.getBrowserName(), is(equalTo(BROWSER_NAME)));
        assertThat(notification.getBrowserVersion(), is(equalTo(BROWSER_VERSION)));
        assertThat(notification.getOsName(), is(equalTo(OS_NAME)));
        assertThat(notification.getOsVersion(), is(equalTo(OS_VERSION)));
        assertThat(notification.getEnrollmentId(), is(equalTo(DEVICE_ID)));
        assertThat(notification.getTransactionToken(), is(equalTo(TRANSACTION_TOKEN)));
        assertThat(notification.getLocation(), is(equalTo(LOCATION)));
        assertThat(notification.getLatitude(), is(equalTo(LATITUDE)));
        assertThat(notification.getLongitude(), is(equalTo(LONGITUDE)));
        assertThat(notification.getChallenge(), is(equalTo(CHALLENGE)));
    }

    @Test
    public void shouldHaveCorrectDataWithHttpsHostname() throws Exception {
        Date currentDate = new Date();
        Bundle data = createPushNotificationPayload(
                HOSTNAME_HTTPS, DEVICE_ID, TRANSACTION_TOKEN, currentDate, CHALLENGE);

        ParcelableNotification notification = ParcelableNotification.parse(data);

        assertThat(notification, is(notNullValue()));

        assertThat(notification.getUrl(), is(equalTo(HOSTNAME_HTTPS)));
        assertThat(notification.getDate(), is(equalTo(currentDate)));
        assertThat(notification.getBrowserName(), is(equalTo(BROWSER_NAME)));
        assertThat(notification.getBrowserVersion(), is(equalTo(BROWSER_VERSION)));
        assertThat(notification.getOsName(), is(equalTo(OS_NAME)));
        assertThat(notification.getOsVersion(), is(equalTo(OS_VERSION)));
        assertThat(notification.getEnrollmentId(), is(equalTo(DEVICE_ID)));
        assertThat(notification.getTransactionToken(), is(equalTo(TRANSACTION_TOKEN)));
        assertThat(notification.getLocation(), is(equalTo(LOCATION)));
        assertThat(notification.getLatitude(), is(equalTo(LATITUDE)));
        assertThat(notification.getLongitude(), is(equalTo(LONGITUDE)));
        assertThat(notification.getChallenge(), is(equalTo(CHALLENGE)));
    }

    @Test
    public void shouldHaveCorrectDataAfterParse() throws Exception {
        Date currentDate = new Date();
        Bundle data = createPushNotificationPayload(
                HOSTNAME, DEVICE_ID, TRANSACTION_TOKEN, currentDate, CHALLENGE);

        ParcelableNotification notification = ParcelableNotification.parse(data);

        assertThat(notification, is(notNullValue()));

        assertThat(notification.getUrl(), is(equalTo(HOSTNAME_HTTPS)));
        assertThat(notification.getDate(), is(equalTo(currentDate)));
        assertThat(notification.getBrowserName(), is(equalTo(BROWSER_NAME)));
        assertThat(notification.getBrowserVersion(), is(equalTo(BROWSER_VERSION)));
        assertThat(notification.getOsName(), is(equalTo(OS_NAME)));
        assertThat(notification.getOsVersion(), is(equalTo(OS_VERSION)));
        assertThat(notification.getEnrollmentId(), is(equalTo(DEVICE_ID)));
        assertThat(notification.getTransactionToken(), is(equalTo(TRANSACTION_TOKEN)));
        assertThat(notification.getLocation(), is(equalTo(LOCATION)));
        assertThat(notification.getLatitude(), is(equalTo(LATITUDE)));
        assertThat(notification.getLongitude(), is(equalTo(LONGITUDE)));
        assertThat(notification.getChallenge(), is(equalTo(CHALLENGE)));
    }

    @Test
    public void shouldHaveCorrectDataAfterParcel() throws Exception {
        Date currentDate = new Date();
        Bundle data = createPushNotificationPayload(
                HOSTNAME, DEVICE_ID, TRANSACTION_TOKEN, currentDate, CHALLENGE);

        ParcelableNotification originalNotification = ParcelableNotification.parse(data);

        Parcel parcel = Parcel.obtain();
        originalNotification.writeToParcel(parcel, Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
        parcel.setDataPosition(0);
        ParcelableNotification notification = ParcelableNotification.CREATOR.createFromParcel(parcel);

        assertThat(notification, is(notNullValue()));

        assertThat(notification.getUrl(), is(equalTo(HOSTNAME_HTTPS)));
        assertThat(notification.getDate(), is(equalTo(currentDate)));
        assertThat(notification.getBrowserName(), is(equalTo(BROWSER_NAME)));
        assertThat(notification.getBrowserVersion(), is(equalTo(BROWSER_VERSION)));
        assertThat(notification.getOsName(), is(equalTo(OS_NAME)));
        assertThat(notification.getOsVersion(), is(equalTo(OS_VERSION)));
        assertThat(notification.getEnrollmentId(), is(equalTo(DEVICE_ID)));
        assertThat(notification.getTransactionToken(), is(equalTo(TRANSACTION_TOKEN)));
        assertThat(notification.getLocation(), is(equalTo(LOCATION)));
        assertThat(notification.getLatitude(), is(equalTo(LATITUDE)));
        assertThat(notification.getLongitude(), is(equalTo(LONGITUDE)));
        assertThat(notification.getChallenge(), is(equalTo(CHALLENGE)));
    }

    @Test
    public void shouldHaveCorrectDataAfterParcelWithNulls() throws Exception {
        ParcelableNotification originalNotification = new ParcelableNotification(
                HttpUrl.parse(HOSTNAME_HTTPS), DEVICE_ID, TRANSACTION_TOKEN, null,
                OS_NAME, OS_VERSION, BROWSER_NAME, BROWSER_VERSION, LOCATION, null, null, CHALLENGE);

        Parcel parcel = Parcel.obtain();
        originalNotification.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        ParcelableNotification notification = ParcelableNotification.CREATOR.createFromParcel(parcel);

        assertThat(notification, is(notNullValue()));

        assertThat(notification.getUrl(), is(equalTo(HOSTNAME_HTTPS)));
        assertThat(notification.getDate(), is(nullValue()));
        assertThat(notification.getBrowserName(), is(equalTo(BROWSER_NAME)));
        assertThat(notification.getBrowserVersion(), is(equalTo(BROWSER_VERSION)));
        assertThat(notification.getOsName(), is(equalTo(OS_NAME)));
        assertThat(notification.getOsVersion(), is(equalTo(OS_VERSION)));
        assertThat(notification.getEnrollmentId(), is(equalTo(DEVICE_ID)));
        assertThat(notification.getTransactionToken(), is(equalTo(TRANSACTION_TOKEN)));
        assertThat(notification.getLocation(), is(equalTo(LOCATION)));
        assertThat(notification.getLatitude(), is(nullValue()));
        assertThat(notification.getLongitude(), is(nullValue()));
        assertThat(notification.getChallenge(), is(CHALLENGE));
    }

    @Test
    public void shouldReturnNullSource() throws Exception {
        Bundle data = createPushNotificationPayload(
                HOSTNAME, DEVICE_ID, TRANSACTION_TOKEN, new Date(), CHALLENGE,
                null, null, null, null, LOCATION, LATITUDE, LONGITUDE);

        ParcelableNotification notification = ParcelableNotification.parse(data);

        assertThat(notification, is(notNullValue()));
        assertThat(notification.getBrowserName(), is(nullValue()));
        assertThat(notification.getBrowserVersion(), is(nullValue()));
        assertThat(notification.getOsName(), is(nullValue()));
        assertThat(notification.getOsVersion(), is(nullValue()));
    }

    @Test
    public void shouldReturnNullBrowser() throws Exception {
        Bundle data = createPushNotificationPayload(
                HOSTNAME, DEVICE_ID, TRANSACTION_TOKEN, new Date(), CHALLENGE,
                null, null, OS_NAME, OS_VERSION, LOCATION, LATITUDE, LONGITUDE);

        ParcelableNotification notification = ParcelableNotification.parse(data);

        assertThat(notification, is(notNullValue()));
        assertThat(notification.getBrowserName(), is(nullValue()));
        assertThat(notification.getBrowserVersion(), is(nullValue()));
        assertThat(notification.getOsName(), is(equalTo(OS_NAME)));
        assertThat(notification.getOsVersion(), is(equalTo(OS_VERSION)));
    }

    @Test
    public void shouldReturnNullBrowserName() throws Exception {
        Bundle data = createPushNotificationPayload(
                HOSTNAME, DEVICE_ID, TRANSACTION_TOKEN, new Date(), CHALLENGE,
                null, BROWSER_VERSION, OS_NAME, OS_VERSION, LOCATION, LATITUDE, LONGITUDE);

        ParcelableNotification notification = ParcelableNotification.parse(data);

        assertThat(notification, is(notNullValue()));
        assertThat(notification.getBrowserName(), is(nullValue()));
        assertThat(notification.getBrowserVersion(), is(equalTo(BROWSER_VERSION)));
        assertThat(notification.getOsName(), is(equalTo(OS_NAME)));
        assertThat(notification.getOsVersion(), is(equalTo(OS_VERSION)));
    }

    @Test
    public void shouldReturnNullBrowserVersion() throws Exception {
        Bundle data = createPushNotificationPayload(
                HOSTNAME, DEVICE_ID, TRANSACTION_TOKEN, new Date(), CHALLENGE,
                BROWSER_NAME, null, OS_NAME, OS_VERSION, LOCATION, LATITUDE, LONGITUDE);

        ParcelableNotification notification = ParcelableNotification.parse(data);

        assertThat(notification, is(notNullValue()));
        assertThat(notification.getBrowserVersion(), is(nullValue()));
        assertThat(notification.getBrowserName(), is(equalTo(BROWSER_NAME)));
        assertThat(notification.getOsName(), is(equalTo(OS_NAME)));
        assertThat(notification.getOsVersion(), is(equalTo(OS_VERSION)));
    }

    @Test
    public void shouldReturnNullOs() throws Exception {
        Bundle data = createPushNotificationPayload(
                HOSTNAME, DEVICE_ID, TRANSACTION_TOKEN, new Date(), CHALLENGE,
                BROWSER_NAME, BROWSER_VERSION, null, null, LOCATION, LATITUDE, LONGITUDE);

        ParcelableNotification notification = ParcelableNotification.parse(data);

        assertThat(notification, is(notNullValue()));
        assertThat(notification.getOsName(), is(nullValue()));
        assertThat(notification.getOsVersion(), is(nullValue()));
        assertThat(notification.getBrowserName(), is(equalTo(BROWSER_NAME)));
        assertThat(notification.getBrowserVersion(), is(equalTo(BROWSER_VERSION)));
    }

    @Test
    public void shouldReturnNullOsName() throws Exception {
        Bundle data = createPushNotificationPayload(
                HOSTNAME, DEVICE_ID, TRANSACTION_TOKEN, new Date(), CHALLENGE,
                BROWSER_NAME, BROWSER_VERSION, null, OS_VERSION, LOCATION, LATITUDE, LONGITUDE);

        ParcelableNotification notification = ParcelableNotification.parse(data);

        assertThat(notification, is(notNullValue()));
        assertThat(notification.getOsName(), is(nullValue()));
        assertThat(notification.getOsVersion(), is(equalTo(OS_VERSION)));
        assertThat(notification.getBrowserName(), is(equalTo(BROWSER_NAME)));
        assertThat(notification.getBrowserVersion(), is(equalTo(BROWSER_VERSION)));
    }

    @Test
    public void shouldReturnNullOsVersion() throws Exception {
        Bundle data = createPushNotificationPayload(
                HOSTNAME, DEVICE_ID, TRANSACTION_TOKEN, new Date(), CHALLENGE,
                BROWSER_NAME, BROWSER_VERSION, OS_NAME, null, LOCATION, LATITUDE, LONGITUDE);

        ParcelableNotification notification = ParcelableNotification.parse(data);

        assertThat(notification, is(notNullValue()));
        assertThat(notification.getOsVersion(), is(nullValue()));
        assertThat(notification.getOsName(), is(equalTo(OS_NAME)));
        assertThat(notification.getBrowserName(), is(equalTo(BROWSER_NAME)));
        assertThat(notification.getBrowserVersion(), is(equalTo(BROWSER_VERSION)));
    }

    @Test
    public void shouldReturnNullLocation() throws Exception {
        Bundle data = createPushNotificationPayload(
                HOSTNAME, DEVICE_ID, TRANSACTION_TOKEN, new Date(), CHALLENGE,
                BROWSER_NAME, BROWSER_VERSION, OS_NAME, OS_VERSION, null, null, null);

        ParcelableNotification notification = ParcelableNotification.parse(data);

        assertThat(notification, is(notNullValue()));
        assertThat(notification.getLocation(), is(nullValue()));
        assertThat(notification.getLatitude(), is(nullValue()));
        assertThat(notification.getLongitude(), is(nullValue()));
    }

    @Test
    public void shouldReturnNullLocationName() throws Exception {
        Bundle data = createPushNotificationPayload(
                HOSTNAME, DEVICE_ID, TRANSACTION_TOKEN, new Date(), CHALLENGE,
                BROWSER_NAME, BROWSER_VERSION, OS_NAME, OS_VERSION, null, LATITUDE, LONGITUDE);

        ParcelableNotification notification = ParcelableNotification.parse(data);

        assertThat(notification, is(notNullValue()));
        assertThat(notification.getLocation(), is(nullValue()));
        assertThat(notification.getLatitude(), is(equalTo(LATITUDE)));
        assertThat(notification.getLongitude(), is(equalTo(LONGITUDE)));
    }

    @Test
    public void shouldReturnNullLatitude() throws Exception {
        Bundle data = createPushNotificationPayload(
                HOSTNAME, DEVICE_ID, TRANSACTION_TOKEN, new Date(), CHALLENGE,
                BROWSER_NAME, BROWSER_VERSION, OS_NAME, OS_VERSION, LOCATION, null, LONGITUDE);

        ParcelableNotification notification = ParcelableNotification.parse(data);

        assertThat(notification, is(notNullValue()));
        assertThat(notification.getLatitude(), is(nullValue()));
        assertThat(notification.getLocation(), is(equalTo(LOCATION)));
        assertThat(notification.getLongitude(), is(equalTo(LONGITUDE)));
    }

    @Test
    public void shouldReturnNullLongitude() throws Exception {
        Bundle data = createPushNotificationPayload(
                HOSTNAME, DEVICE_ID, TRANSACTION_TOKEN, new Date(), CHALLENGE,
                BROWSER_NAME, BROWSER_VERSION, OS_NAME, OS_VERSION, LOCATION, LATITUDE, null);

        ParcelableNotification notification = ParcelableNotification.parse(data);

        assertThat(notification, is(notNullValue()));
        assertThat(notification.getLongitude(), is(nullValue()));
        assertThat(notification.getLocation(), is(equalTo(LOCATION)));
        assertThat(notification.getLatitude(), is(equalTo(LATITUDE)));
    }

    @Test
    public void shouldReturnNullIfThereIsNoHostname() throws Exception {
        Bundle data = createPushNotificationPayload(null, DEVICE_ID, TRANSACTION_TOKEN, new Date(), CHALLENGE);

        ParcelableNotification notification = ParcelableNotification.parse(data);

        assertThat(notification, is(nullValue()));
    }

    @Test
    public void shouldReturnNullIfThereIsNoDeviceId() throws Exception {
        Bundle data = createPushNotificationPayload(HOSTNAME, null, TRANSACTION_TOKEN, new Date(), CHALLENGE);

        ParcelableNotification notification = ParcelableNotification.parse(data);

        assertThat(notification, is(nullValue()));
    }

    @Test
    public void shouldReturnNullIfThereIsNoTransactionToken() throws Exception {
        Bundle data = createPushNotificationPayload(HOSTNAME, DEVICE_ID, null, new Date(), CHALLENGE);

        ParcelableNotification notification = ParcelableNotification.parse(data);

        assertThat(notification, is(nullValue()));
    }

    @Test
    public void shouldReturnNullIfThereIsNoDate() throws Exception {
        Bundle data = createPushNotificationPayload(HOSTNAME, DEVICE_ID, TRANSACTION_TOKEN, null, CHALLENGE);

        ParcelableNotification notification = ParcelableNotification.parse(data);

        assertThat(notification, is(nullValue()));
    }

    @Test
    public void shouldReturnNullIfDateCannotBeParsed() throws Exception {
        Bundle data = createPushNotificationPayload(HOSTNAME, DEVICE_ID, TRANSACTION_TOKEN, null, CHALLENGE);
        data.putString("d", "this date cannot be parsed");

        ParcelableNotification notification = ParcelableNotification.parse(data);

        assertThat(notification, is(nullValue()));
    }

    @Test
    public void shouldReturnNullIfThereIsNoChallenge() throws Exception {
        Bundle data = createPushNotificationPayload(HOSTNAME, DEVICE_ID, TRANSACTION_TOKEN, new Date(), null);

        ParcelableNotification notification = ParcelableNotification.parse(data);

        assertThat(notification, is(nullValue()));
    }

    private Bundle createPushNotificationPayload(String hostname,
                                                 String deviceId,
                                                 String transactionToken,
                                                 Date date,
                                                 String challenge) {
        return createPushNotificationPayload(hostname, deviceId, transactionToken, date, challenge, LATITUDE, LONGITUDE);
    }

    private Bundle createPushNotificationPayload(String hostname,
                                                 String deviceId,
                                                 String transactionToken,
                                                 Date date,
                                                 String challenge,
                                                 Double latitude,
                                                 Double longitude) {
        return createPushNotificationPayload(hostname, deviceId, transactionToken, date, challenge,
                BROWSER_NAME, BROWSER_VERSION, OS_NAME, OS_VERSION, LOCATION, latitude, longitude);
    }

    private Bundle createPushNotificationPayload(String hostname,
                                                 String deviceId,
                                                 String transactionToken,
                                                 Date date,
                                                 String challenge,
                                                 String browserName,
                                                 String browserVersion,
                                                 String osName,
                                                 String osVersion,
                                                 String location,
                                                 Double latitude,
                                                 Double longitude) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        Bundle data = new Bundle();
        data.putString("d", date != null ? simpleDateFormat.format(date) : null);
        if (browserName != null || browserVersion != null || osName != null || osVersion != null) {
            data.putString("s", "{"
                    + (browserName != null || browserVersion != null ? "\"b\":{"
                    + (browserVersion != null
                    ? ("\"v\":\"" + browserVersion + "\"" + (browserName != null ? "," : "")) : "")
                    + (browserName != null
                    ? "\"n\":\"" + browserName + "\"" : "")
                    + "}"
                    + (osName != null || osVersion != null ? "," : "") : "")
                    + (osName != null || osVersion != null ? "\"os\":{"
                    + (osVersion != null
                    ? ("\"v\":\"" + osVersion + "\"" + (osName != null ? "," : "")) : "")
                    + (osName != null
                    ? "\"n\":\"" + osName + "\"" : "")
                    + "}" : "")
                    + "}");
        }
        if (location != null || latitude != null || longitude != null) {
            data.putString("l", "{"
                    + (location != null
                    ? ("\"n\":\"" + location + "\"" + (latitude != null || longitude != null ? "," : ""))
                    : "")
                    + (latitude != null
                    ? ("\"lat\":\"" + latitude + "\"" + (longitude != null ? "," : ""))
                    : "")
                    + (longitude != null ? "\"long\":\"" + longitude + "\"" : "")
                    + "}");
        }
        data.putString("sh", hostname);
        data.putString("txtkn", transactionToken);
        data.putString("dai", deviceId);
        data.putString("c", challenge);

        return data;
    }
}