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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TOTPTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private int period = 30;

    // Seed for HMAC-SHA1 - 20 bytes
    private String secret = "3132333435363738393031323334353637383930";

    @Test
    public void shouldFailWithMoreThanEightDigits() throws Exception {
        thrown.expect(IllegalArgumentException.class);

        new TOTP("sha1", Utils.hexStr2Bytes(secret), 9, period);
    }

    @Test
    public void shouldFailWithUnknownAlgorithm() throws Exception {
        thrown.expect(IllegalArgumentException.class);

        new TOTP("sha111", Utils.hexStr2Bytes(secret), 8, period);
    }

    @Test
    public void shouldFailWithInvalidSecret() throws Exception {
        thrown.expect(IllegalArgumentException.class);

        new TOTP("sha256", null, 8, period);
    }
}