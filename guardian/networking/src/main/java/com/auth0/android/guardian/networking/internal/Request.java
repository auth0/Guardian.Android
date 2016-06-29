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

package com.auth0.android.guardian.networking.internal;

import com.auth0.android.guardian.networking.HttpRequest;
import com.auth0.android.guardian.networking.ParameterizableRequest;
import com.auth0.android.guardian.networking.ParseErrorException;
import com.auth0.android.guardian.networking.Serializer;
import com.auth0.android.guardian.networking.Callback;
import com.auth0.android.guardian.networking.ServerErrorException;
import com.auth0.android.guardian.networking.ServerErrorParser;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Request<T> implements
        com.auth0.android.guardian.networking.Request<T>,
        HttpRequest<T>,
        ParameterizableRequest<T> {

    private final Executor executor;
    private final Serializer serializer;
    private final Serializer.Parser<T> responseParser;
    private final ServerErrorParser serverErrorParser;
    private final MediaType mediaType;
    private final OkHttpClient client;

    private HttpUrl baseUrl;
    private String path;
    private String method;
    private Object body;
    private final Map<String, String> headers;
    private final Map<String, Object> bodyParameters;
    private final Map<String, String> queryParameters;

    public Request(Executor executor,
                   Serializer serializer,
                   Serializer.Parser<T> responseParser,
                   ServerErrorParser serverErrorParser,
                   MediaType mediaType,
                   OkHttpClient client) {
        this.executor = executor;
        this.serializer = serializer;
        this.responseParser = responseParser;
        this.serverErrorParser = serverErrorParser;
        this.mediaType = mediaType;
        this.client = client;

        this.headers = new HashMap<>();
        this.bodyParameters = new HashMap<>();
        this.queryParameters = new HashMap<>();
    }

    @Override
    public HttpRequest<T> baseUrl(String baseUrl) {
        this.baseUrl = HttpUrl.parse(baseUrl);
        if (this.baseUrl == null) {
            throw new IllegalArgumentException("Cannot use an invalid HTTP or HTTPS url: " + baseUrl);
        }
        return this;
    }

    @Override
    public ParameterizableRequest<T> addParameter(String name, Object value) {
        if (body != null) {
            throw new IllegalArgumentException("Cannot set body and parameters at the same time");
        }
        bodyParameters.put(name, value);
        return this;
    }

    @Override
    public ParameterizableRequest<T> addQueryParameter(String name, String value) {
        queryParameters.put(name, value);
        return this;
    }

    @Override
    public ParameterizableRequest<T> addHeader(String name, String value) {
        headers.put(name, value);
        return this;
    }

    @Override
    public ParameterizableRequest<T> setBearer(String token) {
        return addHeader("Authorization", "Bearer " + token);
    }

    @Override
    public ParameterizableRequest<T> post(String path) {
        return post(path, null);
    }

    @Override
    public ParameterizableRequest<T> post(String path, Object body) {
        setPathBodyAndMethod(path, body, "POST");
        return this;
    }

    @Override
    public ParameterizableRequest<T> patch(String path) {
        return patch(path, null);
    }

    @Override
    public ParameterizableRequest<T> patch(String path, Object body) {
        setPathBodyAndMethod(path, body, "PATCH");
        return this;
    }

    @Override
    public ParameterizableRequest<T> put(String path) {
        return put(path, null);
    }

    @Override
    public ParameterizableRequest<T> put(String path, Object body) {
        setPathBodyAndMethod(path, body, "PUT");
        return this;
    }

    @Override
    public ParameterizableRequest<T> delete(String path) {
        setPathBodyAndMethod(path, null, "DELETE");
        return this;
    }

    @Override
    public ParameterizableRequest<T> get(String path) {
        setPathBodyAndMethod(path, null, "GET");
        return this;
    }

    @Override
    public T execute() throws IOException, ServerErrorException, ParseErrorException {
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
                        callSuccess(callback, data);
                    } else {
                        ServerErrorException exception = exceptionFromErrorResponse(response);
                        callFailure(callback, exception);
                    }
                } catch (ParseErrorException exception) {
                    callFailure(callback, exception);
                }
            }

            @Override
            public void onFailure(Call call, IOException t) {
                callFailure(callback, t);
            }
        });
    }

    private void setPathBodyAndMethod(String path, Object body, String method) {
        this.path = path;
        this.body = body;
        this.method = method;
    }

    private Call buildCall() {
        if (baseUrl == null) {
            throw new IllegalArgumentException("You must set a baseUrl");
        }

        HttpUrl.Builder urlBuilder = baseUrl.newBuilder()
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

        return client.newCall(builder.build());
    }

    private T payloadFromResponse(Response response) throws ParseErrorException {
        if (responseParser != null) {
            try {
                final Reader reader = response.body().charStream();
                return responseParser.parse(reader);
            } catch (Exception e) {
                throw new ParseErrorException("Error parsing server response", e);
            }
        } else {
            return null;
        }
    }

    private ServerErrorException exceptionFromErrorResponse(Response response) throws ParseErrorException {
        try {
            final Reader reader = response.body().charStream();
            return serverErrorParser.parseServerError(reader, response.code());
        } catch (Exception t) {
            throw new ParseErrorException("Error parsing server error", t);
        }
    }

    private void callSuccess(final Callback<T> callback, final T data) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                callback.onSuccess(data);
            }
        });
    }

    private void callFailure(final Callback<T> callback, final Exception exception) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                callback.onFailure(exception);
            }
        });
    }
}
