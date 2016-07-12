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

import java.lang.reflect.UndeclaredThrowableException;
import java.security.GeneralSecurityException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * This class generates One-Time-Passwords following the HOTP algorithm. The supported crypto
 * algorithms are 'sha1', 'sha256' and 'sha512'.
 *
 * The code is based on the sample implementation provided in the spec.
 *
 * See <a href=https://tools.ietf.org/html/rfc4226>https://tools.ietf.org/html/rfc4226</a> for more information.
 */
public class HOTP {

    private static final String HMAC_SHA1   = "HmacSHA1";
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String HMAC_SHA512 = "HmacSHA512";

    private static final int[] DIGITS_POWER
            // 0  1   2    3     4      5       6        7         8
            = {1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000};

    private String crypto;
    private byte[] key;
    private int digits;


    /**
     * HOTP class constructor
     *
     * @param algorithm the crypto algorithm to use.
     *                  Supported values are 'sha1', 'sha256' and 'sha512'
     * @param secret the seed for the crypto algorithm, as a byte array
     * @param digits the number of digits that the generated codes should have
     */
    public HOTP(String algorithm, byte[] secret, int digits) {
        this.key = secret;

        if (digits >= DIGITS_POWER.length) {
            throw new IllegalArgumentException("Unsupported amount of digits. It should not exceed 8 (was: "+ digits +")");
        }
        this.digits = digits;

        switch (algorithm.toLowerCase()) {
            case "sha1":
                this.crypto = HMAC_SHA1;
                break;
            case "sha256":
                this.crypto = HMAC_SHA256;
                break;
            case "sha512":
                this.crypto = HMAC_SHA512;
                break;
            default:
                throw new IllegalArgumentException("Unsupported algorithm: "+ algorithm);
        }
    }

    /**
     * Generates the code for the specified counter value
     *
     * @param eventCount the counter value
     * @return the code as a String
     */
    public String generateOTP(long eventCount) {
        // convert to byte array
        long movingFactor = eventCount;
        byte[] counter = new byte[8]; // 64 bits
        for (int i = counter.length - 1; i >= 0; i--) {
            counter[i] = (byte) (movingFactor & 0xff);
            movingFactor >>= 8;
        }

        byte[] hash = hmac_sha(crypto, key, counter);

        // put selected bytes into result int
        int offset = hash[hash.length - 1] & 0xf;

        int binary =
                ((hash[offset    ] & 0x7f) << 24) |
                ((hash[offset + 1] & 0xff) << 16) |
                ((hash[offset + 2] & 0xff) <<  8) |
                 (hash[offset + 3] & 0xff);

        int otp = binary % DIGITS_POWER[digits];

        String result = Integer.toString(otp);

        // padding with zeros to complete code length
        while (result.length() < digits) {
            result = "0" + result;
        }

        return result;
    }

    /**
     * This method uses the JCE to provide the crypto. HMAC computes a Hashed Message Authentication
     * Code with the crypto hash as a parameter.
     *
     * @param crypto the crypto algorithm to use (HmacSHA1, HmacSHA256, HmacSHA512)
     * @param keyBytes the bytes to use for the HMAC key
     * @param text the message or text to be authenticated
     */
    private static byte[] hmac_sha(String crypto,
                                   byte[] keyBytes,
                                   byte[] text) {
        try {
            Mac hmac;
            hmac = Mac.getInstance(crypto);
            SecretKeySpec macKey = new SecretKeySpec(keyBytes, "RAW");
            hmac.init(macKey);
            return hmac.doFinal(text);
        } catch (GeneralSecurityException gse) {
            throw new UndeclaredThrowableException(gse);
        }
    }
}
