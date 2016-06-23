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

package com.auth0.requests.gson;

import com.auth0.requests.Serializer;
import com.auth0.requests.ServerErrorParser;
import com.google.gson.Gson;

import java.io.Reader;
import java.lang.reflect.Type;

/**
 * @author Nicolas Ulrich (nikolaseu@gmail.com)
 */
public class GsonSerializer implements Serializer {

    private static final String MEDIA_TYPE = "application/json; charset=utf-8";

    private final Gson gson;
    private final ServerErrorParser errorParser;

    public GsonSerializer(Gson gson, ServerErrorParser errorParser) {
        this.gson = gson;
        this.errorParser = errorParser;
    }

    @Override
    public String getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public String serialize(Object body) {
        return gson.toJson(body);
    }

    @Override
    public Exception parseServerError(Reader reader, int statusCode) {
        return errorParser.parse(reader, statusCode);
    }

    @Override
    public <T> Parser<T> createParserFor(Class<T> classOfT) {
        return new GsonClassResponseParser<>(gson, classOfT);
    }

    @Override
    public <T> Parser<T> createParserFor(Type typeOfT) {
        return new GsonTypeResponseParser<>(gson, typeOfT);
    }

    private class GsonClassResponseParser<T> implements Serializer.Parser<T> {

        private final Gson gson;
        private final Class<T> classOfT;

        public GsonClassResponseParser(Gson gson, Class<T> classOfT) {
            this.gson = gson;
            this.classOfT = classOfT;
        }

        @Override
        public T parse(Reader reader) {
            return gson.fromJson(reader, classOfT);
        }
    }

    private class GsonTypeResponseParser<T> implements Serializer.Parser<T> {

        private final Gson gson;
        private final Type typeOfT;

        public GsonTypeResponseParser(Gson gson, Type typeOfT) {
            this.gson = gson;
            this.typeOfT = typeOfT;
        }

        @Override
        public T parse(Reader reader) {
            return gson.fromJson(reader, typeOfT);
        }
    }
}
