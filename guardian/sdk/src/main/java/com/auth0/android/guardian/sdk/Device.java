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

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class Device {

    @SerializedName("id")
    private String enrollmentId;

    @SerializedName("identifier")
    private String deviceIdentifier;

    @SerializedName("name")
    private String deviceName;

    @SerializedName("push_credentials")
    private Map<String, String> pushCredentials;

    /**
     * The Guardian enrollment enrollmentId
     *
     * @return the enrollment enrollmentId
     * @see GuardianEnrollment#getId()
     */
    public String getEnrollmentId() {
        return enrollmentId;
    }

    /**
     * The identifier of the physical device, for debug/tracking purposes.
     * Usually will be {@link android.provider.Settings.Secure#ANDROID_ID}
     *
     * @return a unique identifier
     * @see GuardianEnrollment#getDeviceIdentifier()
     */
    public String getDeviceIdentifier() {
        return deviceIdentifier;
    }

    /**
     * The deviceName to display whenever it is necessary to identify this specific enrollment.
     * For example when the user has to choose where to send the push notification, or at the admin
     * interface if the user wants to delete an enrollment from there
     *
     * @return the deviceName
     * @see GuardianEnrollment#getDeviceName()
     */
    public String getDeviceName() {
        return deviceName;
    }

    /**
     * The GCM token for this physical device, required to check against the current token and
     * update the server in case it's not the same.
     * Needs to be up-to-data for the push notifications to work.
     *
     * @return the GCM token
     * @see GuardianEnrollment#getGCMToken()
     */
    public String getGCMToken() {
        return pushCredentials.get("token");
    }
}
