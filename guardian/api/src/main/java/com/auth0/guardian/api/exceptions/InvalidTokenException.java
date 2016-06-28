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
 * Exception that indicates that the token is invalid.
 * <p>
 * This exception occurs when trying to update or delete a {@link com.auth0.guardian.api.data.DeviceAccount}
 * and the Guardian server returns with the error code "invalid_token".
 *
 * @author Nicolas Ulrich (nikolaseu@gmail.com)
 * @see com.auth0.guardian.api.GuardianAPI#deleteDeviceAccount
 * @see com.auth0.guardian.api.GuardianAPI#updateDeviceAccount
 */
public class InvalidTokenException extends GuardianServerErrorException {

    public static final String ERROR_CODE = "invalid_token";

    public InvalidTokenException(ServerError serverError, int statusCode) {
        super(serverError, statusCode);
    }
}
