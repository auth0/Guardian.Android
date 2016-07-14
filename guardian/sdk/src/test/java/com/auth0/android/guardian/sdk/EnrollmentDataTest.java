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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 18, manifest = Config.NONE)
public class EnrollmentDataTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void shouldFailWithInvalidScheme() throws Exception {
        thrown.expect(IllegalArgumentException.class);

        EnrollmentData shouldBeNull = EnrollmentData.parse("invalidScheme://totp/issuer:user@email.com?secret=secret&issuer=issuer&enrollment_tx_id=enrollment_tx_id&id=device_account_id&algorithm=SHA1&digits=6&period=30&base_url=https://tenant.pig.com");
    }

    @Test
    public void shouldFailWithInvalidAuthority() throws Exception {
        thrown.expect(IllegalArgumentException.class);

        EnrollmentData shouldBeNull = EnrollmentData.parse("otpauth://invalidAuthority/issuer:user@email.com?secret=secret&issuer=issuer&enrollment_tx_id=enrollment_tx_id&id=device_account_id&algorithm=SHA1&digits=6&period=30&base_url=https://tenant.pig.com");
    }

    @Test
    public void shouldFailWithInvalidIssuer() throws Exception {
        thrown.expect(IllegalArgumentException.class);

        EnrollmentData shouldBeNull = EnrollmentData.parse("otpauth://totp/tenant:user@email.com?secret=secret&issuer=issuer&enrollment_tx_id=enrollment_tx_id&id=device_account_id&algorithm=SHA1&digits=6&period=30&base_url=https://tenant.pig.com");
    }

    @Test
    public void shouldFailWithInvalidDigits() throws Exception {
        thrown.expect(IllegalArgumentException.class);

        EnrollmentData shouldBeNull = EnrollmentData.parse("otpauth://totp/issuer:user@email.com?secret=secret&issuer=issuer&enrollment_tx_id=enrollment_tx_id&id=device_account_id&algorithm=SHA1&digits=six&period=30&base_url=https://tenant.pig.com");
    }

    @Test
    public void shouldFailWithInvalidDigitsFloat() throws Exception {
        thrown.expect(IllegalArgumentException.class);

        EnrollmentData shouldBeNull = EnrollmentData.parse("otpauth://totp/issuer:user@email.com?secret=secret&issuer=issuer&enrollment_tx_id=enrollment_tx_id&id=device_account_id&algorithm=SHA1&digits=6.1&period=30&base_url=https://tenant.pig.com");
    }

    @Test
    public void shouldFailWithInvalidPeriod() throws Exception {
        thrown.expect(IllegalArgumentException.class);

        EnrollmentData shouldBeNull = EnrollmentData.parse("otpauth://totp/issuer:user@email.com?secret=secret&issuer=issuer&enrollment_tx_id=enrollment_tx_id&id=device_account_id&algorithm=SHA1&digits=6&period=thirty&base_url=https://tenant.pig.com");
    }

    @Test
    public void shouldFailWithInvalidPeriodFloat() throws Exception {
        thrown.expect(IllegalArgumentException.class);

        EnrollmentData shouldBeNull = EnrollmentData.parse("otpauth://totp/issuer:user@email.com?secret=secret&issuer=issuer&enrollment_tx_id=enrollment_tx_id&id=device_account_id&algorithm=SHA1&digits=6&period=30.1&base_url=https://tenant.pig.com");
    }

    @Test
    public void shouldFailWithNoLabel() throws Exception {
        thrown.expect(IllegalArgumentException.class);

        EnrollmentData shouldBeNull = EnrollmentData.parse("otpauth://totp/?secret=secret&issuer=issuer&enrollment_tx_id=enrollment_tx_id&id=device_account_id&algorithm=SHA1&digits=6&period=30&base_url=https://tenant.pig.com");
    }

    @Test
    public void shouldFailWithNoSecret() throws Exception {
        thrown.expect(IllegalArgumentException.class);

        EnrollmentData shouldBeNull = EnrollmentData.parse("otpauth://totp/issuer:user@email.com?issuer=issuer&enrollment_tx_id=enrollment_tx_id&id=device_account_id&algorithm=SHA1&digits=6&period=30&base_url=https://tenant.pig.com");
    }

    @Test
    public void shouldReturnValidData() throws Exception {
        EnrollmentData data = EnrollmentData.parse("otpauth://totp/issuer:user@email.com?secret=secret&issuer=issuer&enrollment_tx_id=enrollment_tx_id&id=device_account_id&algorithm=SHA1&digits=6&period=30&base_url=https://tenant.pig.com");

        assertThat(data, is(not(nullValue())));
        assertThat(data.getUser(), is(equalTo("user@email.com")));
        assertThat(data.getSecret(), is(equalTo("secret")));
        assertThat(data.getIssuer(), is(equalTo("issuer")));
        assertThat(data.getEnrollmentTransactionId(), is(equalTo("enrollment_tx_id")));
        assertThat(data.getDeviceId(), is(equalTo("device_account_id")));
        assertThat(data.getAlgorithm(), is(equalTo("SHA1")));
        assertThat(data.getDigits(), is(equalTo(6)));
        assertThat(data.getPeriod(), is(equalTo(30)));
        assertThat(data.getBaseUrl(), is(equalTo("https://tenant.pig.com")));
    }

    @Test
    public void shouldReturnValidDataWithoutIssuer() throws Exception {
        EnrollmentData data = EnrollmentData.parse("otpauth://totp/issuer:user@email.com?secret=secret&enrollment_tx_id=enrollment_tx_id&id=device_account_id&algorithm=SHA1&digits=6&period=30&base_url=https://tenant.pig.com");

        assertThat(data, is(not(nullValue())));
        assertThat(data.getUser(), is(equalTo("user@email.com")));
        assertThat(data.getSecret(), is(equalTo("secret")));
        assertThat(data.getIssuer(), is(equalTo("issuer")));
        assertThat(data.getEnrollmentTransactionId(), is(equalTo("enrollment_tx_id")));
        assertThat(data.getDeviceId(), is(equalTo("device_account_id")));
        assertThat(data.getAlgorithm(), is(equalTo("SHA1")));
        assertThat(data.getDigits(), is(equalTo(6)));
        assertThat(data.getPeriod(), is(equalTo(30)));
        assertThat(data.getBaseUrl(), is(equalTo("https://tenant.pig.com")));
    }

    @Test
    public void shouldReturnValidDataWithoutIssuerPrefix() throws Exception {
        EnrollmentData data = EnrollmentData.parse("otpauth://totp/user@email.com?secret=secret&issuer=issuer&enrollment_tx_id=enrollment_tx_id&id=device_account_id&algorithm=SHA1&digits=6&period=30&base_url=https://tenant.pig.com");

        assertThat(data, is(not(nullValue())));
        assertThat(data.getUser(), is(equalTo("user@email.com")));
        assertThat(data.getSecret(), is(equalTo("secret")));
        assertThat(data.getIssuer(), is(equalTo("issuer")));
        assertThat(data.getEnrollmentTransactionId(), is(equalTo("enrollment_tx_id")));
        assertThat(data.getDeviceId(), is(equalTo("device_account_id")));
        assertThat(data.getAlgorithm(), is(equalTo("SHA1")));
        assertThat(data.getDigits(), is(equalTo(6)));
        assertThat(data.getPeriod(), is(equalTo(30)));
        assertThat(data.getBaseUrl(), is(equalTo("https://tenant.pig.com")));
    }

    @Test
    public void shouldReturnDefaultDigits() throws Exception {
        EnrollmentData data = EnrollmentData.parse("otpauth://totp/issuer:user@email.com?secret=secret&issuer=issuer&enrollment_tx_id=enrollment_tx_id&id=device_account_id&algorithm=SHA256&period=31&base_url=https://tenant.pig.com");

        assertThat(data, is(not(nullValue())));
        assertThat(data.getUser(), is(equalTo("user@email.com")));
        assertThat(data.getSecret(), is(equalTo("secret")));
        assertThat(data.getIssuer(), is(equalTo("issuer")));
        assertThat(data.getEnrollmentTransactionId(), is(equalTo("enrollment_tx_id")));
        assertThat(data.getDeviceId(), is(equalTo("device_account_id")));
        assertThat(data.getAlgorithm(), is(equalTo("SHA256")));
        assertThat(data.getDigits(), is(equalTo(6)));
        assertThat(data.getPeriod(), is(equalTo(31)));
        assertThat(data.getBaseUrl(), is(equalTo("https://tenant.pig.com")));
    }

    @Test
    public void shouldReturnDefaultPeriod() throws Exception {
        EnrollmentData data = EnrollmentData.parse("otpauth://totp/issuer:user@email.com?secret=secret&issuer=issuer&enrollment_tx_id=enrollment_tx_id&id=device_account_id&algorithm=SHA512&digits=8&base_url=https://tenant.pig.com");

        assertThat(data, is(not(nullValue())));
        assertThat(data.getUser(), is(equalTo("user@email.com")));
        assertThat(data.getSecret(), is(equalTo("secret")));
        assertThat(data.getIssuer(), is(equalTo("issuer")));
        assertThat(data.getEnrollmentTransactionId(), is(equalTo("enrollment_tx_id")));
        assertThat(data.getDeviceId(), is(equalTo("device_account_id")));
        assertThat(data.getAlgorithm(), is(equalTo("SHA512")));
        assertThat(data.getDigits(), is(equalTo(8)));
        assertThat(data.getPeriod(), is(equalTo(30)));
        assertThat(data.getBaseUrl(), is(equalTo("https://tenant.pig.com")));
    }

    @Test
    public void shouldReturnDefaultAlgorithm() throws Exception {
        EnrollmentData data = EnrollmentData.parse("otpauth://totp/issuer:user@email.com?secret=secret&issuer=issuer&enrollment_tx_id=enrollment_tx_id&id=device_account_id&digits=7&period=33&base_url=https://tenant.pig.com");

        assertThat(data, is(not(nullValue())));
        assertThat(data.getUser(), is(equalTo("user@email.com")));
        assertThat(data.getSecret(), is(equalTo("secret")));
        assertThat(data.getIssuer(), is(equalTo("issuer")));
        assertThat(data.getEnrollmentTransactionId(), is(equalTo("enrollment_tx_id")));
        assertThat(data.getDeviceId(), is(equalTo("device_account_id")));
        assertThat(data.getAlgorithm(), is(equalTo("SHA1")));
        assertThat(data.getDigits(), is(equalTo(7)));
        assertThat(data.getPeriod(), is(equalTo(33)));
        assertThat(data.getBaseUrl(), is(equalTo("https://tenant.pig.com")));
    }
}