/*
 * Copyright (c) 2026 Auth0 (http://auth0.com)
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

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Persists the sample's single enrollment. Shared by {@link MainActivity} and
 * {@link EnrollActivity}, so an enrollment started on its own (e.g. through the
 * {@code ENROLLMENT_DATA} test hook) is still known to the app afterwards.
 */
final class EnrollmentStorage {

    private EnrollmentStorage() {
    }

    // Same file android.preference.PreferenceManager used, so existing enrollments survive.
    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(
                context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
    }

    @Nullable
    static ParcelableEnrollment load(@NonNull Context context) {
        String json = preferences(context).getString(Constants.ENROLLMENT, null);
        return json != null ? ParcelableEnrollment.fromJSON(json) : null;
    }

    static void save(@NonNull Context context, @Nullable ParcelableEnrollment enrollment) {
        preferences(context).edit()
                .putString(Constants.ENROLLMENT, enrollment != null ? enrollment.toJSON() : null)
                .apply();
    }
}
