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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.security.PrivateKey;

class GuardianEnrollment implements Enrollment {

    private final String id;
    private final String userId;
    private final Integer period;
    private final Integer digits;
    private final String algorithm;
    private final String secret;
    private final String deviceIdentifier;
    private final String deviceName;
    private final String notificationToken;
    private final String deviceToken;
    private final PrivateKey privateKey;

    GuardianEnrollment(@NonNull String userId,
                       @Nullable Integer period,
                       @Nullable Integer digits,
                       @Nullable String algorithm,
                       @Nullable String secret,
                       @NonNull String enrollmentId,
                       @NonNull CurrentDevice device,
                       @NonNull String deviceToken,
                       @NonNull PrivateKey privateKey) {
        this.userId = userId;
        this.period = period;
        this.digits = digits;
        this.algorithm = algorithm;
        this.secret = secret;
        this.id = enrollmentId;
        this.deviceIdentifier = device.getIdentifier();
        this.deviceName = device.getName();
        this.notificationToken = device.getNotificationToken();
        this.deviceToken = deviceToken;
        this.privateKey = privateKey;
    }

    @NonNull
    @Override
    public String getId() {
        return id;
    }

    @NonNull
    @Override
    public String getUserId() {
        return userId;
    }

    @Nullable
    @Override
    public Integer getPeriod() {
        return period;
    }

    @Nullable
    @Override
    public Integer getDigits() {
        return digits;
    }

    @Nullable
    @Override
    public String getAlgorithm() {
        return algorithm;
    }

    @Nullable
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
        return notificationToken;
    }

    @NonNull
    @Override
    public String getDeviceToken() {
        return deviceToken;
    }

    @NonNull
    @Override
    public PrivateKey getSigningKey() {
        return privateKey;
    }
}
