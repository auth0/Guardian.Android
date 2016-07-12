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

package com.auth0.android.guardian.sdk.otp;

import java.util.Date;

/**
 * This class generates One-Time-Passwords following the TOTP algorithm. It is an extension of the
 * {@link HOTP} algorithm.
 *
 * See <a href="https://tools.ietf.org/html/rfc6238">https://tools.ietf.org/html/rfc6238</a> for more information.
 */
public class TOTP {

    private HOTP hotp;
    private long period;

    /**
     * TOTP class constructor
     *
     * @param algorithm the crypto algorithm to use. See {@link HOTP} for a list of supported values
     * @param secret the seed for the crypto algorithm, as a byte array
     * @param digits the number of digits that the generated codes should have
     * @param period the time period (in seconds) used to obtain the counter value
     */
    public TOTP(String algorithm, byte[] secret, int digits, long period) {
        this.hotp = new HOTP(algorithm, secret, digits);
        this.period = period;
    }

    /**
     * Generates the code corresponding to the current date and time
     *
     * @return the TOTP code as a string
     */
    public String generateOTP() {
        long timeSecs = System.currentTimeMillis() / 1000;
        return hotp.generateOTP(timeSecs / period);
    }

    /**
     * Generates the code corresponding to an specific date and time
     *
     * @param date the date/time used to generate the TOTP code
     * @return the TOTP code as a string
     */
    public String generateOTP(Date date) {
        long timeSecs = date.getTime() / 1000;
        return hotp.generateOTP(timeSecs / period);
    }
}
