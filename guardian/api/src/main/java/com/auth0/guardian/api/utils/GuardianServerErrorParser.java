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

package com.auth0.guardian.api.utils;

import com.auth0.guardian.api.data.ServerError;
import com.auth0.guardian.api.exceptions.DeviceAccountNotFoundException;
import com.auth0.guardian.api.exceptions.EnrollmentTransactionNotFoundException;
import com.auth0.guardian.api.exceptions.GuardianServerErrorException;
import com.auth0.guardian.api.exceptions.InvalidOTPCodeException;
import com.auth0.guardian.api.exceptions.InvalidTokenException;
import com.auth0.guardian.api.exceptions.LoginTransactionNotFoundException;
import com.auth0.guardian.api.exceptions.UnparseableServerErrorException;
import com.auth0.requests.ServerErrorException;
import com.auth0.requests.ServerErrorParser;
import com.google.gson.Gson;

import java.io.Reader;

/**
 * @author Nicolas Ulrich (nikolaseu@gmail.com)
 */
public class GuardianServerErrorParser implements ServerErrorParser {

    private final Gson gson;

    public GuardianServerErrorParser(Gson gson) {
        this.gson = gson;
    }

    @Override
    public ServerErrorException parse(Reader reader, int statusCode) {
        try {
            ServerError serverError = gson.fromJson(reader, ServerError.class);
            switch (serverError.getErrorCode()) {
                case InvalidTokenException.ERROR_CODE:
                    return new InvalidTokenException(serverError, statusCode);
                case InvalidOTPCodeException.ERROR_CODE:
                    return new InvalidOTPCodeException(serverError, statusCode);
                case LoginTransactionNotFoundException.ERROR_CODE:
                    return new LoginTransactionNotFoundException(serverError, statusCode);
                case DeviceAccountNotFoundException.ERROR_CODE:
                    return new DeviceAccountNotFoundException(serverError, statusCode);
                case EnrollmentTransactionNotFoundException.ERROR_CODE:
                    return new EnrollmentTransactionNotFoundException(serverError, statusCode);
                default:
                    // Any other unhandled error
                    return new GuardianServerErrorException(serverError, statusCode);
            }
        } catch (Exception e) {
            return new UnparseableServerErrorException("Invalid server error response: " + reader, statusCode, e);
        }
    }
}
