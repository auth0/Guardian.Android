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
import android.support.annotation.NonNull;
import android.util.Base64;

import com.auth0.android.guardian.sdk.Enrollment;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

public class ParcelableEnrollment implements Enrollment, Parcelable {

    @SerializedName("id")
    private final String id;

    @SerializedName("userId")
    private final String userId;

    @SerializedName("period")
    private final Integer period;

    @SerializedName("digits")
    private final Integer digits;

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

    @SerializedName("recoveryCode")
    private final String recoveryCode;

    @SerializedName("privateKey")
    private final String privateKey;

    public ParcelableEnrollment(Enrollment enrollment) {
        this.userId = enrollment.getUserId();
        this.period = enrollment.getPeriod();
        this.digits = enrollment.getDigits();
        this.algorithm = enrollment.getAlgorithm();
        this.secret = enrollment.getSecret();
        this.id = enrollment.getId();
        this.deviceIdentifier = enrollment.getDeviceIdentifier();
        this.deviceName = enrollment.getDeviceName();
        this.deviceGCMToken = enrollment.getNotificationToken();
        this.deviceToken = enrollment.getDeviceToken();
        this.recoveryCode = enrollment.getRecoveryCode();
        this.privateKey = Base64.encodeToString(enrollment.getSigningKey().getEncoded(), Base64.DEFAULT);
    }

    @NonNull
    @Override
    public String getId() {
        return id;
    }

    @NonNull
    public String getUserId() {
        return userId;
    }

    @Override
    public Integer getPeriod() {
        return period;
    }

    @Override
    public Integer getDigits() {
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

    @NonNull
    @Override
    public String getDeviceIdentifier() {
        return deviceIdentifier;
    }

    @NonNull
    @Override
    public String getDeviceName() {
        return deviceName;
    }

    @NonNull
    @Override
    public String getNotificationToken() {
        return deviceGCMToken;
    }

    @NonNull
    @Override
    public String getDeviceToken() {
        return deviceToken;
    }

    @Override
    public String getRecoveryCode() {
        return recoveryCode;
    }

    @NonNull
    @Override
    public PrivateKey getSigningKey() {
        try {
            byte[] key = Base64.decode(privateKey, Base64.DEFAULT);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(key);
            return keyFactory.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Invalid private key!");
        }
    }

    // PARCELABLE
    protected ParcelableEnrollment(Parcel in) {
        id = in.readString();
        userId = in.readString();
        period = in.readInt();
        digits = in.readInt();
        algorithm = in.readString();
        secret = in.readString();
        deviceIdentifier = in.readString();
        deviceName = in.readString();
        deviceGCMToken = in.readString();
        deviceToken = in.readString();
        recoveryCode = in.readString();
        privateKey = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(userId);
        dest.writeInt(period);
        dest.writeInt(digits);
        dest.writeString(algorithm);
        dest.writeString(secret);
        dest.writeString(deviceIdentifier);
        dest.writeString(deviceName);
        dest.writeString(deviceGCMToken);
        dest.writeString(deviceToken);
        dest.writeString(recoveryCode);
        dest.writeString(privateKey);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<ParcelableEnrollment> CREATOR = new Parcelable.Creator<ParcelableEnrollment>() {
        @Override
        public ParcelableEnrollment createFromParcel(Parcel in) {
            return new ParcelableEnrollment(in);
        }

        @Override
        public ParcelableEnrollment[] newArray(int size) {
            return new ParcelableEnrollment[size];
        }
    };

    // SIMPLE SERIALIZATION
    public String toJSON() {
        return JSON.toJson(this);
    }

    public static ParcelableEnrollment fromJSON(String json) {
        return JSON.fromJson(json, ParcelableEnrollment.class);
    }

    private static final Gson JSON = new GsonBuilder().create();
}