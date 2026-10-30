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

package com.auth0.guardian.sample

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Persists the sample's single enrollment. Shared by [MainActivity] and [EnrollActivity], so an
 * enrollment started on its own (e.g. through the `ENROLLMENT_DATA` test hook) is still known to
 * the app afterwards.
 */
object EnrollmentStorage {

    // Same file android.preference.PreferenceManager used, so existing enrollments survive.
    private fun preferences(context: Context): SharedPreferences =
        context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)

    @JvmStatic
    fun load(context: Context): ParcelableEnrollment? =
        preferences(context).getString(Constants.ENROLLMENT, null)
            ?.let { ParcelableEnrollment.fromJSON(it) }

    @JvmStatic
    fun save(context: Context, enrollment: ParcelableEnrollment?) {
        preferences(context).edit {
            putString(Constants.ENROLLMENT, enrollment?.toJSON())
        }
    }
}
