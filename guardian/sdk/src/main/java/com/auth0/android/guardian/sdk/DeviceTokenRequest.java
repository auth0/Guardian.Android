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

import com.auth0.android.guardian.sdk.networking.Callback;
import com.auth0.android.guardian.sdk.networking.Request;

import java.io.IOException;
import java.util.Map;

class DeviceTokenRequest implements GuardianAPIRequest<String> {

    private static final String DEVICE_ACCOUNT_TOKEN_FIELD = "device_account_token";
    private final Request<Map<String, String>> request;

    DeviceTokenRequest(Request<Map<String, String>> request) {
        this.request = request;
    }

    @Override
    public String execute() throws IOException, GuardianException {
        Map<String, String> response = request.execute();
        if (!response.containsKey(DEVICE_ACCOUNT_TOKEN_FIELD)) {
            throw new GuardianException("Invalid server error response: " + response);
        }

        return response.get(DEVICE_ACCOUNT_TOKEN_FIELD);
    }

    @Override
    public void start(final Callback<String> callback) {
        request.start(new Callback<Map<String, String>>() {
            @Override
            public void onSuccess(Map<String, String> response) {
                if (!response.containsKey(DEVICE_ACCOUNT_TOKEN_FIELD)) {
                    callback.onFailure(new GuardianException("Invalid server error response: " + response));
                }

                callback.onSuccess(response.get(DEVICE_ACCOUNT_TOKEN_FIELD));
            }

            @Override
            public void onFailure(Throwable exception) {
                callback.onFailure(exception);
            }
        });
    }
}
