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

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * This class generates One-Time-Passwords following the TOTP algorithm. It is an extension of the
 * HOTP algorithm.
 * <p>
 * The supported crypto algorithms are 'sha1', 'sha256' and 'sha512'.
 * <p>
 * The code is based on the sample implementation provided in the spec.
 * <p>
 * See <a href=https://tools.ietf.org/html/rfc4226>https://tools.ietf.org/html/rfc4226</a> and
 * <a href="https://tools.ietf.org/html/rfc6238">https://tools.ietf.org/html/rfc6238</a> for more
 * information.
 */
public class TOTP {

    private static final String HMAC_SHA1 = "HmacSHA1";
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String HMAC_SHA512 = "HmacSHA512";

    private static final int[] DIGITS_POWER
            // 0  1   2    3     4      5       6        7         8
            = {1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000};

    private final int digits;
    private final int period;
    private final SystemClock clock;
    private final Mac hmac;

    /**
     * TOTP class constructor
     *
     * @param algorithm the crypto algorithm to use.
     *                  Supported values are 'sha1', 'sha256' and 'sha512'
     * @param secret    the seed for the crypto algorithm, as a byte array
     * @param digits    the number of digits that the generated codes should have
     * @param period    the time period (in seconds) used to obtain the counter value
     */
    public TOTP(String algorithm, byte[] secret, int digits, int period) {
        this(algorithm, secret, digits, period, new SystemClock());
    }

    TOTP(String algorithm, byte[] secret, int digits, int period, SystemClock clock) {
        try {
            switch (algorithm.toLowerCase()) {
                case "sha1":
                    hmac = Mac.getInstance(HMAC_SHA1);
                    break;
                case "sha256":
                    hmac = Mac.getInstance(HMAC_SHA256);
                    break;
                case "sha512":
                    hmac = Mac.getInstance(HMAC_SHA512);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
            }
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("The specified crypto algorithm is not available", e);
        }

        try {
            SecretKeySpec macKey = new SecretKeySpec(secret, "RAW");
            hmac.init(macKey);
        } catch (InvalidKeyException e) {
            throw new IllegalArgumentException("The key/secret is not valid", e);
        }

        if (digits >= DIGITS_POWER.length) {
            throw new IllegalArgumentException(
                    "Unsupported amount of digits. It should not exceed 8 (was: " + digits + ")");
        }
        this.digits = digits;

        this.period = period;

        this.clock = clock;
    }

    /**
     * Generates the code corresponding to the current date and time
     *
     * @return the OTP code as a string
     */
    public String generate() {
        long timeSecs = clock.getCurrentTimeSecs();
        return generate(timeSecs / period);
    }

    /**
     * Generates the code for the specified counter value
     *
     * @param eventCount the counter value
     * @return the OTP code as a string
     */
    public String generate(long eventCount) {
        // convert to byte array
        long movingFactor = eventCount;
        byte[] counter = new byte[8]; // 64 bits
        for (int i = counter.length - 1; i >= 0; i--) {
            counter[i] = (byte) (movingFactor & 0xff);
            movingFactor >>= 8;
        }

        // This method uses the JCE to provide the crypto.
        // HMAC computes a Hashed Message Authentication Code with the crypto hash as a parameter.
        byte[] hash = hmac.doFinal(counter);

        // put selected bytes into result int
        int offset = hash[hash.length - 1] & 0xf;

        int binary =
                ((hash[offset] & 0x7f) << 24) |
                        ((hash[offset + 1] & 0xff) << 16) |
                        ((hash[offset + 2] & 0xff) << 8) |
                        (hash[offset + 3] & 0xff);

        int otp = binary % DIGITS_POWER[digits];

        String result = Integer.toString(otp);

        // padding with zeros to complete code length
        while (result.length() < digits) {
            result = "0" + result;
        }

        return result;
    }

    static class SystemClock {

        public long getCurrentTimeSecs() {
            return System.currentTimeMillis() / 1000;
        }
    }
}
