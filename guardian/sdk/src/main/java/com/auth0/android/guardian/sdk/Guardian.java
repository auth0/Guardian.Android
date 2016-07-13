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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.auth0.android.guardian.sdk.otp.TOTP;
import com.auth0.android.guardian.sdk.otp.utils.Base32;

public class Guardian implements Parcelable {

    private final GuardianAPIClient client;
    private final String deviceName;
    private final String gcmToken;

    Guardian(@NonNull GuardianAPIClient client,
             @NonNull String deviceName,
             @NonNull String gcmToken) {
        this.client = client;
        this.deviceName = deviceName;
        this.gcmToken = gcmToken;
    }

    public GuardianAPIRequest<Enrollment> enroll(@NonNull String enrollmentUri) {
        EnrollmentData enrollmentData = EnrollmentData.parse(enrollmentUri);
        return new EnrollRequest(client, enrollmentData, deviceName, gcmToken);
    }

    public GuardianAPIRequest<Void> delete(@NonNull GuardianEnrollment enrollment) {
        return client
                .device(enrollment.getDeviceId(), enrollment.getDeviceToken())
                .delete();
    }

    public GuardianAPIRequest<Void> allow(@NonNull GuardianEnrollment enrollment,
                                          @NonNull GuardianNotification notification) {
        return client
                .allow(notification.getTransactionToken(), getOTPCode(enrollment));
    }

    public GuardianAPIRequest<Void> reject(@NonNull GuardianEnrollment enrollment,
                                           @NonNull GuardianNotification notification,
                                           @Nullable String reason) {
        return client
                .reject(notification.getTransactionToken(), getOTPCode(enrollment), reason);
    }

    public GuardianAPIRequest<Void> reject(@NonNull GuardianEnrollment enrollment,
                                           @NonNull GuardianNotification notification) {
        return reject(enrollment, notification, null);
    }

    @Nullable
    public static Notification parseNotification(@NonNull Bundle pushNotificationPayload) {
        return Notification.parse(pushNotificationPayload);
    }

    private String getOTPCode(GuardianEnrollment enrollment) {
        try {
            TOTP totp = new TOTP(
                    enrollment.getAlgorithm(),
                    Base32.decode(enrollment.getSecret()),
                    enrollment.getDigits(),
                    enrollment.getPeriod());
            return totp.generateOTP();
        } catch (Base32.DecodingException e) {
            throw new GuardianException("Unable to generate OTP: could not decode secret", e);
        }
    }

    public static class Builder {

        private String url;
        private String deviceName;
        private String gcmToken;

        public Builder url(@NonNull String url) {
            this.url = url;
            return this;
        }

        public Builder deviceName(@NonNull String deviceName) {
            this.deviceName = deviceName;
            return this;
        }

        public Builder gcmToken(@NonNull String gcmToken) {
            this.gcmToken = gcmToken;
            return this;
        }

        public Guardian build() {
            GuardianAPIClient client = new GuardianAPIClient.Builder()
                    .baseUrl(url)
                    .build();

            if (deviceName == null) {
                throw new IllegalArgumentException("deviceName cannot be null");
            }

            if (gcmToken == null) {
                throw new IllegalArgumentException("gcmToken cannot be null");
            }

            return new Guardian(client, deviceName, gcmToken);
        }
    }

    // PARCELABLE
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(client.getUrl());
        dest.writeString(deviceName);
        dest.writeString(gcmToken);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Guardian> CREATOR = new Parcelable.Creator<Guardian>() {
        @Override
        public Guardian createFromParcel(Parcel in) {
            String url = in.readString();
            String deviceName = in.readString();
            String gcmToken = in.readString();
            return new Builder()
                    .url(url)
                    .deviceName(deviceName)
                    .gcmToken(gcmToken)
                    .build();
        }

        @Override
        public Guardian[] newArray(int size) {
            return new Guardian[size];
        }
    };
}
