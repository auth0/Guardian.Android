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

package com.auth0.android.guardian.sdk.networking;

import com.auth0.android.guardian.sdk.GuardianAPIRequest;
import com.auth0.android.guardian.sdk.GuardianException;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Request<T> implements GuardianAPIRequest<T> {

    private static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private final GsonConverter converter;
    private final OkHttpClient client;
    private final Type typeOfT;
    private final HttpUrl url;
    private final String method;

    private Object body;
    private final Map<String, String> headers;
    private final Map<String, Object> bodyParameters;
    private final Map<String, String> queryParameters;

    Request(String method, String url, GsonConverter converter, OkHttpClient client, Type typeOfT) {
        this.method = method;
        this.url = HttpUrl.parse(url);
        if (this.url == null) {
            throw new IllegalArgumentException("Cannot use an invalid HTTP or HTTPS url: " + url);
        }

        this.converter = converter;
        this.client = client;
        this.typeOfT = typeOfT;

        this.headers = new HashMap<>();
        this.bodyParameters = new HashMap<>();
        this.queryParameters = new HashMap<>();
    }

    public Request<T> addParameter(String name, Object value) throws IllegalArgumentException {
        if (body != null) {
            throw new IllegalArgumentException("Cannot set body and parameters at the same time");
        }
        bodyParameters.put(name, value);
        return this;
    }

    public Request<T> addQueryParameter(String name, String value) {
        queryParameters.put(name, value);
        return this;
    }

    public Request<T> addHeader(String name, String value) {
        headers.put(name, value);
        return this;
    }

    public Request<T> setBearer(String token) {
        return addHeader("Authorization", "Bearer " + token);
    }

    public Request<T> setBody(Object body) {
        this.body = body;
        return this;
    }

    @Override
    public T execute() throws IOException {
        Response response = buildCall().execute();
        if (response.isSuccessful()) {
            return payloadFromResponse(response);
        }

        throw exceptionFromErrorResponse(response);
    }

    @Override
    public void start(final Callback<T> callback) {
        buildCall().enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(Call call, Response response) {
                try {
                    if (response.isSuccessful()) {
                        final T data = payloadFromResponse(response);
                        callback.onSuccess(data);
                    } else {
                        GuardianException exception = exceptionFromErrorResponse(response);
                        callback.onFailure(exception);
                    }
                } catch (GuardianException exception) {
                    callback.onFailure(exception);
                }
            }

            @Override
            public void onFailure(Call call, IOException exception) {
                callback.onFailure(exception);
            }
        });
    }

    private Call buildCall() {
        HttpUrl.Builder urlBuilder = url.newBuilder();

        for (Map.Entry<String, String> entry : queryParameters.entrySet()) {
            urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
        }

        okhttp3.Request.Builder requestBuilder = new okhttp3.Request.Builder()
                .url(urlBuilder.build());

        if (body == null && bodyParameters.isEmpty()) {
            requestBuilder.method(method, null);
        } else if (body != null) {
            requestBuilder.method(method, RequestBody.create(MEDIA_TYPE, converter.serialize(body)));
        } else {
            requestBuilder.method(method, RequestBody.create(MEDIA_TYPE, converter.serialize(bodyParameters)));
        }

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            requestBuilder.addHeader(entry.getKey(), entry.getValue());
        }

        return client.newCall(requestBuilder.build());
    }

    private T payloadFromResponse(Response response) throws GuardianException {
        try {
            final Reader reader = response.body().charStream();
            return converter.parse(typeOfT, reader);
        } catch (Exception e) {
            throw new GuardianException("Error parsing server response", e);
        }
    }

    private GuardianException exceptionFromErrorResponse(Response response) {
        try {
            final Reader reader = response.body().charStream();
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, String> error = converter.parse(type, reader);
            return new GuardianException(error.get("error"), error.get("errorCode"));
        } catch (Exception t) {
            return new GuardianException("Error parsing server error", t);
        }
    }
}
