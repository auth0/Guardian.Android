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

import okhttp3.HttpUrl;

class EnrollmentData {

    private static final String OTPAUTH_SCHEME = "otpauth";
    private static final String TOTP_AUTHORITY = "totp";

    private static final String DEFAULT_ALGORITHM = "SHA1";
    private static final int DEFAULT_DIGITS = 6;
    private static final int DEFAULT_PERIOD = 30;

    private final HttpUrl url;
    private final String deviceId;
    private final String enrollmentTransactionId;
    private final String user;
    private final String issuer;
    private final String algorithm;
    private final int digits;
    private final int period;
    private final String secret;

    EnrollmentData(HttpUrl url,
                   String deviceId,
                   String enrollmentTransactionId,
                   String user,
                   String issuer,
                   String algorithm,
                   int digits,
                   int period,
                   String secret) {
        this.url = url;
        this.deviceId = deviceId;
        this.enrollmentTransactionId = enrollmentTransactionId;
        this.user = user;
        this.issuer = issuer;
        this.algorithm = algorithm;
        this.digits = digits;
        this.period = period;
        this.secret = secret;
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

    public HttpUrl getUrl() {
        return url;
    }

    @NonNull
    static EnrollmentData parse(@NonNull Uri uri) {
        if (!OTPAUTH_SCHEME.equals(uri.getScheme())) {
            throw new IllegalArgumentException(
                    String.format("Invalid data: scheme != 'otpauth' (is '%s') in '%s'",
                            uri.getScheme(), uri));
        }

        if (!TOTP_AUTHORITY.equals(uri.getAuthority())) {
            throw new IllegalArgumentException(
                    String.format("Invalid data: authority != 'totp' (is '%s') in '%s'",
                            uri.getAuthority(), uri));
        }

        String enrollmentTransactionId = uri.getQueryParameter("enrollment_tx_id");
        if (enrollmentTransactionId == null) {
            throw new IllegalArgumentException("Invalid data: it must have a 'enrollment_tx_id'");
        }

        String deviceId = uri.getQueryParameter("id");
        if (deviceId == null) {
            throw new IllegalArgumentException("Invalid data: it must have an 'id'");
        }

        String baseUrl = uri.getQueryParameter("base_url");
        HttpUrl url = HttpUrl.parse(baseUrl);
        if (url == null) {
            throw new IllegalArgumentException("Invalid data: it must have a valid 'base_url'");
        }

        String secret = uri.getQueryParameter("secret");
        if (secret == null) {
            throw new IllegalArgumentException("Invalid data: it must have a 'secret'");
        }

        String issuer = parseIssuer(uri);
        if (issuer == null) {
            throw new IllegalArgumentException("Invalid data: it must have an 'issuer' or issuer" +
                    " prefix at 'label'");
        }

        String algorithm = uri.getQueryParameter("algorithm");
        if (algorithm == null) {
            algorithm = DEFAULT_ALGORITHM;
        }

        String user = parseUser(uri);
        int digits = parseDigits(uri);
        int period = parsePeriod(uri);

        return new EnrollmentData(url, deviceId, enrollmentTransactionId, user,
                issuer, algorithm, digits, period, secret);
    }

    private static int parseDigits(Uri uri) {
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
        return digits;
    }

    private static int parsePeriod(Uri uri) {
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
        return period;
    }

    private static String parseUser(Uri uri) {
        String path = uri.getPath();
        if (path == null || path.length() < 2) {
            throw new IllegalArgumentException("Invalid data: path must contain the label");
        }

        String label = path.substring(1);
        String[] labelParts = label.split(":");
        String user;
        if (labelParts.length > 1) {
            user = labelParts[1];
        } else {
            user = label;
        }
        return user;
    }

    private static String parseIssuer(Uri uri) {
        String path = uri.getPath();
        if (path == null || path.length() < 2) {
            throw new IllegalArgumentException("Invalid data: path must contain the label");
        }

        String label = path.substring(1);
        String[] labelParts = label.split(":");
        String issuer = null;
        if (labelParts.length > 1) {
            issuer = labelParts[0];
        }

        String issuerParam = uri.getQueryParameter("issuer");
        if (issuerParam != null && issuer != null && !issuerParam.equals(issuer)) {
            throw new IllegalArgumentException("Invalid data: if both 'issuer' and issuer prefix" +
                    " at 'label' are present, they must be equal");
        }
        if (issuerParam != null) {
            issuer = issuerParam;
        }
        return issuer;
    }
}
