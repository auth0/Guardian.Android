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

import java.util.Date;

public interface GuardianNotification {

    /**
     * The id of the enrollment
     */
    String getEnrollmentId();

    /**
     * The transaction token, used to identify the authentication request
     */
    String getTransactionToken();

    /**
     * The Guardian server url
     */
    String getUrl();

    /**
     * The date/time when the authentication request was initiated
     */
    Date getDate();

    /**
     * The name of the operating system where the authentication request was initiated
     */
    String getOsName();

    /**
     * The version of the operating system
     */
    String getOsVersion();

    /**
     * The name of the browser where the authentication request was initiated
     */
    String getBrowserName();

    /**
     * The version of the browser
     */
    String getBrowserVersion();

    /**
     * The name of the (approximate) location where the authentication request was initiated
     */
    String getLocation();

    /**
     * The latitude of the (approximate) location
     */
    Double getLatitude();

    /**
     * The longitude of the (approximate) location
     */
    Double getLongitude();
}
