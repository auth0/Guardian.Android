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

package com.auth0.android.guardian.sdk;

import java.util.Map;

public class GuardianException extends RuntimeException {

    private final Map<String, String> errorResponse;
    private final String errorCode;

    public GuardianException(String detailMessage) {
        super(detailMessage);
        this.errorCode = null;
        this.errorResponse = null;
    }

    public GuardianException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
        this.errorCode = null;
        this.errorResponse = null;
    }

    public GuardianException(Map<String, String> errorResponse) {
        super(errorResponse.get("error"));
        this.errorCode = errorResponse.get("errorCode");
        this.errorResponse = errorResponse;
    }

    @Override
    public String toString() {
        if (errorResponse != null) {
            return "GuardianException{" + errorResponse + '}';
        } else {
            return super.toString();
        }
    }
}
