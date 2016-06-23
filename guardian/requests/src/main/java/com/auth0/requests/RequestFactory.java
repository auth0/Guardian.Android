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

package com.auth0.requests;

import java.lang.reflect.Type;
import java.util.concurrent.Executor;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;

/**
 * @author Nicolas Ulrich (nikolaseu@gmail.com)
 */
public class RequestFactory {

    private final Executor callbackExecutor;
    private final Serializer serializer;
    private final OkHttpClient client;
    private final MediaType mediaType;

    public RequestFactory(Executor callbackExecutor,
                          Serializer serializer,
                          OkHttpClient client) {
        this.callbackExecutor = callbackExecutor;
        this.serializer = serializer;
        this.client = client;
        this.mediaType = MediaType.parse(serializer.getMediaType());
    }

    public <T> Request<T> newRequest(Class<T> classOfT) {
        return new com.auth0.requests.internal.Request<>(
                callbackExecutor, serializer,
                serializer.createParserFor(classOfT),
                mediaType,
                client);
    }

    public <T> Request<T> newRequest(Type typeOfT) {
        return new com.auth0.requests.internal.Request<>(
                callbackExecutor, serializer,
                serializer.<T>createParserFor(typeOfT),
                mediaType,
                client);
    }

    public Request<Void> newRequest() {
        return new com.auth0.requests.internal.Request<>(
                callbackExecutor, serializer,
                null,
                mediaType,
                client);
    }
}
