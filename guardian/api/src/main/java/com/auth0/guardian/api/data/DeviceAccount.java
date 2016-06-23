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
 * Contains the information of a Guardian server-side account and is the result of a new enroll or
 * the update of an existing account:
 * <ul>
 * <li>id: a server issued id for the account
 * <li>identifier: a unique identifier for the device
 * <li>name: the friendly name that will be used to notify the user that a push notification has
 * been sent to this device.
 * <li>push_credentials: the push service identifier and token
 * </ul>
 * <p>
 * Instances of this class should only be created by {@link GuardianAPI} and must be used as value
 * objects.
 *
 * @author Nicolas Ulrich (nikolaseu@gmail.com)
 * @see GuardianAPI#updateDeviceAccount
 * @see PushCredentials
 */
public class DeviceAccount {

    @JsonRequired
    @SerializedName("id")
    private String id;

    @JsonRequired
    @SerializedName("identifier")
    private String identifier;

    @JsonRequired
    @SerializedName("name")
    private String name;

    @JsonRequired
    @SerializedName("push_credentials")
    private PushCredentials pushCredentials;

    /**
     * Returns the server issued id for the account
     *
     * @return the account id
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the unique identifier for the device
     *
     * @return the device identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Return the friendly name that will be used to notify the user that a push notification has
     * been sent to this device
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Return the push service identifier and token
     *
     * @return the {@link PushCredentials} containing the identifier and token
     */
    public PushCredentials getPushCredentials() {
        return pushCredentials;
    }

    @Override
    public String toString() {
        return "DeviceAccount{" +
                "id='" + id + '\'' +
                ", identifier='" + identifier + '\'' +
                ", name='" + name + '\'' +
                ", pushCredentials=" + pushCredentials +
                '}';
    }
}
