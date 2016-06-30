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

import com.auth0.android.guardian.sdk.networking.Callback;
import com.auth0.android.guardian.sdk.networking.Request;
import com.auth0.android.guardian.sdk.networking.RequestFactory;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
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
        Type type = new TypeToken<Void>() {
        }.getType();
        return requestFactory
                .<Void>newRequest("DELETE", url, type)
                .setBearer(token);
    }

    /**
     * Creates a device with the specified name and GCM token
     *
     * @param name the name of the device, used to display to the user so he can identify it
     * @param gcmToken the GCM token
     * @return a request to execute
     */
    public GuardianAPIRequest<Device> create(String name, String gcmToken) {
        return newEmptyUpdateRequest()
                .addParameter("identifier", Settings.Secure.ANDROID_ID)
                .addParameter("name", name)
                .addParameter("push_credentials", getPushCredentials(gcmToken));
    }

    /**
     * Gets a request that allows to update different parameters of the device
     *
     * @return an update request
     */
    public UpdateDeviceRequest update() {
        return new UpdateDeviceRequest(newEmptyUpdateRequest());
    }

    /**
     * Updates identifier, name and GCM token of the device
     *
     * @param identifier the local identifier
     * @param name the name
     * @param gcmToken the GCM token
     * @return a request to execute
     */
    public GuardianAPIRequest<Device> update(String identifier, String name, String gcmToken) {
        return new UpdateDeviceRequest(newEmptyUpdateRequest())
                .localIdentifier(identifier)
                .name(name)
                .GCMToken(gcmToken);
    }

    private Request<Device> newEmptyUpdateRequest() {
        Type type = new TypeToken<Device>() {}.getType();
        return requestFactory.<Device>newRequest("PATCH", url, type)
                .setBearer(token);
    }

    private static Map<String, String> getPushCredentials(String gcmToken) {
        Map<String, String> pushCredentials = new HashMap<>(2);
        pushCredentials.put("service", "GCM");
        pushCredentials.put("token", gcmToken);
        return pushCredentials;
    }

    public static class UpdateDeviceRequest implements GuardianAPIRequest<Device> {

        private final Request<Device> request;

        UpdateDeviceRequest(Request<Device> request) {
            this.request = request;
        }

        /**
         * Updates the name of the device
         *
         * @param name the name of the device, used to display to the user so he can identify it
         * @return a request that allows to update other parameters of the device or execute
         */
        public UpdateDeviceRequest name(String name) {
            request.addParameter("name", name);
            return this;
        }

        /**
         * Updates the local identifier of the device
         *
         * @param identifier the local identifier of the device
         * @return a request that allows to update other parameters of the device or execute
         */
        public UpdateDeviceRequest localIdentifier(String identifier) {
            request.addParameter("identifier", identifier);
            return this;
        }

        /**
         * Updates the GCM token of the device. You should update the token whenever it changes,
         * otherwise the server will not be able to send push notifications
         *
         * @param gcmToken the GCM token
         * @return a request that allows to update other parameters of the device or execute
         */
        public UpdateDeviceRequest GCMToken(String gcmToken) {
            request.addParameter("push_credentials", getPushCredentials(gcmToken));
            return this;
        }

        @Override
        public Device execute() throws IOException, GuardianException {
            return request.execute();
        }

        @Override
        public void start(Callback<Device> callback) {
            request.start(callback);
        }
    }
}
