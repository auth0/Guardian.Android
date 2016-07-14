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

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.Log;

import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Notification implements GuardianNotification, Parcelable {

    private static final String TAG = Notification.class.getName();

    private static final String TRANSACTION_TOKEN_KEY = "txtkn";
    private static final String DEVICE_ACCOUNT_ID_KEY = "dai";
    private static final String DATE_KEY = "d";
    private static final String SOURCE_KEY = "s";
    private static final String HOSTNAME_KEY = "sh";
    private static final String LOCATION_KEY = "l";
    private static final String LATITUDE_KEY = "lat";
    private static final String LONGITUDE_KEY = "long";
    private static final String BROWSER_KEY = "b";
    private static final String OS_KEY = "os";
    private static final String NAME_KEY = "n";
    private static final String VERSION_KEY = "v";

    private final String enrollmentId;
    private final String transactionToken;
    private final Date date;
    private final String osName;
    private final String osVersion;
    private final String browserName;
    private final String browserVersion;
    private final String location;
    private final Double latitude;
    private final Double longitude;

    Notification(String hostname,
                 String deviceId,
                 String transactionToken,
                 Date date,
                 String osName,
                 String osVersion,
                 String browserName,
                 String browserVersion,
                 String location,
                 Double latitude,
                 Double longitude) {
        this.enrollmentId = String.format("%s/%s", hostname, deviceId);
        this.transactionToken = transactionToken;
        this.date = date;
        this.osName = osName;
        this.osVersion = osVersion;
        this.browserName = browserName;
        this.browserVersion = browserVersion;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @NonNull
    public static Notification parse(@NonNull Bundle pushNotificationPayload) {
        String transactionToken = pushNotificationPayload.getString(TRANSACTION_TOKEN_KEY);
        String deviceAccountId = pushNotificationPayload.getString(DEVICE_ACCOUNT_ID_KEY);
        String dateStr = pushNotificationPayload.getString(DATE_KEY);

        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = null;
        try {
            date = simpleDateFormat.parse(dateStr);
        } catch (ParseException e) {
            Log.e(TAG, "Error while parsing date", e);
        }

        String browserName = null;
        String browserVersion = null;
        String osName = null;
        String osVersion = null;

        // source ("s") arrives as a JSON encoded string
        String sourceJson = pushNotificationPayload.getString(SOURCE_KEY);
        try {
            JSONObject sourceData = new JSONObject(sourceJson);
            JSONObject browserData = sourceData.getJSONObject(BROWSER_KEY);
            if (browserData != null) {
                browserName = browserData.getString(NAME_KEY);
                browserVersion = browserData.getString(VERSION_KEY);
            }
            JSONObject osData = sourceData.getJSONObject(OS_KEY);
            if (osData != null) {
                osName = osData.getString(NAME_KEY);
                osVersion = osData.getString(VERSION_KEY);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while parsing source", e);
        }

        String hostname = pushNotificationPayload.getString(HOSTNAME_KEY);

        String location = null;
        Double latitude = null;
        Double longitude = null;

        // location ("l") arrives as a JSON encoded string
        if (pushNotificationPayload.containsKey(LOCATION_KEY)) {
            String locationJson = pushNotificationPayload.getString(LOCATION_KEY);
            try {
                JSONObject locationData = new JSONObject(locationJson);
                location = locationData.getString(NAME_KEY);
                latitude = locationData.getDouble(LATITUDE_KEY);
                longitude = locationData.getDouble(LONGITUDE_KEY);
            } catch (Exception e) {
                Log.e(TAG, "Error while parsing location", e);
            }
        }

        return new Notification(hostname, deviceAccountId, transactionToken, date, osName,
                osVersion, browserName, browserVersion, location, latitude, longitude);
    }

    @Override
    public String getEnrollmentId() {
        return enrollmentId;
    }

    @Override
    public String getTransactionToken() {
        return transactionToken;
    }

    @Override
    public Date getDate() {
        return date;
    }

    @Override
    public String getOsName() {
        return osName;
    }

    @Override
    public String getOsVersion() {
        return osVersion;
    }

    @Override
    public String getBrowserName() {
        return browserName;
    }

    @Override
    public String getBrowserVersion() {
        return browserVersion;
    }

    @Override
    public String getLocation() {
        return location;
    }

    @Override
    public Double getLatitude() {
        return latitude;
    }

    @Override
    public Double getLongitude() {
        return longitude;
    }

    // PARCELABLE
    protected Notification(Parcel in) {
        enrollmentId = in.readString();
        transactionToken = in.readString();
        long tmpDate = in.readLong();
        date = tmpDate != -1 ? new Date(tmpDate) : null;
        osName = in.readString();
        osVersion = in.readString();
        browserName = in.readString();
        browserVersion = in.readString();
        location = in.readString();
        latitude = in.readByte() == 0x00 ? null : in.readDouble();
        longitude = in.readByte() == 0x00 ? null : in.readDouble();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(enrollmentId);
        dest.writeString(transactionToken);
        dest.writeLong(date != null ? date.getTime() : -1L);
        dest.writeString(osName);
        dest.writeString(osVersion);
        dest.writeString(browserName);
        dest.writeString(browserVersion);
        dest.writeString(location);
        if (latitude == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeDouble(latitude);
        }
        if (longitude == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeDouble(longitude);
        }
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Notification> CREATOR = new Parcelable.Creator<Notification>() {
        @Override
        public Notification createFromParcel(Parcel in) {
            return new Notification(in);
        }

        @Override
        public Notification[] newArray(int size) {
            return new Notification[size];
        }
    };
}
