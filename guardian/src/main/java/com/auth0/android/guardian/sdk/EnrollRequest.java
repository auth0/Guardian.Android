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

import com.auth0.android.guardian.sdk.networking.Callback;

import java.io.IOException;
import java.security.KeyPair;
import java.util.Map;

class EnrollRequest implements GuardianAPIRequest<Enrollment> {

    private final static String KEY_ID = "id";
    private final static String KEY_URL = "url";
    private final static String KEY_ISSUER = "issuer";
    private final static String KEY_USER_ID = "user_id";
    private final static String KEY_TOKEN = "token";
    private final static String KEY_TOTP = "totp";
    private final static String KEY_SECRET = "secret";
    private final static String KEY_ALGORITHM = "algorithm";
    private final static String KEY_PERIOD = "period";
    private final static String KEY_DIGITS = "digits";

    final GuardianAPIRequest<Map<String, Object>> request;
    final CurrentDevice device;
    final KeyPair deviceKeyPair;

    EnrollRequest(@NonNull GuardianAPIRequest<Map<String, Object>> request,
                  @NonNull CurrentDevice device,
                  @NonNull KeyPair deviceKeyPair) {
        this.request = request;
        this.device = device;
        this.deviceKeyPair = deviceKeyPair;
    }

    @Override
    public Enrollment execute() throws IOException, GuardianException {
        return createEnrollment(request.execute());
    }

    @Override
    public void start(@NonNull final Callback<Enrollment> callback) {
        request.start(new Callback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> response) {
                try {
                    Enrollment enrollment = createEnrollment(response);
                    callback.onSuccess(enrollment);
                } catch (GuardianException e) {
                    callback.onFailure(e);
                }
            }

            @Override
            public void onFailure(Throwable exception) {
                callback.onFailure(exception);
            }
        });
    }

    private Enrollment createEnrollment(@NonNull Map<String, Object> result) {

        if (!result.containsKey(KEY_ID)
                || !result.containsKey(KEY_URL)
                || !result.containsKey(KEY_ISSUER)
                || !result.containsKey(KEY_USER_ID)
                || !result.containsKey(KEY_TOKEN)) {
            throw new GuardianException("Invalid response, missing required fields " + result);
        }

        String enrollmentId = (String) result.get(KEY_ID);
        String userId = (String) result.get(KEY_USER_ID);
        String deviceToken = (String) result.get(KEY_TOKEN);
        String totpAlgorithm = null, totpSecret = null;
        Integer totpPeriod = null, totpDigits = null;
        if (result.containsKey(KEY_TOTP)) {
            Map<String, Object> totpData = (Map<String, Object>) result.get(KEY_TOTP);
            totpAlgorithm = (String) totpData.get(KEY_ALGORITHM);
            totpSecret = (String) totpData.get(KEY_SECRET);
            totpDigits = ((Double) totpData.get(KEY_DIGITS)).intValue();
            totpPeriod = ((Double) totpData.get(KEY_PERIOD)).intValue();
        }

        return new GuardianEnrollment(userId, totpPeriod, totpDigits, totpAlgorithm,
                totpSecret, enrollmentId, device, deviceToken, deviceKeyPair.getPrivate());
    }
}
