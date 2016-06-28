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
 * Contains the information of a Guardian Tenant, like the name, a friendly name and the url of an
 * image that could be used as the tenant logo/icon.
 * <p>
 * Instances of this class should only be created by {@link GuardianAPI} and must be used as value
 * objects.
 *
 * @author Nicolas Ulrich (nikolaseu@gmail.com)
 * @see GuardianAPI#getTenantInfo
 */
public class TenantInfo {

    @JsonRequired
    @SerializedName("name")
    private String name;

    @JsonRequired
    @SerializedName("friendly_name")
    private String friendlyName;

    @SerializedName("picture_url")
    private String pictureUrl;

    /**
     * Returns the name that is used to identify the Guardian Tenant
     *
     * @return the tenant name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the friendly name of the Guardian Tenant, usually to display a better, more friendly
     * name to the user
     *
     * @return the tenant friendly name
     */
    public String getFriendlyName() {
        return friendlyName;
    }

    /**
     * Returns the URL of an image that could be used as the Tenant logo or icon
     *
     * @return the image url
     */
    public String getPictureUrl() {
        return pictureUrl;
    }

    @Override
    public String toString() {
        return "TenantInfo{" +
                "name='" + name + '\'' +
                ", friendlyName='" + friendlyName + '\'' +
                ", pictureUrl='" + pictureUrl + '\'' +
                '}';
    }
}
