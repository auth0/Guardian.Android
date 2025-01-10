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
import androidx.annotation.NonNull;
import android.util.Base64;

import com.auth0.android.guardian.sdk.Enrollment;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;

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

    @SerializedName("privateKey")
    private final String privateKey;

    @SerializedName("publicKey")
    private final String publicKey;

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
        this.privateKey = Base64.encodeToString(enrollment.getSigningKey().getEncoded(), Base64.DEFAULT);
        this.publicKey = Base64.encodeToString(enrollment.getPublicKey().getEncoded(), Base64.DEFAULT);
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

    @NonNull
    @Override
    public PublicKey getPublicKey() {
        try {
            if (publicKey == null || publicKey.isEmpty()) {
                // For backwards-compatibility with enrollments encoded without a public key in
                // version 1.8.1 and before of the Guardian.Android SDK
                return getPublicKeyFromSigningKey();
            }

            byte[] key = Base64.decode(publicKey, Base64.DEFAULT);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(key);
            return keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchProviderException e) {
            throw new IllegalStateException("Invalid public key!");
        }
    }

    private PublicKey getPublicKeyFromSigningKey() throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        // BouncyCastleProvider is used in order to ensure compatibility when targeting Android API Level 32
        // and lower due to changes to how the default provider handles decoding of RSA keys across versions.
        // Prior to API Level 32 the default Security Provider will decode PKCS8Encoded private key to a
        // RSAPrivateKey rather than RSAPrivateCrtKey which discards the public exponent required to reconstruct
        // the public key.
        KeyFactory keyFactory = KeyFactory.getInstance("RSA", new BouncyCastleProvider());

        byte[] signingKeyBytes = Base64.decode(privateKey, Base64.DEFAULT);
        PKCS8EncodedKeySpec signingKeySpec = new PKCS8EncodedKeySpec(signingKeyBytes);
        PrivateKey signingKey = keyFactory.generatePrivate(signingKeySpec);

        if (signingKey instanceof RSAPrivateCrtKey) {
            RSAPrivateCrtKey rsaPrivateKey = (RSAPrivateCrtKey) signingKey;
            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(rsaPrivateKey.getModulus(), rsaPrivateKey.getPublicExponent());
            return keyFactory.generatePublic(publicKeySpec);
        } else {
            throw new IllegalStateException("Not an RSA private key.");
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
        privateKey = in.readString();
        publicKey = in.readString();
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
        dest.writeString(privateKey);
        dest.writeString(publicKey);
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