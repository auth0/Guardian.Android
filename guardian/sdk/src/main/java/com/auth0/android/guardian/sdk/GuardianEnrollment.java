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

class GuardianEnrollment implements Enrollment {

    private final String id;
    private final String url;
    private final String label;
    private final String user;
    private final int period;
    private final int digits;
    private final String algorithm;
    private final String secret;
    private final String deviceIdentifier;
    private final String deviceName;
    private final String deviceGCMToken;
    private final String deviceToken;

    GuardianEnrollment(String url,
                       String label,
                       String user,
                       int period,
                       int digits,
                       String algorithm,
                       String secret,
                       String deviceId,
                       String deviceIdentifier,
                       String deviceName,
                       String deviceGCMToken,
                       String deviceToken) {
        this.url = url;
        this.label = label;
        this.user = user;
        this.period = period;
        this.digits = digits;
        this.algorithm = algorithm;
        this.secret = secret;
        this.id = deviceId;
        this.deviceIdentifier = deviceIdentifier;
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
}
