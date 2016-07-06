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

public interface GuardianEnrollment {

    // we'll need to generate something so we can match push notifications with the enrollment
    // or we could use the device id since this will be for single tenants?
    // anyway, we only need to be sure we can obtain the same from the notification
    String getId();

    // Guardian server url (just in case)
    String getUrl();

    // Issuer/tenant (just in case)
    String getTenant();

    // User name/email
    String getUser();

    // TOTP data, all data we require to generate the code
    // maybe create a class for this? so we have everything in one place?
    int getPeriod(); // maybe we can leave this out if we will always use the default
    int getDigits(); // maybe we can leave this out if we will always use the default
    String getAlgorithm(); // maybe we can leave this out if we will always use the default
    String getSecret(); // base32 encoded secret, as it is on the QR

    //
    // Data from Device class (API client) includes id, name, localIdentifier and gcmToken
    //

    /**
     * This is the actual id of the enrollment on guardian server
     */
    String getDeviceId();

    /**
     * The identifier of the physical device, for debug/tracking purposes
     */
    String getDeviceLocalIdentifier();

    /**
     * The name to display to the user whenever it has to choose where to send the push notification
     * or at the admin interface for example if the user want's to delete one enrollment
     */
    String getDeviceName();

    /**
     * The GCM token for this physical device, required to check against the current token and
     * update in case it's not the same. Needs to be up-to-data for the push notifications to work.
     */
    String getGCMToken();

    /**
     * The token used to authenticate when updating the device data or deleting the enrollment
     */
    String getDeviceToken();
}
