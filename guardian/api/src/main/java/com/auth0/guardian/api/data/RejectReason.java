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

package com.auth0.guardian.api.data;

import com.auth0.guardian.api.GuardianAPI;
import com.auth0.requests.gson.JsonRequired;
import com.google.gson.annotations.SerializedName;

/**
 * Contains the identifier and description of a reject reason
 * <p>
 * The id must be used when rejecting an auth request with {@link GuardianAPI#rejectLogin}
 * <p>
 * The description is only used for display purposes. Only the id is required to correctly identify
 * the reject reason on the server side.
 * <p>
 * Instances of this class should only be created by {@link GuardianAPI} and must be used as value
 * objects.
 *
 * @author Nicolas Ulrich (nikolaseu@gmail.com)
 * @see GuardianAPI#getRejectReasons
 * @see GuardianAPI#rejectLogin
 */
public class RejectReason {

    @JsonRequired
    @SerializedName("type")
    private String id;

    @JsonRequired
    @SerializedName("description")
    private String description;

    /**
     * Returns the identifier for the reject reason. E.g. "mistake", "hack"
     * <p>
     * This is the string that must be used with {@link GuardianAPI#rejectLogin}
     *
     * @return the identifier
     * @see GuardianAPI#rejectLogin
     */
    public String getId() {
        return id;
    }

    /**
     * Returns a better, user friendly description of the reason. E.g. "I've been hacked"
     * <p>
     * Should only be used to display to the user
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }
}
