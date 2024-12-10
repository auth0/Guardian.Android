package com.auth0.android.guardian.sdk;

import com.google.gson.annotations.SerializedName;

public class GuardianRichConsent implements RichConsent {
    private final String id;
    @SerializedName("requested_details")
    private final GuardianRichConsentRequestedDetails requestedDetails;
    @SerializedName("created_at")
    private final String createdAt;
    @SerializedName("expires_at")
    private final String expiresAt;

    public GuardianRichConsent(String id, GuardianRichConsentRequestedDetails requestedDetails, String createdAt, String expiresAt) {
        this.id = id;
        this.requestedDetails = requestedDetails;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }


    @Override
    public String getId() {
        return id;
    }

    @Override
    public RichConsentRequestedDetails getRequestedDetails() {
        return requestedDetails;
    }

    @Override
    public String getCreatedAt() {
        return createdAt;
    }

    @Override
    public String getExpiresAt() {
        return expiresAt;
    }
}
