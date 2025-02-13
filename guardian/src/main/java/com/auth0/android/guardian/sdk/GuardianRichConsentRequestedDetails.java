package com.auth0.android.guardian.sdk;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class GuardianRichConsentRequestedDetails implements RichConsentRequestedDetails {
    private final String audience;
    private final String[] scope;
    @SerializedName("binding_message")
    private final String bindingMessage;
    @SerializedName("authorization_details")
    private final List<Map<String, Object>> authorizationDetails;

    public GuardianRichConsentRequestedDetails(
            String audience,
            String[] scope,
            String bindingMessage,
            List<Map<String, Object>> authorizationDetails
            ) {
        this.bindingMessage = bindingMessage;
        this.audience = audience;
        this.scope = scope;
        this.authorizationDetails = authorizationDetails;
    }


    @NonNull
    @Override
    public String getAudience() {
        return audience;
    }

    @NonNull
    @Override
    public String[] getScope() {
        return scope;
    }

    @Override
    public String getBindingMessage() {
        return bindingMessage;
    }

    @Override
    public List<Map<String, Object>> getAuthorizationDetails() {
        return authorizationDetails;
    }
}
