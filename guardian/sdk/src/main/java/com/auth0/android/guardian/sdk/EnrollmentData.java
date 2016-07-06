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
import android.support.annotation.NonNull;

class EnrollmentData {

    private static final String OTPAUTH_SCHEME = "otpauth";
    private static final String TOTP_AUTHORITY = "totp";

    private static final String DEFAULT_ALGORITHM = "SHA1";
    private static final int DEFAULT_DIGITS = 6;
    private static final int DEFAULT_PERIOD = 30;

    private final String baseUrl;
    private final String deviceId;
    private final String enrollmentTransactionId;
    private final String user;
    private final String issuer;
    private final String algorithm;
    private final int digits;
    private final int period;
    private final String secret;

    EnrollmentData(String baseUrl,
                   String deviceId,
                   String enrollmentTransactionId,
                   String user,
                   String issuer,
                   String algorithm,
                   int digits,
                   int period,
                   String secret) {
        this.baseUrl = baseUrl;
        this.deviceId = deviceId;
        this.enrollmentTransactionId = enrollmentTransactionId;
        this.user = user;
        this.issuer = issuer;
        this.algorithm = algorithm;
        this.digits = digits;
        this.period = period;
        this.secret = secret;
    }

    @NonNull
    static EnrollmentData parse(@NonNull String rawData) {
        Uri uri = Uri.parse(rawData);

        if (!OTPAUTH_SCHEME.equals(uri.getScheme())) {
            throw new IllegalArgumentException(
                    String.format("Invalid data: scheme != 'otpauth' (is '%s') in '%s'",
                            uri.getScheme(), rawData));
        }

        if (!TOTP_AUTHORITY.equals(uri.getAuthority())) {
            throw new IllegalArgumentException(
                    String.format("Invalid data: authority != 'totp' (is '%s') in '%s'",
                            uri.getAuthority(), rawData));
        }

        String digitsParam = uri.getQueryParameter("digits");
        int digits;
        if (digitsParam == null) {
            digits = DEFAULT_DIGITS;
        } else {
            try {
                digits = Integer.parseInt(digitsParam);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid data: 'digits' must be an integer", e);
            }
        }

        String periodParam = uri.getQueryParameter("period");
        int period;
        if (periodParam == null) {
            period = DEFAULT_PERIOD;
        } else {
            try {
                period = Integer.parseInt(periodParam);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid data: 'period' must be an integer", e);
            }
        }

        String algorithm = uri.getQueryParameter("algorithm");
        if (algorithm == null) {
            algorithm = DEFAULT_ALGORITHM;
        }

        String path = uri.getPath();
        if (path == null || path.length() < 2) {
            throw new IllegalArgumentException("Invalid data: path must contain the label");
        }

        String label = path.substring(1);
        String[] labelParts = label.split(":");
        String issuer = null;
        String user;
        if (labelParts.length > 1) {
            issuer = labelParts[0];
            user = labelParts[1];
        } else {
            user = label;
        }

        String secret = uri.getQueryParameter("secret");
        if (secret == null) {
            throw new IllegalArgumentException("Invalid data: it must have a 'secret'");
        }

        String issuerParam = uri.getQueryParameter("issuer");
        if (issuerParam != null && issuer != null && !issuerParam.equals(issuer)) {
            throw new IllegalArgumentException("Invalid data: if both 'issuer' and issuer prefix" +
                    " at 'label' are present, they must be equal");
        }
        if (issuerParam != null) {
            issuer = issuerParam;
        }
        if (issuer == null) {
            throw new IllegalArgumentException("Invalid data: it must have an 'issuer' or issuer" +
                    " prefix at 'label'");
        }

        String enrollmentTransactionId = uri.getQueryParameter("enrollment_tx_id");
        String deviceId = uri.getQueryParameter("id");
        String baseUrl = uri.getQueryParameter("base_url");

        return new EnrollmentData(baseUrl, deviceId, enrollmentTransactionId, user,
                issuer, algorithm, digits, period, secret);
    }

    public String getUser() {
        return user;
    }

    public String getSecret() {
        return secret;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getEnrollmentTransactionId() {
        return enrollmentTransactionId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public int getDigits() {
        return digits;
    }

    public int getPeriod() {
        return period;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
