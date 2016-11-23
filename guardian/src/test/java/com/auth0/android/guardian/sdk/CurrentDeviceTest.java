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

import android.content.Context;
import android.provider.Settings;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 23, manifest = Config.NONE)
public class CurrentDeviceTest {

    private static final String NOTIFICATION_TOKEN = "NOTIFICATION_TOKEN";
    private static final String DEVICE_NAME = "DEVICE_NAME";
    private static final String DEVICE_IDENTIFIER = "DEVICE_IDENTIFIER";

    @Test
    public void shouldHaveCorrectDataWithExplicitConstructor() throws Exception {
        CurrentDevice currentDevice = new CurrentDevice(NOTIFICATION_TOKEN, DEVICE_NAME, DEVICE_IDENTIFIER);

        assertThat(currentDevice.getNotificationToken(), is(equalTo(NOTIFICATION_TOKEN)));
        assertThat(currentDevice.getName(), is(equalTo(DEVICE_NAME)));
        assertThat(currentDevice.getIdentifier(), is(equalTo(DEVICE_IDENTIFIER)));
    }

    @Test
    public void shouldHaveCorrectDataDefaultIdentifier() throws Exception {
        Context context = RuntimeEnvironment.application;

        Settings.Secure.putString(
                context.getContentResolver(), Settings.Secure.ANDROID_ID, DEVICE_IDENTIFIER);

        CurrentDevice currentDevice = new CurrentDevice(
                context, NOTIFICATION_TOKEN, DEVICE_NAME);

        assertThat(currentDevice.getNotificationToken(), is(equalTo(NOTIFICATION_TOKEN)));
        assertThat(currentDevice.getName(), is(equalTo(DEVICE_NAME)));
        assertThat(currentDevice.getIdentifier(), is(equalTo(DEVICE_IDENTIFIER)));
    }
}