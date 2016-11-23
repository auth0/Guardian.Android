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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Date;

/**
 * A Guardian Notification contains data about an authentication request.
 */
public interface Notification {

    /**
     * The id of the enrollment
     */
    @NonNull
    String getEnrollmentId();

    /**
     * The transaction token, used to identify the authentication request
     */
    @NonNull
    String getTransactionToken();

    /**
     * The Guardian server url
     */
    @NonNull
    String getUrl();

    /**
     * The date/time when the authentication request was initiated
     */
    @NonNull
    Date getDate();

    /**
     * The name of the operating system where the authentication request was initiated
     */
    @Nullable
    String getOsName();

    /**
     * The version of the operating system
     */
    @Nullable
    String getOsVersion();

    /**
     * The name of the browser where the authentication request was initiated
     */
    @Nullable
    String getBrowserName();

    /**
     * The version of the browser
     */
    @Nullable
    String getBrowserVersion();

    /**
     * The name of the (approximate) location where the authentication request was initiated
     */
    @Nullable
    String getLocation();

    /**
     * The latitude of the (approximate) location
     */
    @Nullable
    Double getLatitude();

    /**
     * The longitude of the (approximate) location
     */
    @Nullable
    Double getLongitude();

    /**
     * The challenge sent by the server. The same challenge should be sent back when trying to
     * allow or reject an authentication request
     */
    @NonNull
    String getChallenge();
}
