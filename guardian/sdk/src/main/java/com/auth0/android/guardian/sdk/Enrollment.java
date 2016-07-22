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

import android.provider.Settings;

/**
 * The representation of a Guardian Enrollment
 */
public interface Enrollment {

    /**
     * The Guardian enrollment id
     *
     * @return the id
     */
    String getId();

    /**
     * The Guardian server url
     *
     * @return the url
     */
    String getUrl();

    /**
     * A label for the enrollment. This will be pre-filled with the Guardian tenant, but since it's
     * only used to display to the user, this could be overriden to return whatever the developer
     * wants.
     *
     * @return the label
     */
    String getLabel();

    /**
     * The name or email for this enrollment's user
     *
     * @return the user name or email
     */
    String getUser();

    /**
     * The TOTP period
     *
     * @return the period, in seconds
     */
    int getPeriod();

    /**
     * The TOTP digits, i.e. the code length
     *
     * @return the amount of digits of the code
     */
    int getDigits();

    /**
     * The TOTP algorithm
     *
     * @return the algorithm name
     */
    String getAlgorithm(); // maybe we can leave this out if we will always use the default

    /**
     * The TOTP secret, Base32 encoded
     *
     * @return the encoded secret
     */
    String getSecret();

    /**
     * The identifier of the physical device, for debug/tracking purposes.
     * Usually will be {@link Settings.Secure#ANDROID_ID}
     *
     * @return a unique device identifier
     */
    String getDeviceIdentifier();

    /**
     * The name to display whenever it is necessary to identify this specific enrollment.
     * For example when the user has to choose where to send the push notification, or at the admin
     * interface if the user wants to delete an enrollment from there
     *
     * @return the name
     */
    String getDeviceName();

    /**
     * The GCM token for this physical device, required to check against the current token and
     * update the server in case it's not the same.
     * Needs to be up-to-data for the push notifications to work.
     *
     * @return the GCM token
     */
    String getGCMToken();

    /**
     * The token used to authenticate when updating the device data or deleting the enrollment
     *
     * @return the Guardian token
     */
    String getDeviceToken();
}
