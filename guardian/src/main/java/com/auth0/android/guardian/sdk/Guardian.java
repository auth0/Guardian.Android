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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.auth0.android.guardian.sdk.otp.TOTP;
import com.auth0.android.guardian.sdk.otp.utils.Base32;

import java.security.KeyPair;
import java.util.Map;

/**
 * Guardian is the core of the Guardian SDK.
 * This class allows to create and delete enrollments, parse the push notifications and allow or
 * reject authentication requests
 */
public class Guardian {

    private final GuardianAPIClient client;

    Guardian(@NonNull GuardianAPIClient client) {
        this.client = client;
    }

    /**
     * Creates an enroll. When successful, returns a new Enrollment that is required to allow or
     * reject authentication requests.
     * <p>
     * This device will now be available as a Guardian second factor.
     *
     * @param enrollmentData the enrollment URI or ticket obtained from a Guardian QR code or
     *                       enrollment email
     * @param device         the data of the device to enroll
     * @param deviceKeyPair  the RSA key pair to associate with the enrollment
     * @return a request to execute or start
     * @throws IllegalArgumentException when the key pair in not an RSA key pair
     */
    @NonNull
    public GuardianAPIRequest<Enrollment> enroll(@NonNull String enrollmentData,
                                                 @NonNull CurrentDevice device,
                                                 @NonNull KeyPair deviceKeyPair) {
        final String ticket;
        final Uri uri = Uri.parse(enrollmentData);
        if (uri != null && uri.getQueryParameterNames().contains("enrollment_tx_id")) {
            ticket = uri.getQueryParameter("enrollment_tx_id");
        } else {
            ticket = enrollmentData;
        }
        final GuardianAPIRequest<Map<String, Object>> request = client
                .enroll(ticket, device.getIdentifier(), device.getName(),
                        device.getNotificationToken(), deviceKeyPair.getPublic());
        return new EnrollRequest(request, device, deviceKeyPair);
    }

    /**
     * Deletes an enrollment. When successful, the enrollment becomes useless and this device will
     * not be available to allow or reject subsequent authentication requests.
     *
     * @param enrollment the enrollment to delete
     * @return a request to execute or start
     */
    @NonNull
    public GuardianAPIRequest<Void> delete(@NonNull Enrollment enrollment) {
        return client
                .device(enrollment.getId(), enrollment.getUserId(), enrollment.getSigningKey())
                .delete();
    }

    /**
     * Allows an authentication request
     *
     * @param notification the (parsed) push notification received
     * @param enrollment   the enrollment to whom the notification corresponds
     * @return a request to execute or start
     */
    @NonNull
    public GuardianAPIRequest<Void> allow(@NonNull Notification notification,
                                          @NonNull Enrollment enrollment) {
        return client
                .allow(notification.getTransactionToken(), enrollment.getDeviceIdentifier(),
                        notification.getChallenge(), enrollment.getSigningKey());
    }

    /**
     * Rejects an authentication request
     *
     * @param notification the (parsed) push notification received
     * @param enrollment   the enrollment to whom the notification corresponds
     * @param reason       the reject reason
     * @return a request to execute or start
     * @see #reject(Notification, Enrollment)
     */
    @NonNull
    public GuardianAPIRequest<Void> reject(@NonNull Notification notification,
                                           @NonNull Enrollment enrollment,
                                           @Nullable String reason) {
        return client
                .reject(notification.getTransactionToken(), enrollment.getDeviceIdentifier(),
                        notification.getChallenge(), enrollment.getSigningKey(), reason);
    }

    /**
     * Rejects an authentication request
     *
     * @param notification the (parsed) push notification received
     * @param enrollment   the enrollment to whom the notification corresponds
     * @return a request to execute or start
     * @see #reject(Notification, Enrollment, String)
     */
    @NonNull
    public GuardianAPIRequest<Void> reject(@NonNull Notification notification,
                                           @NonNull Enrollment enrollment) {
        return reject(notification, enrollment, null);
    }

    GuardianAPIClient getAPIClient() {
        return client;
    }

    /**
     * Parses the Bundle received from the GCM push notification into a Notification
     *
     * @param pushNotificationPayload the GCM payload Bundle
     * @return the parsed data, or null if the push notification is not a valid Guardian
     * notification
     * @deprecated After migrating to FCM you should use {@link #parseNotification(Map)} instead.
     */
    @SuppressWarnings("unused")
    @Nullable
    @Deprecated
    public static ParcelableNotification parseNotification(@NonNull Bundle pushNotificationPayload) {
        return ParcelableNotification.parse(pushNotificationPayload);
    }

    /**
     * Parses the Map received from the FCM push notification into a Notification
     *
     * @param pushNotificationPayload the FCM payload Map
     * @return the parsed data, or null if the push notification is not a valid Guardian
     * notification
     */
    @SuppressWarnings("unused")
    @Nullable
    public static ParcelableNotification parseNotification(@NonNull Map<String, String> pushNotificationPayload) {
        return ParcelableNotification.parse(pushNotificationPayload);
    }

    /**
     * Returns the current OTP code for the {@link Enrollment}. This can be used for manual input,
     * when there's no internet connection or the push notification is not received.
     *
     * @param enrollment the enrollment for which to generate the OTP
     * @return the OTP code, or null if the enrollment doesn't include OTP data
     * @throws IllegalArgumentException when the enrollment's OTP data is invalid
     */
    @Nullable
    public static String getOTPCode(Enrollment enrollment) {
        if (enrollment.getSecret() == null
                || enrollment.getAlgorithm() == null
                || enrollment.getDigits() == null
                || enrollment.getPeriod() == null) {
            return null;
        }
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
        private boolean loggingEnabled = false;

        /**
         * Set the URL of the Guardian server.
         * For example {@code https://tenant.guardian.auth0.com/}
         *
         * @param url the url
         * @return itself
         * @throws IllegalArgumentException when an url or domain was already set
         */
        public Builder url(@NonNull Uri url) {
            if (this.url != null) {
                throw new IllegalArgumentException("An url/domain was already set");
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
         * @throws IllegalArgumentException when an url or domain was already set
         */
        public Builder domain(@NonNull String domain) {
            if (this.url != null) {
                throw new IllegalArgumentException("An url/domain was already set");
            }
            this.url = new Uri.Builder()
                    .scheme("https")
                    .authority(domain)
                    .build();
            return this;
        }

        /**
         * Enables the logging of all HTTP requests to the console.
         * <p>
         * Should only be used during development, on debug builds
         *
         * @return itself
         */
        public Builder enableLogging() {
            this.loggingEnabled = true;
            return this;
        }

        /**
         * Builds and returns the Guardian instance
         *
         * @return the created instance
         * @throws IllegalStateException when the builder was not configured correctly
         */
        public Guardian build() {
            if (this.url == null) {
                throw new IllegalStateException("You must set either a domain or an url");
            }

            GuardianAPIClient.Builder builder = new GuardianAPIClient.Builder()
                    .url(url);

            if (loggingEnabled) {
                builder.enableLogging();
            }

            return new Guardian(builder.build());
        }
    }
}
