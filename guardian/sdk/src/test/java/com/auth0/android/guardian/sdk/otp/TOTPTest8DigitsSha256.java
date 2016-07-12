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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class TOTPTest8DigitsSha256 {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {59L         , "46119246"},
                {1111111109L , "68084774"},
                {1111111111L , "67062674"},
                {1234567890L , "91819424"},
                {2000000000L , "90698825"},
                {20000000000L, "77737706"}
        });
    }

    private long period = 30;
    private int digits = 8;

    // Seed for HMAC-SHA256 - 32 bytes
    String secret = "3132333435363738393031323334353637383930313233343536373839303132";

    private TOTP totp = new TOTP("sha256", Utils.hexStr2Bytes(secret), digits, period);

    private long timeSecs;
    private String expected;

    public TOTPTest8DigitsSha256(long timeSecs, String expected) {
        this.timeSecs = timeSecs;
        this.expected = expected;
    }

    @Test
    public void test() {
        Date date = new Date(timeSecs * 1000);
        assertThat(expected, is(equalTo(totp.generateOTP(date))));
    }
}
