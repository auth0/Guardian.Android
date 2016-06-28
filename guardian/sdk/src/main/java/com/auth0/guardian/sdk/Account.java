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

/**
 * Represents a Guardian Account, and is the result of a successful call to {@link Guardian#enroll}.
 * <p>
 * It contains all the information required to generate the OTP code and some details about the user
 * and the tenant it belongs.
 * We also find here the information and tokens required in order to update or unenroll (delete) the
 * account.
 * <p>
 * Finally this also contains the id required to identify the account, for example from a given
 * {@link AuthChallenge}.
 *
 * @author Nicolas Ulrich (nikolaseu@gmail.com)
 * @see Guardian#enroll
 * @see Guardian#unenroll
 * @see Guardian#allowLogin
 * @see Guardian#rejectLogin
 */
public class Account {

    /**
     * The account id
     */
    private String id;

    /**
     * TOTP period
     */
    private int period;

    /**
     * TOTP digits
     */
    private int digits;

    /**
     * TOTP algorithm
     */
    private String algorithm;

    /**
     * TOTP secret
     */
    private String secret;

    /**
     * Issuer/tenant
     */
    private String issuer;

    /**
     * User name/email
     */
    private String user;

    /**
     * Server side id, used to update or delete the account on the server
     */
    private String deviceId;

    /**
     * Token used to authenticate when updating or deleting the account from the server
     */
    private String deviceToken;

    /**
     * The identifier of the service used to send push notifications
     */
    private String pushService;

    /**
     * The token used to send push notifications to this device
     */
    private String pushToken;
}
