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

package com.auth0.requests.internal;

import com.auth0.requests.Serializer;
import com.auth0.requests.Callback;
import com.auth0.requests.ServerErrorException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;

/**
 * @author Nicolas Ulrich (nikolaseu@gmail.com)
 */
public class Request<T> implements com.auth0.requests.Request<T> {

    private final Executor executor;
    private final Serializer serializer;
    private final Serializer.Parser<T> parser;
    private final MediaType mediaType;
    private final OkHttpClient client;

    private String baseUrl;
    private String path;
    private String method;
    private Object body;
    private final Map<String, String> headers;
    private final Map<String, Object> bodyParameters;
    private final Map<String, String> queryParameters;

    public Request(Executor executor,
                   Serializer serializer,
                   Serializer.Parser<T> parser,
                   MediaType mediaType,
                   OkHttpClient client) {
        this.executor = executor;
        this.serializer = serializer;
        this.parser = parser;
        this.mediaType = mediaType;
        this.client = client;

        this.headers = new HashMap<>();
        this.bodyParameters = new HashMap<>();
        this.queryParameters = new HashMap<>();
    }

    @Override
    public Request<T> baseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    @Override
    public Request<T> addParameter(String name, Object value) {
        bodyParameters.put(name, value);
        return this;
    }

    @Override
    public Request<T> addQueryParameter(String name, String value) {
        queryParameters.put(name, value);
        return this;
    }

    @Override
    public Request<T> addHeader(String name, String value) {
        headers.put(name, value);
        return this;
    }

    @Override
    public Request<T> setBearer(String jwt) {
        return addHeader("Authorization", "Bearer " + jwt);
    }

    @Override
    public Request<T> post(String path) {
        return post(path, null);
    }

    @Override
    public Request<T> post(String path, Object body) {
        this.path = path;
        this.method = "POST";
        this.body = body;
        return this;
    }

    @Override
    public Request<T> patch(String path) {
        return patch(path, null);
    }

    @Override
    public Request<T> patch(String path, Object body) {
        this.path = path;
        this.method = "PATCH";
        this.body = body;
        return this;
    }

    @Override
    public Request<T> delete(String path) {
        this.path = path;
        this.method = "DELETE";
        return this;
    }

    @Override
    public Request<T> get(String path) {
        this.path = path;
        this.method = "GET";
        return this;
    }

    @Override
    public T execute() throws IOException, ServerErrorException {
        return buildCall().execute();
    }

    @Override
    public void start(Callback<T> callback) {
        buildCall().start(callback);
    }

    private WebServiceCall<T> buildCall() {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl).newBuilder()
                .addPathSegments(path);

        for (Map.Entry<String, String> entry : queryParameters.entrySet()) {
            urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
        }

        okhttp3.Request.Builder builder = new okhttp3.Request.Builder()
                .url(urlBuilder.build());

        if (body == null && bodyParameters.isEmpty()) {
            builder.method(method, null);
        } else if (body != null) {
            builder.method(method, RequestBody.create(mediaType, serializer.serialize(body)));
        } else {
            builder.method(method, RequestBody.create(mediaType, serializer.serialize(bodyParameters)));
        }

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            builder.addHeader(entry.getKey(), entry.getValue());
        }

        Call call = client.newCall(builder.build());

        return new WebServiceCall<T>(executor, serializer, parser, call);
    }
}
