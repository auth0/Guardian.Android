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

import com.auth0.android.guardian.networking.Callback;
import com.auth0.android.guardian.networking.ParseErrorException;
import com.auth0.android.guardian.networking.Serializer;
import com.auth0.android.guardian.networking.ServerErrorException;

import java.io.IOException;
import java.io.Reader;
import java.util.concurrent.Executor;

import okhttp3.Call;
import okhttp3.Response;

/**
 * @author Nicolas Ulrich (nikolaseu@gmail.com)
 */
public class WebServiceCall<T> {

    private final Executor executor;
    private final Serializer serializer;
    private final Serializer.Parser<T> parser;
    private final Call call;

    public WebServiceCall(Executor executor,
                          Serializer serializer,
                          Serializer.Parser<T> parser,
                          Call call) {
        this.executor = executor;
        this.serializer = serializer;
        this.parser = parser;
        this.call = call;
    }

    public T execute() throws IOException, ServerErrorException, ParseErrorException {
        Response response = call.execute();
        if (response.isSuccessful()) {
            return payloadFromResponse(response);
        }

        throw exceptionFromErrorResponse(response);
    }

    public void start(final Callback<T> callback) {
        call.enqueue(new okhttp3.Callback() {
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

    private T payloadFromResponse(Response response) throws ParseErrorException {
        if (parser != null) {
            try {
                final Reader reader = response.body().charStream();
                return parser.parse(reader);
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
            return serializer.parseServerError(reader, response.code());
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
