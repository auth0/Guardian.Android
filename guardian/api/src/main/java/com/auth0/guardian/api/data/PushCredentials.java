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

package com.auth0.guardian.api.data;

import com.auth0.guardian.api.GuardianAPI;
import com.auth0.requests.gson.JsonRequired;
import com.google.gson.annotations.SerializedName;

/**
 * Contains the push service identifier and token
 * <p>
 * Instances of this class should only be created by {@link GuardianAPI} and must be used as value
 * objects.
 *
 * @author Nicolas Ulrich (nikolaseu@gmail.com)
 * @see DeviceAccount
 * @see GuardianAPI#updateDeviceAccount
 */
public class PushCredentials {

    @JsonRequired
    @SerializedName("service")
    private String service;

    @JsonRequired
    @SerializedName("token")
    private String token;

    public PushCredentials(String service, String token) {
        this.service = service;
        this.token = token;
    }

    /**
     * Returns the identifier used by Guardian to indicate the push service to use
     *
     * @return the push service identifier
     */
    public String getService() {
        return service;
    }

    /**
     * Returns the token used by the push service
     *
     * @return the push service token
     */
    public String getToken() {
        return token;
    }

    @Override
    public String toString() {
        return "PushCredentials{" +
                "service='" + service + '\'' +
                ", token='" + token + '\'' +
                '}';
    }
}
