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

package com.auth0.guardian.sdk;

import java.util.Date;

/**
 * Contains information that the Guardian server sends in the push notification.
 * It works as a challenge since it contains a unique id that must be sent in order to allow or
 * reject an authentication request. It also contains an identifier for the account it belongs.
 * Additionally it contains useful information about the source of the request:
 * <ul>
 *     <li>Browser/OS
 *     <li>Date and time
 *     <li>Approximate location
 * </ul>
 * Instances of this class are only created by {@link Guardian#parsePushNotification}
 *
 * @author Nicolas Ulrich (nikolaseu@gmail.com)
 * @see Guardian#parsePushNotification
 * @see Guardian#allowLogin
 * @see Guardian#rejectLogin
 */
public class AuthChallenge {

    /**
     * The challenge that must be sent when allowing or rejecting an authentication request.
     * Can be seen as an unique id for the authentication request.
     *
     * AKA transaction token
     */
    private String transactionToken;

    /**
     * The account id
     *
     * @see Account#id
     */
    private String accountId;

    /**
     * The start date for the authentication request
     */
    private Date date;

    /**
     * The operating system where the request was initiated, if available
     */
    private String osName;

    /**
     * The version of the operating system where the request was initiated, if available
     */
    private String osVersion;

    /**
     * The browser where the request was initiated, if available
     */
    private String browserName;

    /**
     * The version of the browser where the request was initiated, if available
     */
    private String browserVersion;

    /**
     * The approximate location where the request was initiated, if available
     */
    private String location;

    /**
     * The approximate latitude of the location where the request was initiated, if available
     */
    private String latitude;

    /**
     * The approximate longitude of the location where the request was initiated, if available
     */
    private String longitude;
}
