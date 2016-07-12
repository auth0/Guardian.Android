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

package com.auth0.guardian.sample;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.auth0.android.guardian.sdk.Guardian;
import com.auth0.android.guardian.sdk.ParcelableNotification;

public class GuardianUI {

    public static final String ENROLLMENT = "com.auth0.guardian.sample.GuardianUI.ENROLLMENT";

    private final Guardian guardian;

    GuardianUI(Guardian guardian) {
        this.guardian = guardian;
    }

    public static GuardianUI from(@NonNull Guardian guardian) {
        return new GuardianUI(guardian);
    }

    public void showEnroll(@NonNull Activity activity,
                           @NonNull String deviceName,
                           @NonNull String gcmToken,
                           int requestCode) {
        Intent enrollIntent = EnrollActivity
                .getStartIntent(activity, guardian, deviceName, gcmToken);
        activity.startActivityForResult(enrollIntent, requestCode);
    }

    public void showNotification(@NonNull Activity activity,
                                 @NonNull ParcelableNotification notification,
                                 @NonNull MyEnrollment enrollment) {
        Intent intent = NotificationActivity
                .getStartIntent(activity, guardian, notification, enrollment);
        activity.startActivity(intent);
    }
}
