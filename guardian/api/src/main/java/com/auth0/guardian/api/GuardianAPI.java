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

package com.auth0.guardian.api;

import com.auth0.guardian.api.data.DeviceAccount;
import com.auth0.guardian.api.data.EnrollmentInfo;
import com.auth0.guardian.api.data.PushCredentials;
import com.auth0.guardian.api.data.RejectReason;
import com.auth0.guardian.api.data.TenantInfo;
import com.auth0.guardian.api.utils.GuardianServerErrorParser;
import com.auth0.requests.ExecutableRequest;
import com.auth0.requests.RequestFactory;
import com.auth0.requests.gson.GsonSerializer;
import com.auth0.requests.gson.JsonRequiredTypeAdapterFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.Executor;

import okhttp3.OkHttpClient;

/**
 * An API client for the Guardian MFA server.
 * <p>
 * This class is a low level API client for the Guardian MFA Server, and allows the user to easily
 * issue requests against the different Guardian endpoints.
 * <p>
 * All methods returns an {@link ExecutableRequest} that allows the user to execute it synchronously
 * or asynchronously and get the result or handle the failure.
 * <p>
 * Usage example:
 * <pre>
 * {@code
 *  // the API client works for only one guardian domain
 *  String baseUrl = "https://tenant.guardian.auth0.com";
 *
 *  // create the API client
 *  GuardianAPI client = new GuardianAPI.Builder()
 *      .baseUrl(baseUrl)
 *      .build();
 *
 *  // example: get the enrollment info to start an enroll flow (async)
 *  client.getEnrollmentInfo("enrollmentTransactionId")
 *      .start(new Callback<EnrollmentInfo> {
 *          @Override
 *          void onSuccess(EnrollmentInfo enrollmentInfo) {
 *              // we have the enrollment info to continue with the enroll flow
 *          }
 *
 *          @Override
 *          void onFailure(Throwable exception) {
 *              // something failed
 *          }
 *      });
 *
 *  // or sync
 *  try {
 *      EnrollmentInfo enrollmentInfo = client
 *          .getEnrollmentInfo("enrollmentTransactionId")
 *          .execute();
 *          // we have the enrollment info to continue with the enroll flow
 *  } catch (Exception e) {
 *      // something failed
 *  }
 * }
 * </pre>
 *
 * @author Nicolas Ulrich (nikolaseu@gmail.com)
 */
public class GuardianAPI {

    private final RequestFactory requestFactory;
    private final String baseUrl;

    private GuardianAPI(String baseUrl, RequestFactory requestFactory) {
        this.baseUrl = baseUrl;
        this.requestFactory = requestFactory;
    }

    /**
     * Returns a "device_account_token" that can be used to update the push notification settings
     * and also to un-enroll the device account
     * This endpoint should only be called once (when starting the enroll)
     *
     * @param enrollmentTxId the enrollment transaction id
     * @return a request to execute
     * @see EnrollmentInfo
     */
    public ExecutableRequest<EnrollmentInfo> getEnrollmentInfo(String enrollmentTxId) {
        return requestFactory.newRequest(EnrollmentInfo.class)
                .baseUrl(baseUrl)
                .post("api/enrollment-info")
                .addParameter("enrollment_tx_id", enrollmentTxId);
    }

    /**
     * Associate a device account that belongs to a user with a pair of device credentials, or
     * update these credentials. Confirms the device account if it was not confirmed
     *
     * @param id                 the device account id
     * @param deviceAccountToken the jwt to use in authorization header
     * @param identifier         a unique identifier for the device
     * @param name               the visible name of the device
     * @param pushService        the push service to use
     * @param token              the push service token
     * @return a request to execute
     * @see DeviceAccount
     */
    public ExecutableRequest<DeviceAccount> updateDeviceAccount(String id, String deviceAccountToken,
                                                                String identifier, String name,
                                                                String pushService, String token) {
        return requestFactory.newRequest(DeviceAccount.class)
                .baseUrl(baseUrl)
                .patch(String.format("api/device-accounts/%s", id))
                .setBearer(deviceAccountToken)
                .addParameter("identifier", identifier)
                .addParameter("name", name)
                .addParameter("push_credentials", new PushCredentials(pushService, token));
    }

    /**
     * Deletes (un-enrolls) a device account
     *
     * @param id                 the device account id
     * @param deviceAccountToken the jwt to use in authorization header
     * @return a request to execute
     */
    public ExecutableRequest<Void> deleteDeviceAccount(String id, String deviceAccountToken) {
        return requestFactory.newRequest()
                .baseUrl(baseUrl)
                .delete(String.format("api/device-accounts/%s", id))
                .setBearer(deviceAccountToken);
    }

    /**
     * Returns tenant information (friendly_name / picture_url)
     *
     * @return a request to execute
     * @see TenantInfo
     */
    public ExecutableRequest<TenantInfo> getTenantInfo() {
        return requestFactory.newRequest(TenantInfo.class)
                .baseUrl(baseUrl)
                .get("api/tenant-info");
    }

    /**
     * Accepts/verifies a login transaction using the OTP code
     *
     * @param txToken the jwt to use in authorization header
     * @param code    the OTP code
     * @return a request to execute
     */
    public ExecutableRequest<Void> allowLogin(String txToken, String code) {
        return requestFactory.newRequest()
                .baseUrl(baseUrl)
                .post("api/verify-otp")
                .setBearer(txToken)
                .addParameter("type", "push_notification")
                .addParameter("code", code);
    }

    /**
     * Rejects a login, possibly indicating the reason to be rejected
     *
     * @param txToken the jwt to use in authorization header
     * @param code    the OTP code
     * @param reason  the reject reason
     * @return a request to execute
     */
    public ExecutableRequest<Void> rejectLogin(String txToken, String code, String reason) {
        return requestFactory.newRequest()
                .baseUrl(baseUrl)
                .post("api/reject-login")
                .setBearer(txToken)
                .addParameter("code", code)
                .addParameter("reason", reason);
    }

    /**
     * Obtains the list of reject reasons
     *
     * @return a request to execute
     * @see RejectReason
     */
    public ExecutableRequest<List<RejectReason>> getRejectReasons() {
        Type type = new TypeToken<List<RejectReason>>() {
        }.getType();
        return requestFactory.<List<RejectReason>>newRequest(type)
                .baseUrl(baseUrl)
                .get("api/reject-reasons");
    }

    public static class Builder {

        private String baseUrl;
        private Executor callbackExecutor;
        private OkHttpClient client;
        private Gson gson;

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder callbackExecutor(Executor callbackExecutor) {
            this.callbackExecutor = callbackExecutor;
            return this;
        }

        public Builder client(OkHttpClient client) {
            this.client = client;
            return this;
        }

        public Builder gson(Gson gson) {
            this.gson = gson;
            return this;
        }

        public GuardianAPI build() {
            if (baseUrl == null) {
                throw new IllegalArgumentException("baseUrl cannot be null");
            }

            if (gson == null) {
                gson = new GsonBuilder()
                        .registerTypeAdapterFactory(new JsonRequiredTypeAdapterFactory())
                        .create();
            }

            RequestFactory.Builder builder = new RequestFactory.Builder()
                    .serializer(new GsonSerializer(gson, new GuardianServerErrorParser(gson)));

            if (callbackExecutor != null) {
                builder.callbackExecutor(callbackExecutor);
            }

            if (client != null) {
                builder.client(client);
            }

            return new GuardianAPI(baseUrl, builder.build());
        }
    }
}
