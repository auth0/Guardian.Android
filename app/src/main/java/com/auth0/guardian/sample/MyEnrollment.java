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

package com.auth0.guardian.sample;

import android.os.Parcel;
import android.os.Parcelable;

import com.auth0.android.guardian.sdk.Enrollment;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

public class MyEnrollment implements Enrollment, Parcelable {

    @SerializedName("id")
    private final String id;

    @SerializedName("url")
    private final String url;

    @SerializedName("label")
    private final String label;

    @SerializedName("user")
    private final String user;

    @SerializedName("period")
    private final int period;

    @SerializedName("digits")
    private final int digits;

    @SerializedName("algorithm")
    private final String algorithm;

    @SerializedName("secret")
    private final String secret;

    @SerializedName("deviceIdentifier")
    private final String deviceIdentifier;

    @SerializedName("deviceName")
    private final String deviceName;

    @SerializedName("deviceGCMToken")
    private final String deviceGCMToken;

    @SerializedName("deviceToken")
    private final String deviceToken;

    public MyEnrollment(Enrollment enrollment) {
        this.url = enrollment.getUrl();
        this.label = enrollment.getLabel();
        this.user = enrollment.getUser();
        this.period = enrollment.getPeriod();
        this.digits = enrollment.getDigits();
        this.algorithm = enrollment.getAlgorithm();
        this.secret = enrollment.getSecret();
        this.id = enrollment.getId();
        this.deviceIdentifier = enrollment.getDeviceIdentifier();
        this.deviceName = enrollment.getDeviceName();
        this.deviceGCMToken = enrollment.getGCMToken();
        this.deviceToken = enrollment.getDeviceToken();
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
    public String getLabel() {
        return label;
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
    public String getDeviceIdentifier() {
        return deviceIdentifier;
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

    // PARCELABLE
    protected MyEnrollment(Parcel in) {
        id = in.readString();
        url = in.readString();
        label = in.readString();
        user = in.readString();
        period = in.readInt();
        digits = in.readInt();
        algorithm = in.readString();
        secret = in.readString();
        deviceIdentifier = in.readString();
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
        dest.writeString(label);
        dest.writeString(user);
        dest.writeInt(period);
        dest.writeInt(digits);
        dest.writeString(algorithm);
        dest.writeString(secret);
        dest.writeString(deviceIdentifier);
        dest.writeString(deviceName);
        dest.writeString(deviceGCMToken);
        dest.writeString(deviceToken);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<MyEnrollment> CREATOR = new Parcelable.Creator<MyEnrollment>() {
        @Override
        public MyEnrollment createFromParcel(Parcel in) {
            return new MyEnrollment(in);
        }

        @Override
        public MyEnrollment[] newArray(int size) {
            return new MyEnrollment[size];
        }
    };

    // SIMPLE SERIALIZATION
    public String toJSON() {
        return JSON.toJson(this);
    }

    public static MyEnrollment fromJSON(String json) {
        return JSON.fromJson(json, MyEnrollment.class);
    }

    private static final Gson JSON = new GsonBuilder().create();
}