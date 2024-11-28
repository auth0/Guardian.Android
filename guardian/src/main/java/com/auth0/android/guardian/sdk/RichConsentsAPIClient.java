package com.auth0.android.guardian.sdk;

import com.auth0.android.guardian.sdk.model.RichConsent;
import com.auth0.android.guardian.sdk.networking.RequestFactory;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

import okhttp3.HttpUrl;

public class RichConsentsAPIClient {
    private final RequestFactory requestFactory;
    private final HttpUrl baseUrl;

    RichConsentsAPIClient(RequestFactory requestFactory, HttpUrl baseUrl) {
        this.requestFactory = requestFactory;
        this.baseUrl = baseUrl.newBuilder()
                .addPathSegments("rich-consents")
                .build();
    }

    public GuardianAPIRequest<RichConsent> fetch(String consentId, String transactionToken) {
        Type type = new TypeToken<RichConsent>() {
        }.getType();

        final HttpUrl url = baseUrl.newBuilder()
                .addPathSegment("rich-consents")
                .addPathSegment(consentId)
                .build();

//        final String dpopAssertion = createProofOfPossessionAssertion(
//                url,
//                privateKey,
//                publicKey,
//                transactionToken
//        );

        return requestFactory
                .<RichConsent>newRequest("GET", url, type)
                .setHeader("Authorization", "MFA-DPoP ".concat(transactionToken));
//                .setHeader("MFA-DPoP", dpopAssertion);
    }
}
