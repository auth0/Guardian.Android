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

package com.auth0.guardian.sample.gcm;

import android.content.Context;
import android.os.AsyncTask;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;

public class GcmUtils {

    private final Context context;
    private final String googleAppId;

    public GcmUtils(Context context, String googleAppId) {
        this.context = context;
        this.googleAppId = googleAppId;
    }

    public void fetchGcmToken(GcmTokenListener listener) {
        new FetchAsyncTask(listener)
                .execute();
    }

    public interface GcmTokenListener {
        void onGcmTokenObtained(String gcmToken);

        void onGcmFailure(Throwable exception);
    }

    class FetchAsyncTask extends AsyncTask<Void, Void, Throwable> {

        private final GcmTokenListener listener;
        private String token;

        FetchAsyncTask(GcmTokenListener listener) {
            this.listener = listener;
        }

        @Override
        protected Throwable doInBackground(Void... params) {
            // Initially this call goes out to the network to retrieve the token, subsequent calls
            // are local.
            InstanceID instanceID = InstanceID.getInstance(context);
            try {
                token = instanceID.getToken(googleAppId, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
                return null;
            } catch (IOException e) {
                return e;
            }
        }

        @Override
        protected void onPostExecute(Throwable error) {
            if (error != null) {
                listener.onGcmFailure(error);
            } else {
                listener.onGcmTokenObtained(token);
            }
        }
    }
}
