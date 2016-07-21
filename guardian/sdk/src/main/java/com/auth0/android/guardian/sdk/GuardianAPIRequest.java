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

import android.support.annotation.NonNull;

import com.auth0.android.guardian.sdk.networking.Callback;

import java.io.IOException;

/**
 * A Guardian request that could be executed synchronously or in a background thread.
 * @param <T> the type of the expected response
 */
public interface GuardianAPIRequest<T> {

    /**
     * Executes the request synchronously, blocking the current thread until the request finishes
     *
     * @return the response
     * @throws IOException when there's a connection problem
     * @throws GuardianException when something else went wrong
     */
    T execute() throws IOException, GuardianException;

    /**
     * Starts to execute the request asynchronously, in a background thread. A successful response
     * or the fail cause will be notified to the callback
     *
     * @param callback the Callback where the response or failure will be received
     */
    void start(@NonNull final Callback<T> callback);
}
