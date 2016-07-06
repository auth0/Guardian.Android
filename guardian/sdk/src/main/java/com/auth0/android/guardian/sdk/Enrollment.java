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

import android.os.Parcel;
import android.os.Parcelable;

public class Enrollment implements GuardianEnrollment, Parcelable {

    private final String id;
    private final String url;
    private final String tenant;
    private final String user;
    private final int period;
    private final int digits;
    private final String algorithm;
    private final String secret;
    private final String deviceId;
    private final String deviceLocalIdentifier;
    private final String deviceName;
    private final String deviceGCMToken;
    private final String deviceToken;

    Enrollment(String url,
               String tenant,
               String user,
               int period,
               int digits,
               String algorithm,
               String secret,
               String deviceId,
               String deviceLocalIdentifier,
               String deviceName,
               String deviceGCMToken,
               String deviceToken) {
        this.id = String.format("%s/%s", url, deviceId);
        this.url = url;
        this.tenant = tenant;
        this.user = user;
        this.period = period;
        this.digits = digits;
        this.algorithm = algorithm;
        this.secret = secret;
        this.deviceId = deviceId;
        this.deviceLocalIdentifier = deviceLocalIdentifier;
        this.deviceName = deviceName;
        this.deviceGCMToken = deviceGCMToken;
        this.deviceToken = deviceToken;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String getTenant() {
        return tenant;
    }

    @Override
    public String getUser() {
        return user;
    }

    @Override
    public int getPeriod() {
        return period;
    }

    @Override
    public int getDigits() {
        return digits;
    }

    @Override
    public String getAlgorithm() {
        return algorithm;
    }

    @Override
    public String getSecret() {
        return secret;
    }

    @Override
    public String getDeviceId() {
        return deviceId;
    }

    @Override
    public String getDeviceLocalIdentifier() {
        return deviceLocalIdentifier;
    }

    @Override
    public String getDeviceName() {
        return deviceName;
    }

    @Override
    public String getGCMToken() {
        return deviceGCMToken;
    }

    @Override
    public String getDeviceToken() {
        return deviceToken;
    }

    //// PARCELABLE
    protected Enrollment(Parcel in) {
        id = in.readString();
        url = in.readString();
        tenant = in.readString();
        user = in.readString();
        period = in.readInt();
        digits = in.readInt();
        algorithm = in.readString();
        secret = in.readString();
        deviceId = in.readString();
        deviceLocalIdentifier = in.readString();
        deviceName = in.readString();
        deviceGCMToken = in.readString();
        deviceToken = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(url);
        dest.writeString(tenant);
        dest.writeString(user);
        dest.writeInt(period);
        dest.writeInt(digits);
        dest.writeString(algorithm);
        dest.writeString(secret);
        dest.writeString(deviceId);
        dest.writeString(deviceLocalIdentifier);
        dest.writeString(deviceName);
        dest.writeString(deviceGCMToken);
        dest.writeString(deviceToken);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Enrollment> CREATOR = new Parcelable.Creator<Enrollment>() {
        @Override
        public Enrollment createFromParcel(Parcel in) {
            return new Enrollment(in);
        }

        @Override
        public Enrollment[] newArray(int size) {
            return new Enrollment[size];
        }
    };
}
