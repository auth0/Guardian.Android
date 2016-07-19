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
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(Parameterized.class)
public class TOTPTest8DigitsSha1 {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {59L         , "94287082"},
                {1111111109L , "07081804"},
                {1111111111L , "14050471"},
                {1234567890L , "89005924"},
                {2000000000L , "69279037"},
                {20000000000L, "65353130"}
        });
    }

    @Mock
    TOTP.SystemClock clock;

    private int period = 30;
    private int digits = 8;

    // Seed for HMAC-SHA1 - 20 bytes
    private String secret = "3132333435363738393031323334353637383930";

    private TOTP totp;

    private String expected;

    public TOTPTest8DigitsSha1(long timeSecs, String expected) {
        this.expected = expected;

        initMocks(this);

        when(clock.getCurrentTimeSecs())
                .thenReturn(timeSecs);

        totp = new TOTP("sha1", Utils.hexStr2Bytes(secret), digits, period, clock);
    }

    @Test
    public void test() {
        assertThat(expected, is(equalTo(totp.generate())));
    }
}
