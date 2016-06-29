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

/**
 * A parameterizable HTTP request that already has a method, url and path
 */
public interface ParameterizableRequest<T> extends ExecutableRequest<T> {

    /**
     * Add a parameter and its value to the body to send when executing the request.
     * You cannot mix name/value parameters and a body object to serialize
     *
     * @param name the name of the parameter
     * @param value the value of the parameter
     * @return itself
     */
    ParameterizableRequest<T> addParameter(String name, Object value);

    /**
     * Add a parameter to the query
     *
     * @param name the name of the parameter
     * @param value the value of the parameter
     * @return itself
     */
    ParameterizableRequest<T> addQueryParameter(String name, String value);

    /**
     * Add a header parameter
     *
     * @param name the name of the parameter
     * @param value the value of the parameter
     * @return itself
     */
    ParameterizableRequest<T> addHeader(String name, String value);

    /**
     * Set the Authorization header. Equivalent to `addHeader("Authorization", "Bearer " + token)`
     *
     * @param token the authorization token
     * @return itself
     */
    ParameterizableRequest<T> setBearer(String token);
}
