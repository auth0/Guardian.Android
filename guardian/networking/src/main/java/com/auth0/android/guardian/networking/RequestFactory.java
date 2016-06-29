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

package com.auth0.android.guardian.networking;

import com.auth0.android.guardian.networking.gson.GsonSerializer;
import com.auth0.android.guardian.networking.gson.JsonRequiredTypeAdapterFactory;
import com.auth0.android.guardian.networking.internal.CurrentThreadExecutor;
import com.auth0.android.guardian.networking.internal.SimpleServerErrorParser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Type;
import java.util.concurrent.Executor;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;

/**
 * A {@link Request} factory
 *
 * @author Nicolas Ulrich (nikolaseu@gmail.com)
 */
public class RequestFactory {

    private final Executor callbackExecutor;
    private final Serializer serializer;
    private final OkHttpClient client;
    private final MediaType mediaType;

    private RequestFactory(Executor callbackExecutor,
                   Serializer serializer,
                   OkHttpClient client) {
        this.callbackExecutor = callbackExecutor;
        this.serializer = serializer;
        this.client = client;
        this.mediaType = MediaType.parse(serializer.getMediaType());
    }

    /**
     * Creates a request expecting a response that will be parsed as an instance of the given Class.
     * <p>
     * This method should be used when there's a particular Class that conforms to the expected
     * response.
     *
     * @param classOfT the Class of the expected response
     * @param <T> the type of the expected response
     * @return a configurable request
     * @see RequestFactory#newRequest(Type)
     * @see RequestFactory#newRequest()
     */
    public <T> Request<T> newRequest(Class<T> classOfT) {
        return new com.auth0.android.guardian.networking.internal.Request<>(
                callbackExecutor, serializer,
                serializer.createParserFor(classOfT),
                serializer,
                mediaType,
                client);
    }

    /**
     * Creates a request expecting a response that will be parsed as an instance of the given type.
     * <p>
     * This method could be used when the response is an array of objects of a given class, or
     * simply a generic JSON object that should be returned as a {@code Map<String, Object>}.
     *
     * @param typeOfT the {@link Type} of the expected response
     * @param <T> the type of the expected response
     * @return a configurable request
     * @see RequestFactory#newRequest(Class)
     * @see RequestFactory#newRequest()
     */
    public <T> Request<T> newRequest(Type typeOfT) {
        return new com.auth0.android.guardian.networking.internal.Request<>(
                callbackExecutor, serializer,
                serializer.<T>createParserFor(typeOfT),
                serializer,
                mediaType,
                client);
    }

    /**
     * Creates a request whose response will not be parsed. The request will only notify if the
     * actual http request was successful or not.
     *
     * @return a configurable request
     * @see RequestFactory#newRequest(Class)
     * @see RequestFactory#newRequest(Type)
     */
    public Request<Void> newRequest() {
        return new com.auth0.android.guardian.networking.internal.Request<>(
                callbackExecutor, serializer,
                null,
                serializer,
                mediaType,
                client);
    }

    /**
     * A Builder for {@link RequestFactory}
     */
    public static class Builder {
        private Executor callbackExecutor;
        private Serializer serializer;
        private ServerErrorParser errorParser;
        private OkHttpClient client;

        /**
         * Overrides the {@link Executor} that is used when running the callbacks.
         * Set your own executor if you want to receive the responses in a specific thread, like the
         * Android Main/UI thread for example.
         * <p>
         * By default the callbacks will be executed in a random background thread.
         *
         * @param callbackExecutor the executor used to call the callbacks
         * @return itself
         */
        public Builder callbackExecutor(Executor callbackExecutor) {
            this.callbackExecutor = callbackExecutor;
            return this;
        }

        /**
         * Overrides the serializer that is used to serialize the body and to parse the responses
         * and server errors.
         * <p>
         * By default we will use a {@link GsonSerializer} for the serialization/parsing, and the
         * error responses will be returned as {@link ServerErrorException} that only includes the
         * HTTP code.
         * <p>
         * The serializer itself is a {@link ServerErrorParser}, so you cannot set them separately.
         *
         * @param serializer your own serializer
         * @return itself
         * @see Builder#errorParser(ServerErrorParser)
         * @see GsonSerializer
         * @throws IllegalArgumentException when trying to set a serializer and server error parser
         *                                  at the same time.
         */
        public Builder serializer(Serializer serializer) {
            if (errorParser != null) {
                throw new IllegalArgumentException(
                        "You cannot supply a serializer if already providing an error parser");
            }

            this.serializer = serializer;
            return this;
        }

        /**
         * Overrides only the parser that will be used when receiving an error response. If the API
         * uses a standard format for all error responses, you may want to centralize all error
         * handling and return special subclasses of {@link ServerErrorException} for particular
         * responses, including more information about your use case.
         * <p>
         * By default the error responses will be returned as {@link ServerErrorException} that
         * only includes the HTTP code.
         * <p>
         * The error parser is included in the {@link Serializer}, so you can only use this method
         * when you are using the default serializer.
         *
         * @param errorParser your own parser for error responses
         * @return itself
         * @see Builder#serializer(Serializer)
         * @throws IllegalArgumentException when trying to set a serializer and server error parser
         *                                  at the same time.
         */
        public Builder errorParser(ServerErrorParser errorParser) {
            if (serializer != null) {
                throw new IllegalArgumentException(
                        "You cannot supply an errorParser if already providing a full serializer");
            }

            this.errorParser = errorParser;
            return this;
        }

        /**
         * Overrides the {@link OkHttpClient} that is used to execute the requests.
         * <p>
         * This is only included in case you want to use your own customizations, timeouts, cache,
         * etc.
         * <p>
         * By default, the {@link RequestFactory} will create and use its own instance
         *
         * @param client your own client
         * @return itself
         */
        public Builder client(OkHttpClient client) {
            this.client = client;
            return this;
        }

        /**
         * Builds the {@link RequestFactory}
         *
         * @return the new {@link RequestFactory} instance
         */
        public RequestFactory build() {
            if (callbackExecutor == null) {
                callbackExecutor = new CurrentThreadExecutor();
            }

            if (serializer == null) {
                if (errorParser == null) {
                    errorParser = new SimpleServerErrorParser();
                }

                Gson gson = new GsonBuilder()
                        .registerTypeAdapterFactory(new JsonRequiredTypeAdapterFactory())
                        .create();

                serializer = new GsonSerializer(gson, errorParser);
            }

            if (client == null) {
                client = new OkHttpClient();
            }

            return new RequestFactory(callbackExecutor, serializer, client);
        }
    }
}
