package com.auth0.android.guardian.sdk;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class GuardianRichConsentRequestedDetails implements RichConsentRequestedDetails {
    private final String audience;
    private final String[] scope;
    @SerializedName("binding_message")
    private final String bindingMessage;

    public GuardianRichConsentRequestedDetails(
            String audience,
            String[] scope,
            String bindingMessage) {
        this.bindingMessage = bindingMessage;
        this.audience = audience;
        this.scope = scope;
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
}
