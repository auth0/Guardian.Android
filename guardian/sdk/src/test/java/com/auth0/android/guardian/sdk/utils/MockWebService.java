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

package com.auth0.android.guardian.sdk.utils;

import java.io.IOException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class MockWebService {

    private MockWebServer server;

    public MockWebService() throws IOException {
        this.server = new MockWebServer();
        this.server.start();
    }

    public String getDomain() {
        return server.url("/").toString();
    }

    public void shutdown() throws IOException {
        this.server.shutdown();
    }

    public RecordedRequest takeRequest() throws InterruptedException {
        return server.takeRequest();
    }

    public MockWebService willReturnEnrollmentInfo(String deviceAccountToken) {
        String json = "" +
                "{" +
                "   \"device_account_token\": \""+deviceAccountToken+"\"" +
                "}";
        server.enqueue(responseWithJSON(json, 200));
        return this;
    }

    public MockWebService willReturnTenantInfo(String name, String friendlyName, String pictureUrl) {
        String json = "" +
                "{" +
                "   \"name\": \""+name+"\"," +
                "   \"friendly_name\": \""+friendlyName+"\"," +
                "   \"picture_url\": \""+pictureUrl+"\"" +
                "}";
        server.enqueue(responseWithJSON(json, 200));
        return this;
    }

    public MockWebService willReturnRejectReasons() {
        String json = "" +
                "[" +
                "   {" +
                "       \"type\":\"hack\"," +
                "       \"description\":\"I've been hacked\"" +
                "   },{" +
                "       \"type\":\"mistake\"," +
                "       \"description\":\"It was just a mistake\"" +
                "   }" +
                "]";
        server.enqueue(responseWithJSON(json, 200));
        return this;
    }

    public MockWebService willReturnDeviceAccount(String id, String identifier, String name, String service, String token) {
        String json = "" +
                "{" +
                "   \"id\": \""+id+"\"," +
                "   \"identifier\": \""+identifier+"\"," +
                "   \"name\": \""+name+"\"," +
                "   \"push_credentials\": {" +
                "       \"service\": \""+service+"\"," +
                "       \"token\": \""+token+"\"" +
                "   }" +
                "}";
        server.enqueue(responseWithJSON(json, 200));
        return this;
    }

    public MockWebService willReturnServerError(int statusCode, String errorCode, String error, String message) {
        String json = "" +
                "{" +
                "   \"statusCode\": \""+statusCode+"\"," +
                "   \"errorCode\": \""+errorCode+"\"," +
                "   \"error\": \""+error+"\"," +
                "   \"message\": \""+message+"\"" +
                "}";
        server.enqueue(responseWithJSON(json, statusCode));
        return this;
    }

    public MockWebService willReturnInternalServerError() {
        String json = "Internal server error";
        server.enqueue(responseWithJSON(json, 500));
        return this;
    }

    public MockWebService willReturnSuccess(int code) {
        server.enqueue(responseWithCode(code));
        return this;
    }

    private MockResponse responseWithJSON(String json, int statusCode) {
        return new MockResponse()
                .setResponseCode(statusCode)
                .addHeader("Content-Type", "application/json")
                .setBody(json);
    }

    private MockResponse responseWithCode(int statusCode) {
        return new MockResponse()
                .setResponseCode(statusCode)
                .addHeader("Content-Type", "application/json");
    }
}
