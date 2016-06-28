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
 * Contains the token used to update or delete the {@link DeviceAccount} on the server
 * <p>
 * Instances of this class should only be created by {@link GuardianAPI} and must be used as value
 * objects.
 *
 * @author Nicolas Ulrich (nikolaseu@gmail.com)
 * @see GuardianAPI#getEnrollmentInfo
 * @see GuardianAPI#updateDeviceAccount
 * @see GuardianAPI#deleteDeviceAccount
 */
public class EnrollmentInfo {

    @JsonRequired
    @SerializedName("device_account_token")
    private String deviceAccountToken;

    /**
     * Returns the token required to update or delete an {@link DeviceAccount}
     *
     * @return the token
     * @see GuardianAPI#updateDeviceAccount
     * @see GuardianAPI#deleteDeviceAccount
     */
    public String getDeviceAccountToken() {
        return deviceAccountToken;
    }
}
