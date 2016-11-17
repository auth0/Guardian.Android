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

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;

/**
 * Data about the current device, including name, identifier and push service's notification token
 */
public class CurrentDevice {

    private final String name;
    private final String identifier;
    private final String notificationToken;

    /**
     * Constructs an instance using the specified values for the device name, identifier and
     * notification service's token.
     *
     * @param notificationToken the push notification service's token
     * @param name              the name to display whenever it is necessary to identify this
     *                          specific device.
     * @param identifier        an identifier that uniquely identifies this android device, for
     *                          debug/tracking purposes.
     */
    public CurrentDevice(String notificationToken, String name, String identifier) {
        this.notificationToken = notificationToken;
        this.name = name;
        this.identifier = identifier;
    }

    /**
     * Constructs an instance using a default values for this device's identifier.
     *
     * Uses the value of {@link Settings.Secure#ANDROID_ID} as the device identifier.
     * This identifier consists of a 64bit number (as a hex string) that is randomly generated when
     * the user first sets up the device and should remain constant for the lifetime of the user's
     * device. The value may change if a factory reset is performed on the device.
     *
     * @param context           an android context used to obtain the default values
     * @param notificationToken the push notification service's token
     * @param name              the name to display whenever it is necessary to identify this
     *                          specific device.
     */
    @SuppressLint("HardwareIds")
    public CurrentDevice(Context context, String notificationToken, String name) {
        this(notificationToken, name,
                Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID));
    }

    /**
     * The visible name for this device
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * The identifier for this device
     *
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * The push notification service's token that will be used to send push notifications to this
     * device.
     *
     * @return the notification token
     */
    public String getNotificationToken() {
        return notificationToken;
    }
}
