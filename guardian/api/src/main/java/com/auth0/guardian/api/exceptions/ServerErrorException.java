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

package com.auth0.guardian.api.exceptions;

import com.auth0.guardian.api.data.ServerError;

/**
 * This is a general exception for all errors returned by the Guardian server.
 * <p>
 * There are subclasses for specific errors that are useful in order to identify/handle separately
 *
 * @author Nicolas Ulrich (nikolaseu@gmail.com)
 * @see com.auth0.guardian.api.GuardianAPI
 */
public class ServerErrorException extends Exception {

    private final String errorCode;
    private final int statusCode;
    private final String error;

    public ServerErrorException(String detailMessage, String errorCode, int statusCode, Throwable throwable) {
        super(detailMessage, throwable);
        this.errorCode = errorCode;
        this.statusCode = statusCode;
        this.error = null;
    }

    public ServerErrorException(ServerError serverError) {
        super(serverError.getMessage());
        this.errorCode = serverError.getErrorCode();
        this.statusCode = serverError.getStatusCode();
        this.error = serverError.getError();
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getError() {
        return error;
    }

    @Override
    public String toString() {
        return "ServerErrorException{" +
                "errorCode='" + errorCode + '\'' +
                ", statusCode=" + statusCode +
                ", error='" + error + '\'' +
                ", super='" + super.toString() + '\'' +
                '}';
    }
}
