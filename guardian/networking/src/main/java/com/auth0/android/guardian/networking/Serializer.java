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

import java.io.Reader;
import java.lang.reflect.Type;

/**
 * Serializer for the body, and a parser for responses and server errors
 *
 * @see RequestFactory.Builder#serializer(Serializer)
 */
public interface Serializer extends ServerErrorParser {

    /**
     * Should return the HTTP content type that indicates the format used by this serializer
     */
    String getMediaType();

    /**
     * Serializes the body and returns it as a String
     *
     * @param body a class instance or {@code Map<String, Object>} that represents the request body
     * @return the body serialized
     */
    String serialize(Object body);

    /**
     * Returns a parser for a given Class
     *
     * @param classOfT the {@link Class} of the expected parsed response
     * @param <T> the type of the expected parsed response
     * @return the parser
     */
    <T> Parser<T> createParserFor(Class<T> classOfT);

    /**
     * Returns a parser for the given Type
     *
     * @param typeOfT the {@link Type} of the expected response
     * @param <T> the type of the expected response
     * @return the parser
     */
    <T> Parser<T> createParserFor(Type typeOfT);

    /**
     * Parser for a given type
     *
     * @param <T> the type of the expected response
     */
    interface Parser<T> {

        /**
         * Parses the data contained in the {@link Reader} and returns it as an instance of the
         * given type.
         *
         * @param reader the data container
         * @return an instance containing the parsed data
         */
        T parse(Reader reader);
    }
}
