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

package com.auth0.android.guardian.sdk.otp.utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class Base32Test {

    // test vector from https://tools.ietf.org/html/rfc4648#section-10
    // we don't add padding, so modified accordingly
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {new byte[]{}                       , ""},
                {new byte[]{'f'}                    , "MY"},
                {new byte[]{'f','o'}                , "MZXQ"},
                {new byte[]{'f','o','o'}            , "MZXW6"},
                {new byte[]{'f','o','o','b'}        , "MZXW6YQ"},
                {new byte[]{'f','o','o','b','a'}    , "MZXW6YTB"},
                {new byte[]{'f','o','o','b','a','r'}, "MZXW6YTBOI"}
        });
    }

    private byte[] decoded;
    private String encoded;

    public Base32Test(byte[] decoded, String encoded) {
        this.decoded = decoded;
        this.encoded = encoded;
    }

    @Test
    public void testEncode() throws Exception {
        assertThat(Base32.encode(decoded), is(equalTo(encoded)));
    }

    @Test
    public void testDecode() throws Exception {
        assertThat(Base32.decode(encoded), is(equalTo(decoded)));
    }
}