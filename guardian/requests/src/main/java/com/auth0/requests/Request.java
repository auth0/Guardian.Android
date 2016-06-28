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

/**
 * @author Nicolas Ulrich (nikolaseu@gmail.com)
 */
public interface Request<T> extends ParameterizableRequest<T> {

    /**
     * Sets the base url, the domain of the server
     *
     * @param baseUrl the domain of the server
     * @return itself
     */
    Request<T> baseUrl(String baseUrl);

    /**
     * Sets the path for the request and the method to 'POST'.
     * Useful for cases where the body was set adding individual parameters
     *
     * @param path the path of the endpoint
     * @return itself
     */
    ParameterizableRequest<T> post(String path);

    /**
     * Sets the path for the request, the method to 'POST' and will include the serialized object
     * as the body of the request
     *
     * @param path the path of the endpoint
     * @param body the object to be serialized and included as the body of the request
     * @return itself
     */
    ParameterizableRequest<T> post(String path, Object body);

    /**
     * Sets the path for the request and the method to 'PATCH'.
     * Useful for cases where the body was set adding individual parameters
     *
     * @param path the path of the endpoint
     * @return itself
     */
    ParameterizableRequest<T> patch(String path);

    /**
     * Sets the path for the request, the method to 'PATCH' and will include the serialized object
     * as the body of the request
     *
     * @param path the path of the endpoint
     * @param body the object to be serialized and included as the body of the request
     * @return itself
     */
    ParameterizableRequest<T> patch(String path, Object body);

    /**
     * Sets the path for the request and the method to 'PUT'.
     * Useful for cases where the body was set adding individual parameters
     *
     * @param path the path of the endpoint
     * @return itself
     */
    ParameterizableRequest<T> put(String path);

    /**
     * Sets the path for the request, the method to 'PUT' and will include the serialized object
     * as the body of the request
     *
     * @param path the path of the endpoint
     * @param body the object to be serialized and included as the body of the request
     * @return itself
     */
    ParameterizableRequest<T> put(String path, Object body);

    /**
     * Sets the path for the request and the method to 'DELETE'
     *
     * @param path the path of the endpoint
     * @return itself
     */
    ParameterizableRequest<T> delete(String path);

    /**
     * Sets the path for the request and the method to 'GET'
     *
     * @param path the path of the endpoint
     * @return itself
     */
    ParameterizableRequest<T> get(String path);
}
