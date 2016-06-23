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

package com.auth0.guardian.sdk;

import android.os.Bundle;

import com.auth0.guardian.api.GuardianAPI;
import com.auth0.guardian.sdk.utils.AndroidMainThreadExecutor;

/**
 * High level client for the Guardian MFA Server
 * <p>
 * This class wraps all the requests and low level stuff required in order to handle the more common
 * use cases like enroll, unenroll, allow (or reject) a login and the parsing of the push
 * notification payload.
 *
 * @author Nicolas Ulrich (nikolaseu@gmail.com)
 * @see GuardianAPI
 */
public class Guardian {

    private final GuardianAPI api;

    Guardian(GuardianAPI api) {
        this.api = api;
    }

    /**
     * Starts an enroll, creating a new Guardian account
     *
     * @param data the data scanned from a QR code
     * @param callback where the resulting Account (or a failure) will be received
     */
    public void enroll(String data, Callback<Account> callback) {

    }

    /**
     * Unenrolls the given account
     *
     * @param account the account to unenroll
     * @param callback where the result (success or failure) will be notified
     */
    public void unenroll(Account account, Callback<Void> callback) {

    }

    /**
     * Allows the authorization request for the provided account and challenge
     *
     * @param account the account that will be used to allow the auth challenge
     * @param challenge the AuthChallenge received in the push notification
     * @param callback where the result (success or failure) will be notified
     */
    public void allowLogin(Account account, AuthChallenge challenge, Callback<Void> callback) {

    }

    /**
     * Rejects the authorization request for the provided account and challenge, possibly specifying
     * a reject reason
     *
     * @param account the account that will be used to allow the auth challenge
     * @param challenge the AuthChallenge received in the push notification
     * @param reason the reject reason
     * @param callback where the result (success or failure) will be notified
     */
    public void rejectLogin(Account account, AuthChallenge challenge, String reason, Callback<Void> callback) {

    }

    /**
     * Parses the push notification payload and returns the authentication challenge that is used to
     * identify a given authentication request in order to allow or reject it.
     *
     * @param pushPayload the bundle received in the push notification sent by guardian server
     * @return an AuthChallenge
     */
    public AuthChallenge parsePushNotification(Bundle pushPayload) {
        return null;
    }

    /**
     * Get the API client.
     * Useful for other low level tasks like getting the possible reject reasons or the tenant info.
     *
     * @return the API client used by this instance
     */
    public GuardianAPI getAPIClient() {
        return api;
    }

    /**
     * Generic callback for the async Guardian methods
     *
     * @param <T> the response type
     */
    public interface Callback<T> {

        /**
         * Called when the operation finishes successfully
         *
         * @param result the result of the operation
         */
        void onSuccess(T result);

        /**
         * Called when the operation could not finish successfully
         *
         * @param error the error that occurred when executing the operation
         */
        void onFailure(Throwable error);
    }

    /**
     * Builder for Guardian instances
     */
    public static class Builder {

        private String baseUrl;
        private GuardianAPI api;

        /**
         * Sets the domain of the guardian server.
         *
         * @param baseUrl the url
         * @return itself
         */
        public Builder baseUrl(String baseUrl) {
            if (api != null) {
                throw new IllegalArgumentException("Cannot set baseUrl and api at the same time, they're mutually exclusive options");
            }

            this.baseUrl = baseUrl;
            return this;
        }

        /**
         * Sets the api client to use. Useful when we already have it or when we need more
         * customization options
         *
         * @param api the GuardianAPI to use
         * @return itself
         */
        public Builder api(GuardianAPI api) {
            if (baseUrl != null) {
                throw new IllegalArgumentException("Cannot set api and baseUrl at the same time, they're mutually exclusive options");
            }

            this.api = api;
            return this;
        }

        /**
         * Creates the instance with the options provided
         *
         * @return the Guardian instance
         */
        public Guardian build() {
            if (api == null) {
                if (baseUrl == null) {
                    throw new IllegalArgumentException("baseUrl cannot be null");
                }

                api = new GuardianAPI.Builder()
                        .baseUrl(baseUrl)
                        .callbackExecutor(new AndroidMainThreadExecutor())
                        .build();
            }

            return new Guardian(api);
        }
    }
}
