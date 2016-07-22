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

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.auth0.android.guardian.sdk.otp.TOTP;
import com.auth0.android.guardian.sdk.otp.utils.Base32;

/**
 * Guardian is the core of the Guardian SDK.
 * This class allows to create and delete enrollments, parse the push notifications and allow or
 * reject authentication requests
 */
public class Guardian implements Parcelable {

    private final GuardianAPIClient client;

    Guardian(@NonNull GuardianAPIClient client) {
        this.client = client;
    }

    /**
     * Creates an enroll. When successful, returns a new Enrollment that is required to allow or
     * reject authentication requests.
     * This device will now be available as a Guardian second factor.
     *
     * @param enrollmentUri the URI obtained from a Guardian QR code
     * @param deviceName this device's name
     * @param gcmToken the GCM token required to send push notifications to this device
     * @return a request to execute or start
     * @throws IllegalArgumentException when the enrollmentUri is not a valid Guardian enrollment
     *                                  uri
     */
    @NonNull
    public GuardianAPIRequest<Enrollment> enroll(@NonNull Uri enrollmentUri,
                                                 @NonNull String deviceName,
                                                 @NonNull String gcmToken) {
        EnrollmentData enrollmentData = EnrollmentData.parse(enrollmentUri);
        return new EnrollRequest(client, enrollmentData, deviceName, gcmToken);
    }

    /**
     * Deletes an enrollment. When successful, the enrollment becomes useless and this device will
     * not be available to allow or reject subsequent authentication requests.
     *
     * @param enrollment the enrollment to delete
     * @return a request to execute or start
     */
    @NonNull
    public GuardianAPIRequest<Void> delete(@NonNull GuardianEnrollment enrollment) {
        return client
                .device(enrollment.getId(), enrollment.getDeviceToken())
                .delete();
    }

    /**
     * Allows an authentication request
     *
     * @param notification the (parsed) push notification received
     * @param enrollment the enrollment to whom the notification corresponds
     * @return a request to execute or start
     */
    @NonNull
    public GuardianAPIRequest<Void> allow(@NonNull GuardianNotification notification,
                                          @NonNull GuardianEnrollment enrollment) {
        return client
                .allow(notification.getTransactionToken(), getOTPCode(enrollment));
    }

    /**
     * Rejects an authentication request
     *
     * @param notification the (parsed) push notification received
     * @param enrollment the enrollment to whom the notification corresponds
     * @param reason the reject reason
     * @return a request to execute or start
     * @see #reject(GuardianNotification, GuardianEnrollment)
     * @throws IllegalArgumentException when the enrollment's TOTP data is not valid
     */
    @NonNull
    public GuardianAPIRequest<Void> reject(@NonNull GuardianNotification notification,
                                           @NonNull GuardianEnrollment enrollment,
                                           @Nullable String reason) {
        return client
                .reject(notification.getTransactionToken(), getOTPCode(enrollment), reason);
    }

    /**
     * Rejects an authentication request
     *
     * @param notification the (parsed) push notification received
     * @param enrollment the enrollment to whom the notification corresponds
     * @return a request to execute or start
     * @see #reject(GuardianNotification, GuardianEnrollment, String)
     */
    @NonNull
    public GuardianAPIRequest<Void> reject(@NonNull GuardianNotification notification,
                                           @NonNull GuardianEnrollment enrollment) {
        return reject(notification, enrollment, null);
    }

    /**
     * The low level Guardian API Client
     *
     * @return the API client
     */
    @NonNull
    public GuardianAPIClient getAPIClient() {
        return client;
    }

    /**
     * Parses the Bundle received from the GCM push notification into a GuardianNotification
     *
     * @param pushNotificationPayload the GCM payload Bundle
     * @return the parsed data
     * @throws IllegalArgumentException when the push notification is not a valid Guardian
     *                                  notification
     */
    @NonNull
    public static Notification parseNotification(@NonNull Bundle pushNotificationPayload) {
        return Notification.parse(pushNotificationPayload);
    }

    String getOTPCode(GuardianEnrollment enrollment) {
        try {
            TOTP totp = new TOTP(
                    enrollment.getAlgorithm(),
                    Base32.decode(enrollment.getSecret()),
                    enrollment.getDigits(),
                    enrollment.getPeriod());
            return totp.generate();
        } catch (Base32.DecodingException e) {
            throw new IllegalArgumentException(
                    "Enrollment's secret is not a valid Base32 encoded TOTP secret", e);
        }
    }

    /**
     * A {@link Guardian} Builder
     */
    public static class Builder {

        private Uri url;

        /**
         * Set the URL of the Guardian server.
         * For example {@code https://tenant.guardian.auth0.com/}
         *
         * @param url the url
         * @return itself
         */
        public Builder url(@NonNull Uri url) {
            if (this.url != null) {
                throw new IllegalArgumentException("You need to set only one domain or url");
            }
            this.url = url;
            return this;
        }

        /**
         * Set the domain of the Guardian server.
         * For example {@code tenant.guardian.auth0.com}
         *
         * @param domain the domain name
         * @return itself
         */
        public Builder domain(@NonNull String domain) {
            if (this.url != null) {
                throw new IllegalArgumentException("You need to set only one domain or url");
            }
            this.url = new Uri.Builder()
                    .scheme("https")
                    .authority(domain)
                    .build();
            return this;
        }

        /**
         * Builds and returns the Guardian instance
         *
         * @return the created instance
         */
        public Guardian build() {
            if (url == null) {
                throw new IllegalArgumentException("You need to set either a domain or an url");
            }

            GuardianAPIClient client = new GuardianAPIClient.Builder()
                    .baseUrl(url.toString())
                    .build();

            return new Guardian(client);
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
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Guardian> CREATOR = new Parcelable.Creator<Guardian>() {
        @Override
        public Guardian createFromParcel(Parcel in) {
            String url = in.readString();
            return new Builder()
                    .url(Uri.parse(url))
                    .build();
        }

        @Override
        public Guardian[] newArray(int size) {
            return new Guardian[size];
        }
    };
}
