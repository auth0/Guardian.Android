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

import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.auth0.android.guardian.sdk.networking.Request;
import com.auth0.android.guardian.sdk.networking.RequestFactory;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import okhttp3.HttpUrl;

public class DeviceAPIClient {

    private final RequestFactory requestFactory;
    private final HttpUrl url;
    private final String token;

    DeviceAPIClient(RequestFactory requestFactory, HttpUrl baseUrl, String id, String token) {
        this.requestFactory = requestFactory;
        this.url = baseUrl.newBuilder()
                .addPathSegments("api/device-accounts")
                .addPathSegment(id)
                .build();
        this.token = token;
    }

    /**
     * Deletes the device, i.e. unenrolls the device from the account
     *
     * @return a request to execute
     */
    public GuardianAPIRequest<Void> delete() {
        Type type = new TypeToken<Void>() {}.getType();
        return requestFactory
                .<Void>newRequest("DELETE", url, type)
                .setBearer(token);
    }

    /**
     * Creates a device with the specified name and GCM token
     * The response {@link Device} will have all the final values currently at the Guardian server.
     *
     * @param name the name of the android device that will be displayed to the user in Guardian
     * @param gcmToken the GCM token used to send notifications to the android device
     * @return a request to execute
     */
    public GuardianAPIRequest<Device> create(@NonNull String name, @NonNull String gcmToken) {
        return newEmptyUpdateRequest()
                .setParameter("identifier", Settings.Secure.ANDROID_ID)
                .setParameter("name", name)
                .setParameter("push_credentials", createPushCredentials(gcmToken));
    }

    /**
     * Updates identifier, name and GCM token of the device.
     * Any parameter can be null and will not be changed at the server.
     * The response {@link Device} will have all the final values currently at the Guardian server.
     *
     * @param identifier the local identifier that uniquely identifies the android device
     * @param name the name of the android device that will be displayed to the user in Guardian
     * @param gcmToken the GCM token used to send notifications to the android device
     * @return a request to execute
     */
    public GuardianAPIRequest<Device> update(@Nullable String identifier,
                                             @Nullable String name,
                                             @Nullable String gcmToken) {
        return newEmptyUpdateRequest()
                .setParameter("identifier", identifier)
                .setParameter("name", name)
                .setParameter("push_credentials", createPushCredentials(gcmToken));
    }

    private Request<Device> newEmptyUpdateRequest() {
        Type type = new TypeToken<Device>() {}.getType();
        return requestFactory.<Device>newRequest("PATCH", url, type)
                .setBearer(token);
    }

    private static Map<String, String> createPushCredentials(@Nullable String gcmToken) {
        if (gcmToken == null) {
            return null;
        }

        Map<String, String> pushCredentials = new HashMap<>(2);
        pushCredentials.put("service", "GCM");
        pushCredentials.put("token", gcmToken);
        return pushCredentials;
    }
}
